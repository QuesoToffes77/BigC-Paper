package dev.linqfy.bigCasares.modules.moderation;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class ModerationModule implements PluginModule {
    private final BigCasares plugin;
    private final AuditService audit = new AuditService();
    private ModerationSettings settings;
    private AbuseScoringService scoring;
    private ObservationStorage observations;
    private ModerationListener listener;
    private ModerationLogHandler logHandler;
    private BukkitTask purgeTask;
    private BukkitTask rareItemTask;
    private RareItemGainTracker rareItemTracker;

    public ModerationModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "moderation-system";
    }

    @Override
    public void onEnable() {
        if (plugin == null) {
            return;
        }
        settings = ModerationSettings.load(plugin.getConfig());
        scoring = new AbuseScoringService(
            settings.scoreWindow(), settings.warningThreshold(), settings.criticalThreshold(), settings.alertCooldown()
        );
        Path observationPath = plugin.getDataFolder().toPath().resolve("data").resolve("moderation-system");
        observations = new FileObservationStorage(observationPath);
        rareItemTracker = new RareItemGainTracker(
            settings.rareItems().stream().map(org.bukkit.Material::name).collect(java.util.stream.Collectors.toSet())
        );
        ModerationSignalTracker tracker = new ModerationSignalTracker(settings.sensitiveCommands());
        listener = new ModerationListener(plugin, this, tracker);
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        logHandler = new ModerationLogHandler(
            () -> plugin.getServer().getOnlinePlayers(), this::acceptSignal, audit
        );
        Logger.getLogger("").addHandler(logHandler);
        purgeObservations();
        purgeTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(
            plugin, this::purgeObservations, 20L * 60L * 60L, 20L * 60L * 60L * 24L
        );
        rareItemTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::sampleRareItems, 20L, 20L);
        audit.publish(AuditEvent.system(AuditSeverity.INFO, "lifecycle", "plugin-enabled", "BigCasares inició"));
    }

    @Override
    public void onDisable() {
        if (plugin != null) {
            audit.publish(AuditEvent.system(AuditSeverity.INFO, "lifecycle", "plugin-disabled", "BigCasares se detiene"));
        }
        if (purgeTask != null) {
            purgeTask.cancel();
            purgeTask = null;
        }
        if (rareItemTask != null) {
            rareItemTask.cancel();
            rareItemTask = null;
        }
        if (listener != null) {
            HandlerList.unregisterAll(listener);
            listener = null;
        }
        if (logHandler != null) {
            Logger.getLogger("").removeHandler(logHandler);
            logHandler = null;
        }
    }

    public AuditService audit() {
        return audit;
    }

    public ObservationStorage observations() {
        return observations;
    }

    public void acceptSignal(AbuseSignal signal) {
        if (scoring == null) {
            return;
        }
        int configuredPoints = settings.signalPoints().getOrDefault(signal.rule(), signal.points());
        AbuseSignal effectiveSignal = configuredPoints == signal.points() ? signal : new AbuseSignal(
            signal.timestamp(), signal.playerId(), signal.playerName(), signal.rule(), configuredPoints, signal.evidence()
        );
        AbuseScoreResult result = scoring.record(effectiveSignal);
        Map<String, String> details = new HashMap<>();
        details.put("rule", signal.rule());
        details.put("points", String.valueOf(effectiveSignal.points()));
        details.put("score", String.valueOf(result.score()));
        details.put("evidence", signal.evidence());
        audit.publish(new AuditEvent(
            signal.timestamp(), AuditSeverity.INFO, "abuse", "signal", java.util.Optional.of(signal.playerId()),
            signal.playerName(), "Señal de abuso: " + signal.rule(), details
        ));
        result.alert().ifPresent(severity -> {
            Runnable alert = () -> alertStaff(signal, result.score(), severity);
            if (plugin.getServer().isPrimaryThread()) {
                alert.run();
            } else {
                plugin.getServer().getScheduler().runTask(plugin, alert);
            }
        });
    }

    public void observePlayer(Player player, String type, Map<String, String> values) {
        if (observations == null) {
            return;
        }
        Map<String, String> complete = new HashMap<>(values);
        complete.put("name", player.getName());
        complete.put("uuid", player.getUniqueId().toString());
        observations.append(new Observation(Instant.now(), player.getUniqueId(), type, complete));
    }

    public void markInventoryGainExplained(Player player) {
        if (rareItemTracker != null) {
            rareItemTracker.markExplained(player.getUniqueId());
        }
    }

    public void forgetInventoryTracking(Player player) {
        if (rareItemTracker != null) {
            rareItemTracker.forget(player.getUniqueId());
        }
    }

    public void markAdminGive(String command) {
        if (rareItemTracker == null || command == null) {
            return;
        }
        String normalized = command.startsWith("/") ? command.substring(1) : command;
        String[] parts = normalized.trim().split("\\s+");
        if (parts.length < 2 || !(parts[0].equalsIgnoreCase("give") || parts[0].endsWith(":give"))) {
            return;
        }
        Player target = plugin.getServer().getPlayerExact(parts[1]);
        if (target != null) {
            rareItemTracker.markExplained(target.getUniqueId());
        }
    }

    private void alertStaff(AbuseSignal signal, int score, AbuseSeverity severity) {
        String label = severity == AbuseSeverity.CRITICAL ? "CRÍTICA" : "ADVERTENCIA";
        String message = "§c[Moderación " + label + "] §f" + signal.playerName()
            + " §7alcanzó §e" + score + " puntos§7. " + signal.evidence();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!player.isOp()) {
                continue;
            }
            if (plugin.getServerControlModule() == null
                || plugin.getServerControlModule().service() == null
                || plugin.getServerControlModule().service().staffAlertsEnabled(player.getUniqueId())) {
                player.sendMessage(message);
            }
        }
        Map<String, String> details = Map.of(
            "rule", signal.rule(),
            "score", String.valueOf(score),
            "evidence", signal.evidence()
        );
        audit.publish(new AuditEvent(
            Instant.now(), severity == AbuseSeverity.CRITICAL ? AuditSeverity.CRITICAL : AuditSeverity.WARNING,
            "abuse", "threshold-" + severity.name().toLowerCase(java.util.Locale.ROOT),
            java.util.Optional.of(signal.playerId()), signal.playerName(), message, details
        ));
    }

    private void purgeObservations() {
        if (observations == null) {
            return;
        }
        try {
            observations.purgeOlderThan(Instant.now().minus(settings.retention()));
        } catch (RuntimeException ex) {
            plugin.getLogger().log(java.util.logging.Level.WARNING, "No se pudieron purgar observaciones de moderación", ex);
        }
    }

    private void sampleRareItems() {
        if (rareItemTracker == null) {
            return;
        }
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            Map<String, Integer> counts = new HashMap<>();
            for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
                if (item != null && settings.rareItems().contains(item.getType())) {
                    counts.merge(item.getType().name(), item.getAmount(), Integer::sum);
                }
            }
            rareItemTracker.sample(Instant.now(), player.getUniqueId(), player.getName(), counts)
                .ifPresent(this::acceptSignal);
        }
    }
}
