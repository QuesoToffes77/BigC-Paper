# Team Identity, Nexus State, and Protected Containers Design

## Goal

Make Nexus ownership and presentation follow the current team identity, expose complete owner-controlled team naming/tag styling, show attack state only while real attackers remain nearby, and persist containers that team members add to their Nexus.

## Team identity and appearance

`TeamId` remains the durable identity. Team names and tags are mutable presentation data. `/team rename <name>` changes a unique, validated display name. `/team tag <tag>` accepts two to five visible grapheme clusters from a configurable safe Unicode allowlist. Validation rejects formatting markers, control characters, bidi controls, zero-width characters, and line breaks.

`TeamTagStyle` stores bold, italic, underline, strikethrough, color, and wrapper. Owners edit it through `/team appearance`; wrappers include square, angle, round, full-width square, and none. Rendering is centralized so chat, scoreboard, placeholders, and Nexus visuals use identical current data.

Existing YAML teams migrate with the current color, bold enabled, and square brackets. Only owners can rename or edit tag appearance.

## Nexus team binding and presentation

Persistent Nexus records store `TeamId`, not copied team names or tags. A team presentation gateway resolves the current team whenever a visual is created or refreshed. Successful team presentation changes trigger an immediate refresh of that team's active Nexus.

The neutral hologram state is `SEGURO`. `CRÍTICO` reflects health at or below 25 percent and is independent from attack tracking.

## Active attackers

A successful enemy damage event registers that player's UUID against the Nexus. `BAJO ATAQUE` remains active while at least one registered attacker is online, alive, in the Nexus world, and within a 60-block radius of the Nexus origin. A repeating main-thread task removes invalid attackers and refreshes the display only when state changes. Death, quit, world change, Nexus destruction, or leaving the radius clears the relevant association.

Friendly or rejected damage never creates attack state. If the Nexus is critical and under attack, the display prioritizes `BAJO ATAQUE`; once no attacker remains it shows `CRÍTICO`.

## Protected team containers

When a team member places an allowed container inside their own Nexus protection volume, its world and block coordinates are persisted under the Nexus and `TeamId`. Any current team member may open, break, and remove it. Non-members cannot open, break, burn, explode, or piston-move it.

Double chests register both halves and are removed consistently when broken. Containers already present in a protected volume can be adopted during Nexus recovery only when they belong unambiguously to that Nexus. Destroying a Nexus or dissolving its team removes protection metadata without deleting blocks or inventories.

Persistence uses a dedicated YAML store written atomically after registration/removal and loaded during module startup. Stale records whose blocks are no longer containers are pruned safely.

## Configuration

Nexus attack radius defaults to `60.0`. Existing placement protection dimensions remain authoritative for container registration. Team configuration includes Unicode categories plus explicit allowed symbols/emojis and supported wrappers.

## Verification

Unit tests cover grapheme-aware validation, unsafe Unicode rejection, rename uniqueness, style YAML migration, rendering, team-change Nexus refresh, attacker lifecycle, member authorization, double chests, piston/explosion protection, and container persistence. The final run includes the complete Gradle test/build suite and a Paper `runServer` smoke test.
