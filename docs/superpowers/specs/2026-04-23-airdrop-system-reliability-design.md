# 2026-04-23-airdrop-system-reliability-design.md

## Objective

Harden the existing airdrop-system module so it survives restarts, validates loot definitions up front, keeps Bukkit concerns outside the service layer, and prevents invalid terrain and duplicate falling-task behavior.

---

## Scope

**In scope**
- Persist an explicit airdrop phase (`FALLING`, `LANDED`, `CLAIMED`)
- Restore active airdrops correctly after restart
- Validate all configured loot materials with `Material.matchMaterial()` at module enable time
- Replace unsafe `Location.equals()` usage with block-coordinate matching
- Tighten terrain validation so drops never land on lava and only land where the chest space is safe
- Prevent duplicate/orphaned falling tasks and cancel schedulers cleanly on disable
- Add and update unit tests for the new rules

**Out of scope**
- Rewriting the module into a different architecture
- Adding multiple simultaneous airdrops
- Changing Minecraft, Java, Gradle, or dependency versions
- Adding mocking libraries or changing unrelated modules

---

## Confirmed Rules

1. Service code must remain free of Bukkit imports.
2. Active airdrops must carry a persisted phase:
   - `FALLING`
   - `LANDED`
   - `CLAIMED`
3. A landing position is valid only when the ground block is safe and the chest space is safe.
4. Lava is always invalid terrain for airdrops.
5. Loot materials must be validated before gameplay begins; invalid definitions fail fast during module enable.
6. The falling task must have at most one active instance per module and must always be cancelled on disable.
7. Restart restoration must resume a falling drop or restore a landed chest without leaking Bukkit details into the service.

---

## Architecture

| Class | Responsibility |
|---|---|
| `AirdropModule` | Validate loot materials, wire Bukkit listener/task/command, restore persisted state, cancel tasks on disable |
| `AirdropService` | Pure domain state machine for spawn, fall/land/claim transitions, safe position selection via gateway |
| `AirdropWorldGateway` | Domain-facing world contract for terrain and space validation |
| `BukkitAirdropWorldGateway` | Bukkit implementation that evaluates block safety and open chest space |
| `AirdropFallingTask` | Visual fall animation plus chest placement callback on land |
| `AirdropListener` | Block-coordinate claim handling and inventory filling using prevalidated materials |
| `YamlAirdropStorage` | Persist and restore `position`, `type`, and `phase`, with backward-compatible legacy reads |
| `AirdropLootMaterialResolver` | Bukkit-only validation helper for `Material.matchMaterial()` fail-fast checks |

---

## Config Shape

```yaml
modules:
  airdrop-system:
    enabled: true

airdrop-system:
  interval-minutes: 30
  radius: 500
  drop-height: 30
```

The module manager must also keep compatibility with older flat boolean entries under `modules.<module-id>`.

---

## Boundaries

- Do not move Bukkit imports into `AirdropService`.
- Do not remove or replace the existing module structure.
- Do not use Mockito; tests must use fakes/in-memory implementations.
- Do not rewrite the loot system from scratch; keep the existing table and validate it at the module boundary.
