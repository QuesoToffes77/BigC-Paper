package dev.linqfy.bigCasares.modules.airdrop;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public final class AirdropService {

    private static final int MAX_LOCATION_RETRIES = 5;

    private final AirdropSettings settings;
    private final AirdropWorldGateway world;
    private final AirdropStorage storage;
    private final Random random;
    private final AirdropQualitySelector qualitySelector;
    private final AirdropLootGenerator lootGenerator;

    private Optional<AirdropData> currentDrop;

    public AirdropService(AirdropSettings settings, AirdropWorldGateway world, AirdropStorage storage) {
        this(settings, world, storage, new Random());
    }

    AirdropService(AirdropSettings settings, AirdropWorldGateway world, AirdropStorage storage, Random random) {
        this.settings = settings;
        this.world = world;
        this.storage = storage;
        this.random = random;
        this.qualitySelector = new AirdropQualitySelector(settings.quality().chances());
        this.lootGenerator = new AirdropLootGenerator(settings.quality());
        this.currentDrop = storage.load().map(this::resolveLegacyLoot);
    }

    public boolean isActive() {
        return currentDrop.map(AirdropData::isActive).orElse(false);
    }

    public Optional<AirdropData> spawnAirdrop() {
        if (isActive()) {
            return Optional.empty();
        }

        Optional<AirdropPosition> position = generateValidPosition();
        if (position.isEmpty()) {
            return Optional.empty();
        }

        AirdropType type = selectRandomType();
        AirdropQuality quality = selectRandomQuality();
        AirdropData data = new AirdropData(
            java.util.UUID.randomUUID(), position.get(), type, quality, AirdropPhase.FALLING, true,
            lootGenerator.generate(type, quality, random));
        currentDrop = Optional.of(data);
        storage.save(data);
        return Optional.of(data);
    }

    public Optional<AirdropData> markLanded(AirdropPosition landedPosition) {
        if (currentDrop.isEmpty() || currentDrop.get().phase() != AirdropPhase.FALLING) {
            return Optional.empty();
        }

        AirdropData landed = currentDrop.get().withPositionAndPhase(landedPosition, AirdropPhase.LANDED);
        currentDrop = Optional.of(landed);
        storage.save(landed);
        return Optional.of(landed);
    }

    public boolean claim() {
        if (currentDrop.isEmpty() || currentDrop.get().phase() != AirdropPhase.LANDED) {
            return false;
        }

        AirdropData claimed = currentDrop.get().withPhase(AirdropPhase.CLAIMED);
        currentDrop = Optional.of(claimed);
        storage.save(claimed);
        return true;
    }

    public Optional<AirdropData> getCurrentDrop() {
        return currentDrop;
    }

    public List<AirdropReward> getLootForCurrent() {
        return currentDrop
                .filter(AirdropData::isActive)
                .map(AirdropData::rewards)
                .orElseGet(List::of);
    }

    public AirdropType selectRandomType() {
        AirdropType[] values = AirdropType.values();
        return values[random.nextInt(values.length)];
    }

    public AirdropQuality selectRandomQuality() {
        return qualitySelector.roll(random);
    }

    private AirdropData resolveLegacyLoot(AirdropData data) {
        if (data.lootGenerated() || !data.isActive()) {
            return data;
        }
        AirdropData migrated = data.withGeneratedLoot(
            lootGenerator.generate(data.type(), data.quality(), random));
        storage.save(migrated);
        return migrated;
    }

    private Optional<AirdropPosition> generateValidPosition() {
        Optional<AirdropPosition> playerPos = world.getRandomPlayerPosition();

        for (int attempt = 0; attempt < MAX_LOCATION_RETRIES; attempt++) {
            int x, z;
            if (playerPos.isPresent()) {
                double angle = random.nextDouble() * 2 * Math.PI;
                int maxDistance = Math.max(32, settings.radius());
                int minDistance = Math.max(16, maxDistance / 2);
                int distance = minDistance + random.nextInt(maxDistance - minDistance + 1);
                x = playerPos.get().x() + (int) (Math.cos(angle) * distance);
                z = playerPos.get().z() + (int) (Math.sin(angle) * distance);
            } else {
                x = random.nextInt(settings.radius() * 2 + 1) - settings.radius();
                z = random.nextInt(settings.radius() * 2 + 1) - settings.radius();
            }

            // Chunk loading budget policy: Skip candidate positions in unloaded chunks
            // to avoid triggering synchronous chunk loading or generation during search.
            if (!world.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }

            int groundY = world.getHighestBlockY(x, z);
            int chestY = groundY + 1;

            if (!world.isSafeLandingBlock(x, groundY, z)) {
                continue;
            }
            if (!world.isSafeOpenSpace(x, chestY, z)) {
                continue;
            }
            if (!world.isSafeOpenSpace(x, chestY + 1, z)) {
                continue;
            }

            int spawnY = groundY + settings.dropHeight();
            return Optional.of(new AirdropPosition(x, spawnY, z));
        }
        return Optional.empty();
    }

    public AirdropSettings getSettings() {
        return settings;
    }
}
