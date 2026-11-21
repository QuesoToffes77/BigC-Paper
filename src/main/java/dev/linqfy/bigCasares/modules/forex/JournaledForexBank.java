package dev.linqfy.bigCasares.modules.forex;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/** Write-ahead evidence for Vault calls. Unresolved records require manual reconciliation. */
final class JournaledForexBank implements ForexBank {
    private final ForexBank delegate;
    private final Path journal;
    private final Path snapshot;
    private volatile boolean paused;
    private volatile boolean successfulTransfer;
    private String context = "unspecified";

    JournaledForexBank(ForexBank delegate, Path journal) {
        this(delegate, journal, null);
    }

    JournaledForexBank(ForexBank delegate, Path journal, Path snapshot) {
        this.delegate = delegate;
        this.journal = journal;
        this.snapshot = snapshot;
        this.paused = Files.exists(journal);
    }

    boolean paused() { return paused; }
    boolean hasUncommittedTransfer() { return successfulTransfer; }
    void context(String description) {
        context = description.replaceAll("[\\r\\n]", " ");
        if (context.length() > 256) context = context.substring(0, 256);
    }

    @Override public long balance(UUID player) { return delegate.balance(player); }
    @Override public boolean withdraw(UUID player, long amount) { return transfer(player, amount, false); }
    @Override public boolean deposit(UUID player, long amount) { return transfer(player, amount, true); }

    private boolean transfer(UUID player, long amount, boolean deposit) {
        if (paused) throw new IllegalStateException("FOREX is paused pending reconciliation");
        String operation = UUID.randomUUID().toString();
        try {
            String intent = "INTENT " + operation + " " + (deposit ? "DEPOSIT" : "WITHDRAW") + " " + player + " " + amount;
            if (!Files.exists(journal)) {
                // One forced write still records all evidence before calling Vault.
                intent = "CONTEXT " + context + "\nBASELINE " + snapshotHash() + "\n" + intent;
            }
            append(intent);
            boolean succeeded = deposit ? delegate.deposit(player, amount) : delegate.withdraw(player, amount);
            append((succeeded ? "SUCCEEDED " : "REJECTED ") + operation);
            if (succeeded) {
                successfulTransfer = true;
            } else if (successfulTransfer) {
                paused = true;
                throw new IllegalStateException("Vault rejected a transfer after earlier transfers; reconcile " + journal);
            } else {
                retire();
            }
            return succeeded;
        } catch (IOException | RuntimeException failure) {
            paused = true;
            throw new IllegalStateException("FOREX transfer requires reconciliation; inspect " + journal, failure);
        }
    }

    /** Called only after the corresponding market snapshot has been saved. */
    void committed() {
        if (paused) throw new IllegalStateException("FOREX is paused pending reconciliation");
        try {
            if (Files.exists(journal)) {
                append("SNAPSHOT COMMITTED " + snapshotHash());
                retire();
            }
            successfulTransfer = false;
        } catch (IOException failure) {
            paused = true;
            throw new IllegalStateException("Could not retire FOREX journal " + journal, failure);
        }
    }

    void pause() { paused = true; }

    record ReconciliationDetails(String journalHash, String snapshotHash, String evidence) { }

    ReconciliationDetails reconciliationDetails() {
        if (!paused || !Files.isRegularFile(journal)) {
            throw new IllegalStateException("No hay un journal pendiente; revisar los logs del servidor.");
        }
        try {
            byte[] evidence = Files.readAllBytes(journal);
            return new ReconciliationDetails(hash(evidence), snapshotHash(),
                new String(evidence, StandardCharsets.UTF_8));
        } catch (IOException failure) {
            throw new IllegalStateException("No se pudo leer el journal pendiente.", failure);
        }
    }

    /** The operator has repaired both sides independently; no Vault operation is replayed. */
    Path acknowledgeReconciliation(String journalHash, String marketHash, String operator) {
        ReconciliationDetails details = reconciliationDetails();
        if (!details.journalHash().equals(journalHash) || !details.snapshotHash().equals(marketHash)) {
            throw new IllegalStateException("El journal o el mercado cambiaron; volver a inspeccionar.");
        }
        if ("MISSING".equals(marketHash) || "UNSPECIFIED".equals(marketHash)) {
            throw new IllegalStateException("Falta market.yml; restaurá el estado antes de resolver.");
        }
        String actor = operator.replaceAll("[\r\n]", " ");
        if (actor.isBlank() || actor.length() > 128) {
            throw new IllegalStateException("Operador inválido.");
        }
        try {
            append("RECONCILED " + actor + " JOURNAL " + journalHash + " SNAPSHOT " + marketHash);
            Path archive = journal.resolveSibling("transfers-resolved-" + UUID.randomUUID() + ".log");
            Files.move(journal, archive, StandardCopyOption.ATOMIC_MOVE);
            syncDirectory(journal.getParent());
            return archive;
        } catch (IOException failure) {
            throw new IllegalStateException("No se pudo archivar el journal; el mercado sigue cerrado.", failure);
        }
    }

    private String snapshotHash() throws IOException {
        if (snapshot == null) return "UNSPECIFIED";
        if (!Files.exists(snapshot)) return "MISSING";
        return hash(Files.readAllBytes(snapshot));
    }

    private static String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 unavailable", impossible);
        }
    }

    private void append(String line) throws IOException {
        ensureDirectory(journal.getParent());
        boolean newFile = !Files.exists(journal);
        byte[] bytes = (line + "\n").getBytes(StandardCharsets.UTF_8);
        try (FileChannel channel = FileChannel.open(journal, StandardOpenOption.CREATE, StandardOpenOption.WRITE,
            StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            while (buffer.hasRemaining()) channel.write(buffer);
            channel.force(true);
        }
        if (newFile) syncDirectory(journal.getParent());
    }

    private void retire() throws IOException {
        Files.deleteIfExists(journal);
        syncDirectory(journal.getParent());
    }

    static void ensureDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) return;
        Path parent = path.getParent();
        if (parent != null) ensureDirectory(parent);
        Files.createDirectory(path);
        if (parent != null) syncDirectory(parent);
    }

    static void syncDirectory(Path path) throws IOException {
        try (FileChannel directory = FileChannel.open(path, StandardOpenOption.READ)) {
            directory.force(true);
        }
    }
}
