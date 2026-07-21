package dev.linqfy.bigCasares.modules.moderation;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class ModerationSignalTracker {
    private final Set<String> sensitiveCommands;
    private final Map<UUID, Deque<Instant>> chatTimes = new HashMap<>();
    private final Map<UUID, Map<String, Deque<Instant>>> chatDuplicates = new HashMap<>();
    private final Map<UUID, Deque<Instant>> commandTimes = new HashMap<>();
    private final Map<UUID, Map<String, Deque<Instant>>> commandDuplicates = new HashMap<>();
    private final Map<UUID, Deque<Instant>> reconnects = new HashMap<>();
    private final Map<String, Deque<Instant>> joinsByIp = new HashMap<>();
    private final Map<String, Map<UUID, Instant>> accountsByIp = new HashMap<>();
    private final Map<UUID, Deque<Instant>> meleeHits = new HashMap<>();
    private final Map<UUID, Deque<Instant>> inventoryClicks = new HashMap<>();
    private final Map<UUID, String> lastKnownIps = new HashMap<>();
    private final Map<String, Set<UUID>> allAccountsByIp = new HashMap<>();

    public ModerationSignalTracker(Set<String> sensitiveCommands) {
        this.sensitiveCommands = sensitiveCommands;
    }

    public List<AbuseSignal> chat(Instant now, UUID playerId, String playerName, String message) {
        List<AbuseSignal> signals = new ArrayList<>();
        if (threshold(chatTimes, playerId, now, Duration.ofSeconds(5), 7)) {
            signals.add(signal(now, playerId, playerName, "chat-rate", 20, "7 mensajes en 5 segundos"));
        }
        String normalized = normalize(message);
        if (!normalized.isBlank() && duplicate(chatDuplicates, playerId, normalized, now, Duration.ofSeconds(15), 3)) {
            signals.add(signal(now, playerId, playerName, "chat-duplicate", 25, "3 mensajes normalizados idénticos"));
        }
        return signals;
    }

    public List<AbuseSignal> command(Instant now, UUID playerId, String playerName, String command, boolean operator) {
        List<AbuseSignal> signals = new ArrayList<>();
        if (threshold(commandTimes, playerId, now, Duration.ofSeconds(5), 8)) {
            signals.add(signal(now, playerId, playerName, "command-rate", 20, "8 comandos en 5 segundos"));
        }
        String normalized = normalizeCommand(command);
        if (duplicate(commandDuplicates, playerId, normalized, now, Duration.ofSeconds(10), 3)) {
            signals.add(signal(now, playerId, playerName, "command-duplicate", 25, "3 comandos idénticos"));
        }
        String root = normalized.split(" ", 2)[0].replaceFirst("^/", "");
        if (!operator && sensitiveCommands.contains(root)) {
            signals.add(signal(now, playerId, playerName, "sensitive-command", 35, "Intento no autorizado: /" + root));
        }
        return signals;
    }

    public List<AbuseSignal> join(Instant now, UUID playerId, String playerName, String ip) {
        List<AbuseSignal> signals = new ArrayList<>();
        if (threshold(reconnects, playerId, now, Duration.ofSeconds(60), 4)) {
            signals.add(signal(now, playerId, playerName, "reconnect-rate", 20, "4 reconexiones en 60 segundos"));
        }
        Deque<Instant> ipJoins = joinsByIp.computeIfAbsent(ip, ignored -> new ArrayDeque<>());
        appendAndPurge(ipJoins, now, Duration.ofSeconds(60));
        if (ipJoins.size() == 6) {
            signals.add(signal(now, playerId, playerName, "ip-join-rate", 35, "6 ingresos desde una IP en 60 segundos"));
        }
        Map<UUID, Instant> accounts = accountsByIp.computeIfAbsent(ip, ignored -> new HashMap<>());
        accounts.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minus(Duration.ofMinutes(10))));
        accounts.put(playerId, now);
        if (accounts.size() == 3) {
            signals.add(signal(now, playerId, playerName, "ip-account-rate", 40, "3 UUID desde una IP en 10 minutos"));
        }

        String previousIp = lastKnownIps.get(playerId);
        if (previousIp != null && !previousIp.equals(ip)) {
            Set<UUID> otherAccounts = allAccountsByIp.getOrDefault(ip, Set.of());
            long others = otherAccounts.stream().filter(id -> !id.equals(playerId)).count();
            if (others > 0) {
                signals.add(signal(now, playerId, playerName, "ip-sudden-change", 100, "Cambio de IP abrupto cruzado con " + others + " cuenta(s) en " + ip));
            }
        }
        lastKnownIps.put(playerId, ip);
        allAccountsByIp.computeIfAbsent(ip, k -> new java.util.HashSet<>()).add(playerId);

        return signals;
    }

    public Optional<AbuseSignal> meleeHit(Instant now, UUID playerId, String playerName) {
        if (threshold(meleeHits, playerId, now, Duration.ofSeconds(1), 8)) {
            return Optional.of(signal(now, playerId, playerName, "melee-rate", 25, "Más de 7 impactos por segundo"));
        }
        return Optional.empty();
    }

    public Optional<AbuseSignal> inventoryClick(Instant now, UUID playerId, String playerName) {
        if (threshold(inventoryClicks, playerId, now, Duration.ofSeconds(1), 45)) {
            return Optional.of(signal(now, playerId, playerName, "inventory-click-rate", 15, "45 clics de inventario por segundo"));
        }
        return Optional.empty();
    }

    private boolean threshold(
        Map<UUID, Deque<Instant>> values,
        UUID playerId,
        Instant now,
        Duration window,
        int threshold
    ) {
        Deque<Instant> times = values.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        appendAndPurge(times, now, window);
        if (times.size() == threshold) {
            times.clear();
            return true;
        }
        return false;
    }

    private boolean duplicate(
        Map<UUID, Map<String, Deque<Instant>>> values,
        UUID playerId,
        String normalized,
        Instant now,
        Duration window,
        int threshold
    ) {
        Deque<Instant> times = values.computeIfAbsent(playerId, ignored -> new HashMap<>())
            .computeIfAbsent(normalized, ignored -> new ArrayDeque<>());
        appendAndPurge(times, now, window);
        if (times.size() == threshold) {
            times.clear();
            return true;
        }
        return false;
    }

    private void appendAndPurge(Deque<Instant> times, Instant now, Duration window) {
        Instant cutoff = now.minus(window);
        while (!times.isEmpty() && times.peekFirst().isBefore(cutoff)) {
            times.removeFirst();
        }
        times.addLast(now);
    }

    private String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    private String normalizeCommand(String command) {
        return normalize(command);
    }

    private AbuseSignal signal(Instant now, UUID id, String name, String rule, int points, String evidence) {
        return new AbuseSignal(now, id, name, rule, points, evidence);
    }
}
