package dev.linqfy.bigCasares.modules.discord;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class RemoteCommandPolicy {
    private static final Set<String> DANGEROUS_ROOTS = Set.of(
        "stop", "restart", "reload", "op", "deop", "ban", "ban-ip", "pardon", "pardon-ip",
        "whitelist", "execute", "kill", "minecraft:kill", "world", "mv", "multiverse", "entity",
        "lp", "luckperms", "pex", "permissions", "perm", "summon", "fill", "setblock", "clone",
        "forceload", "data", "clear", "gamerule",
        "difficulty", "time", "weather", "worldborder"
    );

    private final Duration lifetime;
    private final Map<String, PendingCommand> pending = new HashMap<>();

    public RemoteCommandPolicy(Duration lifetime) {
        this.lifetime = lifetime;
    }

    public boolean requiresConfirmation(String command) {
        String normalized = normalize(command).toLowerCase(Locale.ROOT);
        String root = normalized.split(" ", 2)[0];
        if (DANGEROUS_ROOTS.contains(root)) {
            return true;
        }
        return root.endsWith(":execute") || root.endsWith(":op") || root.endsWith(":deop")
            || root.contains("permission") || root.contains("worldedit");
    }

    public synchronized String createConfirmation(String userId, String command, Instant now) {
        purge(now);
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        pending.put(id, new PendingCommand(userId, normalize(command), now.plus(lifetime)));
        return id;
    }

    public synchronized Optional<String> confirm(String id, String userId, Instant now) {
        PendingCommand command = pending.get(id);
        if (command == null || !command.userId().equals(userId)) {
            return Optional.empty();
        }
        pending.remove(id);
        if (!now.isBefore(command.expiresAt())) {
            return Optional.empty();
        }
        return Optional.of(command.command());
    }

    public synchronized boolean reject(String id, String userId) {
        PendingCommand command = pending.get(id);
        if (command == null || !command.userId().equals(userId)) {
            return false;
        }
        pending.remove(id);
        return true;
    }

    private void purge(Instant now) {
        pending.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiresAt()));
    }

    private String normalize(String command) {
        String value = command == null ? "" : command.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private record PendingCommand(String userId, String command, Instant expiresAt) {
    }
}
