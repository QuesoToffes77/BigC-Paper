package dev.linqfy.bigCasares.modules.resourcepack;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ManualResourcePackTracker {
    public static final String MANUAL_TAG = "bigcasares_resourcepack_manual";
    private static final String REVISION_PREFIX = "bigcasares_resourcepack_revision_";

    private ManualResourcePackTracker() {
    }

    public static Set<String> tagsFor(Set<String> currentTags, String revision) {
        validateRevision(revision);
        Set<String> tags = new LinkedHashSet<>(currentTags);
        tags.removeIf(tag -> tag.startsWith(REVISION_PREFIX));
        tags.add(MANUAL_TAG);
        tags.add(REVISION_PREFIX + revision);
        return Set.copyOf(tags);
    }

    public static boolean isManual(Set<String> tags) {
        return tags.contains(MANUAL_TAG);
    }

    public static boolean needsUpdate(Set<String> tags, String revision) {
        validateRevision(revision);
        return isManual(tags) && !tags.contains(REVISION_PREFIX + revision);
    }

    public static boolean isManagedTag(String tag) {
        return MANUAL_TAG.equals(tag) || tag.startsWith(REVISION_PREFIX);
    }

    private static void validateRevision(String revision) {
        if (revision == null || !revision.matches("[0-9a-f]{16}")) {
            throw new IllegalArgumentException("revision must be a 16-character lowercase hexadecimal digest");
        }
    }
}
