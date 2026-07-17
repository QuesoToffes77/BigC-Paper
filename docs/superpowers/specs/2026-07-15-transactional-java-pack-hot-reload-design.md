# Transactional Java Pack Hot Reload Design

**Date:** 2026-07-15
**Phase:** 1
**Source:** `2026-07-15-oraxen-inspired-content-runtime-investigation.md`

## Objective

Allow an operator to edit resource-pack source files under the BigCasares data folder, build and validate a candidate in isolation, atomically activate a content-addressed Java pack, and resend it without replacing the working pack on failure. The implementation remains a clean-room BigCasares design built around the existing strict deterministic builders.

## Scope

### In scope

- Package the repository `resourcepack/` tree as seed content and extract missing files without overwriting operator edits.
- Treat `plugins/BigCasares/content/pack/` as the runtime-authoritative pack source.
- Build candidates in unique staging directories and clean them after every terminal result.
- Merge BetterModel Java output only through an explicit ordered layer provider with strict conflicts.
- Validate source references and final Java/Bedrock archives, including resource paths, JSON syntax, PNG readability, and ZIP safety.
- Derive version, UUID, SHA-1, and SHA-256 from the final merged Java bytes.
- Publish immutable content-addressed artifacts and atomically replace a small active manifest.
- Keep the previous artifact available after a swap.
- Provide single-flight structured pack reload results with truthful failure phases and no-op detection.
- Support copy-only, external URL, and embedded HTTP delivery modes.
- Serve only active/preserved content-addressed artifacts with immutable cache headers and no path traversal or directory listing.
- Integrate `reload pack`, `pack info`, and `pack send <player|all>` into `/bigcasares`.
- Resend the new Java pack once after a changed commit and ignore stale status events from the previous pack.

### Out of scope

- Declarative items, recipes, sounds, or entity-presentation catalogs.
- Item identity migration or inventory/entity reconciliation.
- Dynamic Geyser definition replacement; Bedrock changes are built but reported restart-required.
- BetterModel command execution or dynamic BetterModel model reload.
- Upload providers, CDN APIs, trusted-proxy handling, TLS termination, or pack obfuscation.
- Removing the Phase 0 listener/task fallback.

## Confirmed rules

- The previous active manifest and URL remain untouched until the candidate is fully built, merged, validated, published, and ready to commit.
- Final merged Java bytes, including BetterModel layers, determine pack identity.
- Equal final Java SHA-256 is a successful no-op and must not resend.
- Strict non-metadata conflicts remain failures.
- Every staging directory is unique and removed after success, no-op, or failure.
- Active manifest replacement uses an atomic move where the filesystem supports it and a same-filesystem replacement fallback otherwise.
- Published artifact filenames contain their SHA-256 prefix and are never overwritten with different bytes.
- Embedded HTTP serves exact filenames only from the artifact directory, streams files, and keeps old files readable during swaps.
- The module owns its worker, HTTP server, and callbacks through its Phase 0 runtime scope.
- The commit and player resend execute on the Bukkit primary thread; discovery/build/validation run on the pack worker.
- A module shutdown invalidates the coordinator so a completed worker cannot commit afterward.
- Existing `bigcasares.shop.reload` remains a compatibility alias; new pack operations require `bigcasares.pack.reload`, `bigcasares.pack.info`, or `bigcasares.pack.send`.

## Runtime layout

```text
plugins/BigCasares/
  content/
    pack/
      java/...
      bedrock/...
      shared/registry.yml
  packs/
    active-pack.json
    artifacts/
      bigcasares-java-<sha-prefix>.zip
      bigcasares-bedrock-<sha-prefix>.mcpack
    .staging/
      <job-id>/...
```

## Architecture

### Seed extraction

Gradle creates one reproducible `bigcasares-content-seed.zip` from `resourcepack/` and embeds it in the plugin. `RuntimePackSourceSeeder` validates ZIP entry paths and copies only missing regular files into `content/pack`.

### Candidate build

`StagedPackBuilder` creates a job directory, delegates base Java/Bedrock generation to the existing deterministic builders, applies ordered `JavaPackLayerProvider` archives through `MergedJavaResourcePackBuilder`, validates the completed archives, and computes final-byte identities. It returns a candidate whose staging directory is closeable and never publishes directly.

### Publication and active manifest

`PackArtifactPublisher` moves/copies candidate archives to hash-addressed immutable names, verifies an existing same-name artifact has identical bytes, and atomically writes `active-pack.json`. `ActivePackManifest` records final hashes, UUID, filenames, activation time, and delivery URI. The previously named artifact is retained.

### Coordination

`PackReloadCoordinator` provides one active job, immutable status, phase-specific failure results, no-op detection against the active manifest, cancellation/retirement, and an asynchronous prepare plus caller-supplied commit executor. The commit updates the module's live delivery snapshot before resending.

### HTTP delivery

`EmbeddedPackHttpServer` binds the configured address and port, exposes `/packs/<content-addressed-file>`, rejects every other path, streams the file, sets immutable caching, and stops idempotently. `public-base-url` is required so client-facing URLs are never inferred from an untrusted request host.

### Runtime service transition

`ResourcePackService` holds an atomic current delivery plus the immediately previous pack ID. Requests always use the current manifest/URI. Status updates mutate player state only for the current pack ID; late status events for the previous pack are recognized but ignored.

## Failure semantics

- Seed or discovery failure: old manifest remains active.
- Source/final validation failure: staging is cleaned; old manifest and URL remain active.
- Layer conflict or build failure: staging is cleaned; old manifest remains active.
- Publication failure before manifest swap: old manifest remains active; immutable candidate files may remain harmlessly unreferenced.
- Commit cancellation after module shutdown: candidate is not activated or resent.
- HTTP startup failure: module reports embedded delivery unavailable and does not claim the candidate is deliverable.

## Verification

- Same source and layers produce byte-identical artifacts and a no-op second reload.
- Invalid JSON, PNG, registry reference, unsafe ZIP entry, or merge conflict cannot replace the active manifest.
- BetterModel-only changes alter final identity and published URL.
- Concurrent requests yield one running job and one `ALREADY_RUNNING` result.
- A streamed old artifact remains readable after active manifest replacement.
- Full existing gameplay and Phase 0 tests remain green.
