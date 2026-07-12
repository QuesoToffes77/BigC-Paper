package dev.linqfy.bigCasares.modules.pveboss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PaperBossPresentationAdapter implements BossPresentationGateway {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final Map<UUID, BossBar> bossBars = new HashMap<>();
    private final Map<UUID, BossView> views = new HashMap<>();

    @Override
    public void showBoss(BossAudience audience, BossView view) {
        hideExisting(view.bossInstanceId());
        BossBar bossBar = Bukkit.createBossBar(title(view), BarColor.PURPLE, BarStyle.SOLID);
        bossBar.setProgress(view.health().progress());
        synchronizeAudience(bossBar, audience);
        bossBars.put(view.bossInstanceId(), bossBar);
        views.put(view.bossInstanceId(), view);
    }

    @Override
    public void updateHealth(BossAudience audience, BossHealthView health) {
        BossBar bossBar = bossBars.get(health.bossInstanceId());
        BossView view = views.get(health.bossInstanceId());
        if (bossBar == null || view == null) {
            return;
        }
        BossView updated = view.withHealth(health);
        views.put(health.bossInstanceId(), updated);
        bossBar.setProgress(health.progress());
        bossBar.setTitle(title(updated));
        synchronizeAudience(bossBar, audience);
    }

    @Override
    public void updatePhase(BossAudience audience, BossPhaseView phase) {
        BossBar bossBar = bossBars.get(phase.bossInstanceId());
        BossView view = views.get(phase.bossInstanceId());
        if (bossBar == null || view == null) {
            return;
        }
        BossView updated = view.withPhase(phase);
        views.put(phase.bossInstanceId(), updated);
        bossBar.setTitle(title(updated));
        synchronizeAudience(bossBar, audience);
    }

    @Override
    public void showAbilityCast(BossAudience audience, BossAbilityCastView ability) {
        String seconds = String.format(Locale.ROOT, "%.1f", ability.remaining().toMillis() / 1000.0);
        Component message = Component.text("⚠ " + ability.displayName() + " en " + seconds + "s");
        forEachOnline(audience, player -> player.sendActionBar(message));
    }

    @Override
    public void showAnnouncement(BossAudience audience, BossAnnouncement announcement) {
        Component message = LEGACY.deserialize(announcement.message());
        forEachOnline(audience, player -> presentAnnouncement(player, announcement.channel(), message));
    }

    @Override
    public void hideBoss(BossAudience audience, UUID bossInstanceId) {
        hideExisting(bossInstanceId);
    }

    private void hideExisting(UUID bossInstanceId) {
        BossBar previous = bossBars.remove(bossInstanceId);
        if (previous != null) {
            previous.removeAll();
            previous.setVisible(false);
        }
        views.remove(bossInstanceId);
    }

    private static void synchronizeAudience(BossBar bossBar, BossAudience audience) {
        bossBar.removeAll();
        forEachOnline(audience, bossBar::addPlayer);
    }

    private static void forEachOnline(BossAudience audience, java.util.function.Consumer<Player> action) {
        audience.playerIds().stream()
            .map(Bukkit::getPlayer)
            .filter(java.util.Objects::nonNull)
            .forEach(action);
    }

    private static void presentAnnouncement(
        Player player,
        BossAnnouncementChannel channel,
        Component message
    ) {
        switch (channel) {
            case CHAT -> player.sendMessage(message);
            case TITLE -> player.showTitle(Title.title(message, Component.empty()));
            case SUBTITLE -> player.showTitle(Title.title(Component.empty(), message));
            case ACTION_BAR, FLOATING_TEXT -> player.sendActionBar(message);
        }
    }

    private static String title(BossView view) {
        String health = NumberFormat.getIntegerInstance(Locale.US).format(view.health().currentHealth());
        String state = view.specialState().isBlank() ? "" : " §8| §6" + view.specialState();
        return "§5§l" + view.displayName()
            + " §8| §d" + view.phase().displayName()
            + " §8| §f" + health + " HP"
            + state;
    }
}
