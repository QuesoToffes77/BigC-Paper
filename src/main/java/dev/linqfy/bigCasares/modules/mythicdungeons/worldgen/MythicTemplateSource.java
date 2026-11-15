package dev.linqfy.bigCasares.modules.mythicdungeons.worldgen;

import java.util.List;

/**
 * Supplies extra structure templates (for example re-themed third-party rooms) that
 * {@link MythicDungeonDatapack#build(List)} merges into a culture's dungeon.
 *
 * <p>Contract:</p>
 * <ul>
 *     <li>{@link #piecesFor(String)} receives a culture id ({@code greek}, {@code norse}, ...
 *     see {@link MythicDungeonTheme#all()}) and returns the pieces for it, or an empty list.
 *     It must be deterministic: the same files must yield the same pieces in the same order,
 *     because the datapack hash (and therefore "restart needed") is derived from the bytes.</li>
 *     <li>Sources ship inside the plugin (e.g. templates read from the plugin's own resources).
 *     If a source throws or yields an invalid piece, {@link MythicDatapackInstaller} skips that
 *     source with a warning and still installs the rest of the pack.</li>
 *     <li>Pieces are validated (vanilla-only ids, connector present and correctly oriented,
 *     size limits for their role); an invalid piece rejects its whole source. The pack builder
 *     rewrites every jigsaw block of an extra piece, so the template's original pool names
 *     never matter. The boss arena and its lodestone altar remain code-built and are always
 *     reachable; extra pieces can never replace them.</li>
 * </ul>
 */
@FunctionalInterface
public interface MythicTemplateSource {

    List<MythicExtraPiece> piecesFor(String culture);
}
