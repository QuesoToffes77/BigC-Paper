package dev.linqfy.bigCasares.modules.resourcepack;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.List;

public final class ManualResourcePackMessages {
    private ManualResourcePackMessages() {
    }

    public static void sendDownloads(Player player, List<JavaPackDelivery> downloads) {
        player.sendMessage(Component.text(
            "Modo manual activado. Descargá y activá todos estos packs:", NamedTextColor.YELLOW));
        for (JavaPackDelivery download : downloads) {
            String label = "bettermodel".equals(download.owner()) ? "BetterModel" : "BigCasares";
            player.sendMessage(Component.text("[Descargar " + label + "]", NamedTextColor.AQUA)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(download.uri().toString())));
        }
        player.sendMessage(Component.text(
            "Guardalos en .minecraft/resourcepacks y activalos. Te avisaremos cuando cambien.",
            NamedTextColor.GRAY));
    }
}
