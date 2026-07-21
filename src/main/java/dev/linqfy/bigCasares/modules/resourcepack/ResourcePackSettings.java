package dev.linqfy.bigCasares.modules.resourcepack;

import org.bukkit.configuration.ConfigurationSection;

import java.net.URI;

public record ResourcePackSettings(
    boolean required,
    boolean resendOnVersionChange,
    String promptMessage,
    ResourcePackPublisher publisher,
    EmbeddedPackHttpSettings embeddedHttp
) {
    public ResourcePackSettings(
        boolean required,
        boolean resendOnVersionChange,
        String promptMessage,
        ResourcePackPublisher publisher
    ) {
        this(required, resendOnVersionChange, promptMessage, publisher, EmbeddedPackHttpSettings.defaults());
    }

    public static ResourcePackSettings load(ConfigurationSection root) {
        ConfigurationSection section = root.getConfigurationSection("resource-pack-system");
        if (section == null) {
            return new ResourcePackSettings(false, true,
                "Este servidor utiliza modelos, música e interfaces custom.",
                ResourcePackPublisher.disabled(), EmbeddedPackHttpSettings.defaults());
        }
        ConfigurationSection publishing = section.getConfigurationSection("publishing");
        String mode = publishing == null ? "copy-only" : publishing.getString("mode", "copy-only");
        String publicUrl = publishing == null ? null : publishing.getString(
            "public-base-url", publishing.getString("public-url"));
        String bindAddress = publishing == null
            ? "127.0.0.1" : publishing.getString("bind-address", "127.0.0.1");
        int port = publishing == null ? 8123 : publishing.getInt("port", 8123);
        int workerThreads = publishing == null ? 2 : publishing.getInt("worker-threads", 2);
        java.net.URI publicBaseUri = publicUrl == null || publicUrl.isBlank()
            ? null : java.net.URI.create(publicUrl.trim());
        return new ResourcePackSettings(
            section.getBoolean("required", false),
            section.getBoolean("resend-on-version-change", true),
            section.getString("prompt-message", "Este servidor utiliza modelos, música e interfaces custom."),
            ResourcePackPublisher.create(mode, publicUrl),
            new EmbeddedPackHttpSettings(bindAddress, port, publicBaseUri, workerThreads)
        );
    }
}
