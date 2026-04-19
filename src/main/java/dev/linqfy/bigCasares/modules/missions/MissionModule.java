package dev.linqfy.bigCasares.modules.missions;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MissionModule implements PluginModule {

    private final BigCasares plugin;

    private MissionCatalog catalog;
    private MissionStorage storage;
    private PlayerMissionService playerMissionService;
    private VaultEconomyGateway economyGateway;
    private MissionHudController hudController;
    private boolean enabled;

    public MissionModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "mission-system";
    }

    @Override
    public void onEnable() {
        Economy economy = VaultEconomyGateway.resolveOrThrow(plugin);
        this.catalog = new MissionCatalogLoader().load(plugin.getConfig());
        this.storage = new YamlMissionStorage(plugin.getDataFolder().toPath().resolve("data").resolve("missions").resolve("players"));
        this.economyGateway = new VaultEconomyGateway(plugin, economy);
        this.playerMissionService = new PlayerMissionService(
            catalog,
            new MissionRotationPolicy(new java.util.Random()),
            Instant::now,
            plugin.getConfig().getInt("mission-system.daily.count", 3),
            plugin.getConfig().getInt("mission-system.weekly.count", 2),
            this::nextDailyReset,
            this::nextWeeklyReset
        );
        this.hudController = new MissionHudController(this, economyGateway);

        registerListener(new MissionListener(this));
        registerListener(hudController);
        this.enabled = true;
    }

    @Override
    public void onDisable() {
        this.enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void openHud(Player player, MissionHudView view) {
        if (!enabled || hudController == null) {
            player.sendMessage("§cEl sistema de misiones no esta disponible.");
            return;
        }
        revalidatePassiveMissions(player);
        hudController.open(player, view);
    }

    public void claimAll(Player player) {
        MissionPlayerState state = getOrCreateState(player);
        MissionPlayerState claimed = playerMissionService.claimAllCompleted(player.getUniqueId(), state, economyGateway);
        storage.save(claimed);
        double amount = sumClaimables(state);
        if (amount > 0.0) {
            player.sendMessage("§aReclamaste " + economyGateway.format(amount) + " en recompensas.");
        } else {
            player.sendMessage("§eNo tenes recompensas pendientes.");
        }
    }

    public void reroll(Player player) {
        MissionPlayerState fresh = playerMissionService.getOrCreateState(player.getUniqueId(), null);
        storage.save(fresh);
    }

    public void reset(Player player) {
        reroll(player);
    }

    public MissionPlayerState getOrCreateState(Player player) {
        Optional<MissionPlayerState> loaded = storage.load(player.getUniqueId());
        MissionPlayerState resolved = playerMissionService.getOrCreateState(player.getUniqueId(), loaded.orElse(null));
        storage.save(resolved);
        return resolved;
    }

    public int countClaimables(MissionPlayerState state) {
        return claimableAssignments(state).size();
    }

    public List<MissionAssignment> claimableAssignments(MissionPlayerState state) {
        List<MissionAssignment> claimables = new ArrayList<>();
        appendClaimables(claimables, state.dailyAssignments());
        appendClaimables(claimables, state.weeklyAssignments());
        return claimables;
    }

    public double sumClaimables(MissionPlayerState state) {
        return claimableAssignments(state).stream()
            .mapToDouble(assignment -> assignment.definition().reward())
            .sum();
    }

    public void incrementMatchingMissions(Player player, MissionType missionType, int delta) {
        updateMatchingMissions(player, missionType, ProgressUpdateMode.INCREMENT, delta, null, null, null);
    }

    public void completeRenameMission(Player player, Material itemType, String newName) {
        updateMatchingMissions(player, MissionType.RENAME_ITEM_TO_EXACT_NAME, ProgressUpdateMode.CONDITIONAL_SET, 1, itemType, newName, null);
    }

    public void completeFinalHitMission(Player player, Material weapon) {
        updateMatchingMissions(player, MissionType.FINAL_HIT_PLAYER_WITH_ITEM, ProgressUpdateMode.CONDITIONAL_SET, 1, weapon, null, null);
    }

    public void completeMobKillMission(Player player, String entityType, Material weapon) {
        updateMatchingMissions(player, MissionType.KILL_ENTITY_WITH_ITEM_ONLY, ProgressUpdateMode.CONDITIONAL_SET, 1, weapon, null, entityType);
    }

    public void completePigNameMission(Player player, String customName) {
        updateMatchingMissions(player, MissionType.NAME_ENTITY_AFTER_PLAYER, ProgressUpdateMode.CONDITIONAL_SET, 1, null, customName, "PIG");
    }

    public void revalidatePassiveMissions(Player player) {
        MissionPlayerState state = getOrCreateState(player);
        MissionPlayerState updated = state;

        for (MissionAssignment assignment : state.dailyAssignments().values()) {
            updated = revalidateAssignment(player, updated, MissionScope.DAILY, assignment);
        }
        for (MissionAssignment assignment : state.weeklyAssignments().values()) {
            updated = revalidateAssignment(player, updated, MissionScope.WEEKLY, assignment);
        }

        storage.save(updated);
    }

    private MissionPlayerState revalidateAssignment(Player player, MissionPlayerState state, MissionScope scope, MissionAssignment assignment) {
        MissionDefinition definition = assignment.definition();
        int progress = assignment.snapshot().progress();

        switch (definition.type()) {
            case HOLD_EXACT_ITEM_COUNT -> {
                Material material = Material.valueOf(String.valueOf(definition.params().get("material")));
                progress = MissionProgressEngine.countInventoryMaterial(player, material);
            }
            case EQUIP_SPECIFIC_ITEM -> {
                Material material = Material.valueOf(String.valueOf(definition.params().get("material")));
                boolean requiresCurse = Boolean.parseBoolean(String.valueOf(definition.params().getOrDefault("binding-curse", false)));
                boolean equipped = requiresCurse
                    ? MissionProgressEngine.isWearingCursed(player, material)
                    : MissionProgressEngine.isWearing(player, material);
                progress = equipped ? definition.goal() : 0;
            }
            case STAND_ON_BLOCK_AT_Y -> {
                Material material = Material.valueOf(String.valueOf(definition.params().get("material")));
                int yLevel = Integer.parseInt(String.valueOf(definition.params().get("y")));
                progress = MissionProgressEngine.isStandingOn(player, material, yLevel) ? definition.goal() : 0;
            }
            default -> {
                return state;
            }
        }

        return playerMissionService.updateAbsoluteProgress(state, scope, definition.id(), progress);
    }

    private void updateMatchingMissions(
        Player player,
        MissionType missionType,
        ProgressUpdateMode mode,
        int value,
        Material material,
        String text,
        String entityType
    ) {
        MissionPlayerState state = getOrCreateState(player);
        MissionPlayerState updated = state;
        updated = updateScope(player, updated, MissionScope.DAILY, missionType, mode, value, material, text, entityType);
        updated = updateScope(player, updated, MissionScope.WEEKLY, missionType, mode, value, material, text, entityType);
        storage.save(updated);
    }

    private MissionPlayerState updateScope(
        Player player,
        MissionPlayerState state,
        MissionScope scope,
        MissionType missionType,
        ProgressUpdateMode mode,
        int value,
        Material material,
        String text,
        String entityType
    ) {
        Map<String, MissionAssignment> assignments = scope == MissionScope.DAILY ? state.dailyAssignments() : state.weeklyAssignments();
        MissionPlayerState updated = state;

        for (MissionAssignment assignment : assignments.values()) {
            MissionDefinition definition = assignment.definition();
            if (definition.type() != missionType || !matchesCondition(player, definition, material, text, entityType)) {
                continue;
            }

            int nextProgress = switch (mode) {
                case INCREMENT -> assignment.snapshot().progress() + value;
                case CONDITIONAL_SET -> value;
            };
            updated = playerMissionService.updateAbsoluteProgress(updated, scope, definition.id(), nextProgress);
        }

        return updated;
    }

    private boolean matchesCondition(Player player, MissionDefinition definition, Material material, String text, String entityType) {
        return switch (definition.type()) {
            case FINAL_HIT_PLAYER_WITH_ITEM -> material != null
                && material.name().equalsIgnoreCase(String.valueOf(definition.params().getOrDefault("material", material.name())));
            case KILL_ENTITY_WITH_ITEM_ONLY -> material != null
                && material.name().equalsIgnoreCase(String.valueOf(definition.params().getOrDefault("material", material.name())))
                && entityType != null
                && entityType.equalsIgnoreCase(String.valueOf(definition.params().getOrDefault("entity", entityType)));
            case RENAME_ITEM_TO_EXACT_NAME -> material != null
                && material.name().equalsIgnoreCase(String.valueOf(definition.params().getOrDefault("material", material.name())))
                && text != null
                && text.equals(definition.params().getOrDefault("name", text));
            case NAME_ENTITY_AFTER_PLAYER -> {
                if (text == null) {
                    yield false;
                }
                String expectedEntity = String.valueOf(definition.params().getOrDefault("entity", "PIG"));
                boolean entityMatches = entityType == null || entityType.equalsIgnoreCase(expectedEntity);
                boolean playerNameMatches = Bukkit.getOfflinePlayer(text).hasPlayedBefore()
                    || Bukkit.getPlayerExact(text) != null
                    || text.equalsIgnoreCase(player.getName());
                yield entityMatches && playerNameMatches;
            }
            default -> true;
        };
    }

    private void appendClaimables(List<MissionAssignment> claimables, Map<String, MissionAssignment> assignments) {
        for (MissionAssignment assignment : assignments.values()) {
            if (assignment.snapshot().completed() && !assignment.snapshot().claimed()) {
                claimables.add(assignment);
            }
        }
    }

    private void registerListener(Listener listener) {
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
    }

    private Instant nextDailyReset() {
        int hour = plugin.getConfig().getInt("mission-system.reset.daily-hour", 0);
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime next = now.withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        return next.atZone(zone).toInstant();
    }

    private Instant nextWeeklyReset() {
        String configuredDay = plugin.getConfig().getString("mission-system.reset.weekly-day", "MONDAY");
        int hour = plugin.getConfig().getInt("mission-system.reset.weekly-hour", 0);
        DayOfWeek dayOfWeek = DayOfWeek.valueOf(configuredDay.toUpperCase(Locale.ROOT));
        ZoneId zone = ZoneId.systemDefault();
        LocalDateTime now = LocalDateTime.now(zone);
        LocalDateTime next = now.with(TemporalAdjusters.nextOrSame(dayOfWeek)).withHour(hour).withMinute(0).withSecond(0).withNano(0);
        if (!next.isAfter(now)) {
            next = next.plusWeeks(1);
        }
        return next.atZone(zone).toInstant();
    }

    private enum ProgressUpdateMode {
        INCREMENT,
        CONDITIONAL_SET
    }
}
