package dev.linqfy.bigCasares;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformVersionConfigurationTest {

    private final Path projectRoot = Path.of(System.getProperty("user.dir"));

    @Test
    void targetsMinecraftTwentySixTwoOnJavaTwentyFive() throws IOException {
        String build = Files.readString(projectRoot.resolve("build.gradle"));
        String plugin = Files.readString(projectRoot.resolve("src/main/resources/plugin.yml"));
        String wrapper = Files.readString(projectRoot.resolve("gradle/wrapper/gradle-wrapper.properties"));

        assertTrue(build.contains("id(\"xyz.jpenilla.run-paper\") version \"3.0.2\""));
        assertTrue(build.contains("https://repo.papermc.io/repository/maven-public/"));
        assertTrue(build.contains("io.papermc.paper:paper-api:26.2.build.56-alpha"));
        assertTrue(build.contains("minecraftVersion(\"26.2\")"));
        assertTrue(build.contains("def targetJavaVersion = 25"));
        assertTrue(plugin.contains("api-version: '26.2'"));
        assertTrue(wrapper.contains("gradle-9.2.1-bin.zip"));
    }
}
