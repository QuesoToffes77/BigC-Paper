package dev.linqfy.bigCasares;

import dev.linqfy.bigCasares.items.CustomItemRegistry;
import dev.linqfy.bigCasares.command.BigCasaresCommand;
import dev.linqfy.bigCasares.module.ModuleManager;
import dev.linqfy.bigCasares.modules.bounties.BountyModule;
import dev.linqfy.bigCasares.modules.copperapple.CopperAppleModule;
import dev.linqfy.bigCasares.modules.customcrossbow.CustomCrossbowModule;
import dev.linqfy.bigCasares.modules.inventorylimit.InventoryLimitModule;
import dev.linqfy.bigCasares.modules.geyser.BedrockShopForm;
import dev.linqfy.bigCasares.modules.geyser.GeyserIntegrationModule;
import dev.linqfy.bigCasares.modules.missions.MissionModule;
import dev.linqfy.bigCasares.modules.airdrop.AirdropModule;
import dev.linqfy.bigCasares.modules.model.JavaModelGateway;
import dev.linqfy.bigCasares.modules.model.JavaModelGatewayFactory;
import dev.linqfy.bigCasares.modules.nexus.NexusModule;
import dev.linqfy.bigCasares.modules.pveboss.PveBossModule;
import dev.linqfy.bigCasares.modules.resourcepack.ResourcePackModule;
import dev.linqfy.bigCasares.modules.shop.ShopModule;
import dev.linqfy.bigCasares.modules.skillrating.SkillRatingModule;
import dev.linqfy.bigCasares.modules.smokebomb.SmokeBombModule;
import dev.linqfy.bigCasares.modules.teams.TeamModule;
import dev.linqfy.bigCasares.platform.ClientPlatform;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.UUID;

public final class BigCasares extends JavaPlugin {

    private ModuleManager moduleManager;
    private CustomItemRegistry customItemRegistry;
    private MissionModule missionModule;
    private BountyModule bountyModule;
    private ShopModule shopModule;
    private InventoryLimitModule inventoryLimitModule;
    private SkillRatingModule skillRatingModule;
    private ResourcePackModule resourcePackModule;
    private TeamModule teamModule;
    private NexusModule nexusModule;
    private PveBossModule pveBossModule;
    private GeyserIntegrationModule geyserIntegrationModule;
    private AirdropModule airdropModule;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        initializeRuntime();
        registerCommands();

