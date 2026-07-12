package dev.linqfy.bigCasares.modules.resourcepack;

import org.bukkit.configuration.ConfigurationSection;

import java.net.URI;

public record ResourcePackSettings(
    boolean required,
    boolean resendOnVersionChange,
    String promptMessage,
    ResourcePackPublisher publisher
) {
    public static ResourcePackSettings load(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("resource-pack-system");
        if (section == null) {
            return new ResourcePackSettings(false, true,
                "Este servidor utiliza modelos, música e interfaces custom.",
                ResourcePackPublisher.disabled());
        }
        ConfigurationSection publishing = section.getConfigurationSection("publishing");
        String mode = publishing == null ? "copy-only" : publishing.getString("mode", "copy-only");
        String publicUrl = publishing == null ? null : publishing.getString("public-url");
        return new ResourcePackSettings(
            section.getBoolean("required", false),
            section.getBoolean("resend-on-version-change", true),
            section.getString("prompt-message", "Este servidor utiliza modelos, música e interfaces custom."),
            ResourcePackPublisher.create(mode, publicUrl)
        );
    }
}
