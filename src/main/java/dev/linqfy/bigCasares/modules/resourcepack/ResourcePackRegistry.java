package dev.linqfy.bigCasares.modules.resourcepack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ResourcePackRegistry(List<ResourcePackAsset> assets) {

    public ResourcePackRegistry {
        assets = List.copyOf(assets);
        Set<String> ids = new HashSet<>();
        for (ResourcePackAsset asset : assets) {
            if (!ids.add(asset.id())) {
                throw new IllegalArgumentException("Duplicate resource-pack asset id: " + asset.id());
            }
        }
    }

    public static ResourcePackRegistry load(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Resource-pack registry does not exist: " + file);
        }

        List<ResourcePackAsset> assets = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        String currentId = null;
        Map<String, String> values = new LinkedHashMap<>();
        boolean insideAssets = false;

        for (String rawLine : Files.readAllLines(file)) {
            String withoutComment = stripComment(rawLine);
            if (withoutComment.isBlank()) {
                continue;
            }
            int indent = leadingSpaces(withoutComment);
            String line = withoutComment.trim();
            if (indent == 0) {
                if (!line.equals("assets:")) {
                    throw new IllegalArgumentException("Unsupported registry root entry: " + line);
                }
                insideAssets = true;
                continue;
            }
            if (!insideAssets) {
                throw new IllegalArgumentException("Registry must start with assets:");
            }
            if (indent == 2 && line.endsWith(":")) {
                if (currentId != null) {
                    assets.add(toAsset(currentId, values));
                }
                currentId = line.substring(0, line.length() - 1).trim();
                if (!ids.add(currentId)) {
                    throw new IllegalArgumentException("Duplicate resource-pack asset id: " + currentId);
                }
                values = new LinkedHashMap<>();
                continue;
            }
            if (indent >= 4 && currentId != null) {
                int separator = line.indexOf(':');
                if (separator < 1) {
                    throw new IllegalArgumentException("Invalid registry property: " + line);
                }
                String key = line.substring(0, separator).trim();
                String value = unquote(line.substring(separator + 1).trim());
                values.put(key, value);
                continue;
            }
            throw new IllegalArgumentException("Invalid registry indentation: " + rawLine);
        }
        if (currentId != null) {
            assets.add(toAsset(currentId, values));
        }
        if (assets.isEmpty()) {
            throw new IllegalArgumentException("Resource-pack registry contains no assets");
        }
        return new ResourcePackRegistry(assets);
    }

    private static ResourcePackAsset toAsset(String id, Map<String, String> values) {
        return new ResourcePackAsset(
            id,
            values.get("type"),
            values.get("java-model"),
            values.get("bedrock-entity"),
            values.get("texture"),
            values.get("java-sound"),
            values.get("bedrock-sound")
        );
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if ((current == '\'' || current == '"') && (!quoted || quote == current)) {
                quoted = !quoted;
                quote = current;
            }
            if (current == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
