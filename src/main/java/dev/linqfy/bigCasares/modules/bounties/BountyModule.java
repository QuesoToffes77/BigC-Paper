package dev.linqfy.bigCasares.modules.bounties;

import dev.linqfy.bigCasares.BigCasares;
import dev.linqfy.bigCasares.module.PluginModule;
import net.milkbowl.vault.economy.Economy;

import java.nio.file.Path;
import java.time.Instant;

public final class BountyModule implements PluginModule {

    private final BigCasares plugin;

    private BountyEconomyGateway economyGateway;
    private BountyService service;

    public BountyModule(BigCasares plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "bounty-system";
    }

    @Override
    public void onEnable() {
        Economy economy = BountyEconomyGateway.resolveOrThrow(plugin);
        BountySettings settings = new BountySettings(
            plugin.getConfig().getDouble("bounty-system.economy.take-percent", 0.30),
            plugin.getConfig().getDouble("bounty-system.economy.bounty-percent", 0.20),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-balance", 100.0),
            plugin.getConfig().getDouble("bounty-system.economy.minimum-bounty", 25.0)
        );

        Path playersDirectory = plugin.getDataFolder().toPath().resolve("data").resolve("bounties").resolve("players");
        BountyStorage storage = new YamlBountyStorage(playersDirectory);
        this.economyGateway = new BountyEconomyGateway(plugin, economy);
        this.service = new BountyService(storage, economyGateway, settings, Instant::now);
        plugin.getServer().getPluginManager().registerEvents(new BountyListener(service, economyGateway), plugin);
    }

    @Override
    public void onDisable() {
    }

    public BountyEconomy economy() {
        return economyGateway;
    }

    public BountyService service() {
        return service;
    }
}
