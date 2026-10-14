# Historical designs, plans and handoffs

These dated records preserve intent, tradeoffs and past validation. They are not
current setup instructions or proof that a feature is complete. Start with the
[current docs](../README.md) and verify any historical claim against source/tests.

- [Specs](specs/): gameplay rules, architecture proposals and investigations.
- [Plans](plans/): implementation tasks and occasional completion notes.
- [Handoffs](handoffs/): status at the time of a session transfer.

Use the filename's date/topic to select a record. Related records can supersede
one another without updating old task checkboxes. In particular:

- April and some July records use Paper 1.21.11/Java 21; current build targets
  Paper 26.2/Java 25. The Oraxen investigation contains a pre-upgrade baseline.
- The July 15 reload handoff says Phase 0 is incomplete; the matching plan later
  records fixes. Current scopes coexist with global fallback cleanup; full plugin
  reload does not restore the old runtime on failure.
- Early special-item notes use 10 nuke rings/130 TNT; later notes and current code
  use 6 rings/350 TNT. Follow `SpecialItemsSettings` and effective server config.
- Early Acid Rain proposals include destructive whitelists and larger budgets;
  shipped configuration has an empty whitelist and zero block budgets. Nitric
  acid was subsequently added to mob drops despite an earlier catalog exclusion.
- The grappling-hook design's first tier table differs from its later YAML/current
  enum. Follow `GrapplingHookTier` and the settings loader for effective ranges.
- The [PR #11 review](../pr-11-code-review.md) is a past snapshot; its damage prose
  does not consistently distinguish health points from hearts.

Records remain in their existing paths so links and design provenance survive.
New substantial work can use the same dated convention; routine doc edits and
small fixes do not require new specs, plans or copied agent transcripts.