        getLogger().info("BigCasares enabled. Active modules: "
            + moduleManager.getActiveModuleCount() + "/"
            + moduleManager.getRegisteredModuleCount());

    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableActiveModules();
        }
    }

    public CustomItemRegistry getCustomItemRegistry() {
        return customItemRegistry;
    }

    public MissionModule getMissionModule() {
        return missionModule;
    }

    public BountyModule getBountyModule() {
        return bountyModule;
    }

    public ShopModule getShopModule() {
        return shopModule;
    }

    public InventoryLimitModule getInventoryLimitModule() {
        return inventoryLimitModule;
    }

    public SkillRatingModule getSkillRatingModule() {
        return skillRatingModule;
    }

    public ResourcePackModule getResourcePackModule() {
        return resourcePackModule;
    }

    public TeamModule getTeamModule() {
        return teamModule;
    }

    public NexusModule getNexusModule() {
        return nexusModule;
    }

    public PveBossModule getPveBossModule() {
        return pveBossModule;
    }

    public GeyserIntegrationModule getGeyserIntegrationModule() {
        return geyserIntegrationModule;
    }

    public AirdropModule getAirdropModule() {
        return airdropModule;
    }

    public static List<String> frameworkV2ModuleOrder() {
        return List.of(
            "resource-pack-system",
            "team-system",
            "nexus-system",
            "entity-shop-system",
            "pve-boss-system",
            "geyser-integration"
        );
    }

    public void reloadPluginState() {
        if (moduleManager != null) {
            moduleManager.disableActiveModules();
        }
        reloadConfig();
        initializeRuntime();
        registerCommands();
    }

    public void openShop(Player player) {
        if (shopModule == null || !shopModule.isEnabled()) {
            player.sendMessage("§cEl shop no esta disponible.");
            return;
        }
        shopModule.openMainMenu(player);
    }

    public int enforceInventoryLimits(Player player) {
        return inventoryLimitModule == null ? 0 : inventoryLimitModule.enforce(player);
    }

    private void initializeRuntime() {
        this.customItemRegistry = new CustomItemRegistry();
        this.moduleManager = new ModuleManager(this, getConfig());
        this.missionModule = new MissionModule(this);
        this.bountyModule = new BountyModule(this);
        this.shopModule = new ShopModule(this);
        this.inventoryLimitModule = new InventoryLimitModule(this);
        this.skillRatingModule = new SkillRatingModule(this);
        this.geyserIntegrationModule = new GeyserIntegrationModule(this);
        this.airdropModule = new AirdropModule(this);
        this.resourcePackModule = new ResourcePackModule(this, this::resolveClientPlatform);
        this.teamModule = new TeamModule(this);
        JavaModelGateway javaModels = JavaModelGatewayFactory.create(this);
        this.nexusModule = new NexusModule(this, playerId ->
            teamModule.service().flatMap(service -> service.findByMember(playerId)).map(team -> team.id()),
            teamId -> teamModule.service().flatMap(service -> service.findById(teamId))
                .map(team -> team.color().legacyCode() + team.name().toUpperCase() + " [" + team.tag() + "]"),
            javaModels);
        this.pveBossModule = new PveBossModule(
            this,
            this::resolveClientPlatform,
            playerId -> resourcePackModule != null
                && resourcePackModule.service().map(service -> service.hasLoadedPack(playerId)).orElse(false),
            javaModels
        );
        this.shopModule.configurePlatform(this::resolveClientPlatform, this::sendBedrockShopForm);

        moduleManager.register(new CopperAppleModule(this));
        moduleManager.register(new SmokeBombModule(this));
        moduleManager.register(new CustomCrossbowModule(this));
        moduleManager.register(missionModule);
        moduleManager.register(bountyModule);
        moduleManager.register(inventoryLimitModule);
        moduleManager.register(skillRatingModule);
        moduleManager.register(airdropModule);

        moduleManager.register(resourcePackModule);
        moduleManager.register(teamModule);
        moduleManager.register(nexusModule);
        moduleManager.register(shopModule);
        moduleManager.register(pveBossModule);
        moduleManager.register(geyserIntegrationModule);
        moduleManager.enableRegisteredModules();
    }

    private ClientPlatform resolveClientPlatform(UUID playerId) {
        return geyserIntegrationModule == null
            ? ClientPlatform.JAVA
            : geyserIntegrationModule.platformGateway().resolvePlatform(playerId);
    }

    private boolean sendBedrockShopForm(
        UUID playerId,
        dev.linqfy.bigCasares.modules.shop.ShopView view,
        java.util.function.IntConsumer responseHandler
    ) {
        if (geyserIntegrationModule == null) {
            return false;
        }
        return geyserIntegrationModule.shopForms()
            .map(forms -> forms.show(playerId, new BedrockShopForm(
                view.title(),
                view.description() + "\n\n§fSaldo: §a" + view.balance(),
                view.items().stream().map(item -> new BedrockShopForm.Button(
                    item.name() + (item.price().isBlank() ? "" : "\n§7" + item.price()),
                    item.iconUrl()
                )).toList(),
                (ignored, index) -> responseHandler.accept(index)
            )))
            .orElse(false);
    }

    private void registerCommands() {
        BigCasaresCommand handler = new BigCasaresCommand(this);
        PluginCommand management = getCommand("bigcasares");
        if (management != null) {
            management.setExecutor(handler);
            management.setTabCompleter(handler);
        }
        PluginCommand shop = getCommand("shop");
        if (shop != null) {
            shop.setExecutor(handler);
            shop.setTabCompleter(handler);
        }
        PluginCommand rating = getCommand("rating");
        if (rating != null) {
            rating.setExecutor(handler);
            rating.setTabCompleter(handler);
        }
        for (String commandName : List.of("team", "teammanage", "nexus", "boss")) {
            PluginCommand command = getCommand(commandName);
            if (command != null) {
                command.setExecutor(handler);
                command.setTabCompleter(handler);
            }
        }
    }
}
