package dev.linqfy.bigCasares.modules.danger;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import dev.linqfy.bigCasares.module.runtime.BukkitRuntimeRegistrations;
import dev.linqfy.bigCasares.module.runtime.RuntimeRegistrationScope;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

public final class DangerModule implements PluginModule {
    private final BigCasares plugin;
    private DangerService service;
    private RuntimeRegistrationScope compatibilityScope;
    private long heartbeat;

    public DangerModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override public String getId() { return "danger-system"; }

    @Override
    public void onEnable() {
        RuntimeRegistrationScope scope = new RuntimeRegistrationScope();
        compatibilityScope = scope;
        onEnable(scope);
    }

    @Override
    public void onEnable(RuntimeRegistrationScope scope) {
        Path directory = plugin.getDataFolder().toPath().resolve("data/danger/players");
        this.service = new DangerService(
            new YamlDangerStorage(directory),
            playerId -> plugin.getSkillRatingModule() == null || plugin.getSkillRatingModule().service() == null
                ? 0.0 : plugin.getSkillRatingModule().service().ratingFor(playerId).skillRating(),
            Clock.systemUTC()
        );
        BukkitRuntimeRegistrations registrations = new BukkitRuntimeRegistrations(plugin, scope);
        registrations.registerListener("danger-gameplay", new DangerGameplayListener(service, this::refreshPresentation));
        registrations.scheduleRepeating("danger-heartbeat", this::heartbeat, 20L, 20L);
        plugin.getServer().getOnlinePlayers().forEach(player -> service.beginSession(player.getUniqueId()));
        scope.register("danger-state", () -> service = null);
    }

    @Override
    public void onDisable() {
        if (service != null) plugin.getServer().getOnlinePlayers().forEach(player -> service.touch(player.getUniqueId()));
        RuntimeRegistrationScope scope = compatibilityScope;
        compatibilityScope = null;
        if (scope != null) scope.close();
    }

    public Optional<DangerService> service() { return Optional.ofNullable(service); }

    public void refreshPresentation(UUID playerId) {
        if (service == null) return;
        Player player = plugin.getServer().getPlayer(playerId);
        if (player == null) return;
        if (plugin.getTeamModule() != null && plugin.getTeamModule().presentationService().isPresent()) {
            plugin.getTeamModule().presentationService().orElseThrow().refreshPlayer(playerId);
            return;
        }
        var color = net.kyori.adventure.text.format.TextColor.color(service.snapshot(playerId).tier().colorRgb());
        player.playerListName(net.kyori.adventure.text.Component.text(player.getName(), color));
        player.displayName(net.kyori.adventure.text.Component.text(player.getName(), color));
    }

    private void heartbeat() {
        heartbeat++;
        Particle.DustOptions red = new Particle.DustOptions(Color.RED, 1.0f);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (service.snapshot(player.getUniqueId()).tier() == DangerTier.LETAL
                && player.getHealth() < player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue() / 2.0) {
                player.getWorld().spawnParticle(Particle.DUST, player.getLocation().add(0, 1, 0), 4, 0.3, 0.6, 0.3, 0, red);
            }
            if (heartbeat % 300L == 0L) service.touch(player.getUniqueId());
        }
    }
}
