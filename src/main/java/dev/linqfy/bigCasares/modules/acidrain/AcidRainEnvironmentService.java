package dev.linqfy.bigCasares.modules.acidrain;

import org.bukkit.Material;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

public final class AcidRainEnvironmentService {
    private static final int MAX_QUEUED_CANDIDATES = 4096;

    private AcidRainEnvironmentSettings settings;
    private AcidRainBlockSafetyPolicy safetyPolicy;
    private final Queue<AcidRainErosionCandidate> queue = new ArrayDeque<>();
    private final Set<String> queuedKeys = new HashSet<>();
    private int brokenThisEvent;
    private int brokenInCurrentSecond;
    private long currentSecondEpoch = Long.MIN_VALUE;

    public AcidRainEnvironmentService(AcidRainEnvironmentSettings settings) {
        updateSettings(settings);
    }

    public synchronized void updateSettings(AcidRainEnvironmentSettings settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
        this.safetyPolicy = AcidRainBlockSafetyPolicy.fromSettings(settings);
    }

    public synchronized boolean enqueue(AcidRainErosionCandidate candidate) {
        if (candidate == null || !settings.destruction().canDestroyBlocks()) {
            return false;
        }
        String key = candidate.key();
        if (queuedKeys.contains(key)) {
            return false;
        }
        if (queue.size() >= MAX_QUEUED_CANDIDATES) {
            // Refuse to grow the queue without bound when players stand in dense
            // whitelisted terrain; the per-second and per-event caps still apply.
            return false;
        }
        queue.add(candidate);
        queuedKeys.add(key);
        return true;
    }

    /**
     * Processes queued candidates for one destruction cycle. Destruction only
     * happens while the storm is {@code ACTIVE}: WARNING, ENDING and INACTIVE
     * never mutate blocks through this service. The global
     * {@code max-blocks-per-second} is enforced as a real per-second budget,
     * so more frequent level cycles cannot exceed it.
     */
    public synchronized AcidRainEnvironmentTickResult processSecond(
        AcidRainBlockAccess access,
        AcidRainSnapshot storm,
        Instant now
    ) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(storm, "storm");
        Objects.requireNonNull(now, "now");
        AcidRainDestructionSettings destruction = settings.destruction();
        if (!destruction.canDestroyBlocks()) {
            queue.clear();
            queuedKeys.clear();
            return new AcidRainEnvironmentTickResult(0, 0, brokenThisEvent, queue.size());
        }
        if (storm.state() != AcidRainState.ACTIVE) {
            return new AcidRainEnvironmentTickResult(0, 0, brokenThisEvent, queue.size());
        }

        long second = now.getEpochSecond();
        if (second != currentSecondEpoch) {
            currentSecondEpoch = second;
            brokenInCurrentSecond = 0;
        }
        int eventRemaining = Math.max(0, destruction.maxBlocksPerEvent() - brokenThisEvent);
        int perSecondRemaining = Math.max(0, destruction.maxBlocksPerSecond() - brokenInCurrentSecond);
        int limit = Math.min(perSecondRemaining, eventRemaining);
        int broken = 0;
        int skipped = 0;

        while (broken < limit && !queue.isEmpty()) {
            AcidRainErosionCandidate candidate = queue.poll();
            queuedKeys.remove(candidate.key());
            if (!candidate.withinRadius()) {
                // The player moved or the candidate is stale: never erode outside
                // the configured radius of the original origin.
                skipped++;
                continue;
            }
            Material material = access.materialAt(candidate);
            if (safetyPolicy.canErode(material) && access.canBreak(candidate)) {
                access.breakBlock(candidate);
                broken++;
                brokenThisEvent++;
                brokenInCurrentSecond++;
            } else {
                skipped++;
            }
        }
        return new AcidRainEnvironmentTickResult(broken, skipped, brokenThisEvent, queue.size());
    }

    public synchronized void resetEvent() {
        queue.clear();
        queuedKeys.clear();
        brokenThisEvent = 0;
        brokenInCurrentSecond = 0;
        currentSecondEpoch = Long.MIN_VALUE;
    }

    public synchronized int queuedCandidates() {
        return queue.size();
    }
}

record AcidRainErosionCandidate(String worldName, int originX, int originZ, int radius, int x, int y, int z) {
    String key() {
        return worldName + ":" + x + ":" + y + ":" + z;
    }

    boolean withinRadius() {
        if (radius <= 0) {
            return false;
        }
        long dx = (long) x - originX;
        long dz = (long) z - originZ;
        return dx * dx + dz * dz <= (long) radius * radius;
    }
}

record AcidRainEnvironmentTickResult(int brokenBlocks, int skippedBlocks, int brokenThisEvent, int queuedBlocks) {
}

interface AcidRainBlockAccess {
    Material materialAt(AcidRainErosionCandidate candidate);

    boolean canBreak(AcidRainErosionCandidate candidate);

    void breakBlock(AcidRainErosionCandidate candidate);
}
