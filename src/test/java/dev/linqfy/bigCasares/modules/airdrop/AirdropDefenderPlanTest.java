package dev.linqfy.bigCasares.modules.airdrop;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure tests for the defender wave planner: the hard {@code maxTotal} ceiling
 * is never exceeded, rider chances gate the rider count, and disabled mobs
 * plan nothing.
 */
class AirdropDefenderPlanTest {

    @Test
    void phantomCreeperDefendersStartAtEpicAndScaleUp() {
        AirdropMobSettings settings = AirdropMobSettings.defaults();
        AirdropQualitySettings qualities = AirdropQualitySettings.defaults();

        assertEquals(0, qualityPlan(settings, qualities, AirdropQuality.COMMON).phantomCreepers());
        assertEquals(0, qualityPlan(settings, qualities, AirdropQuality.RARE).phantomCreepers());
        assertEquals(1, qualityPlan(settings, qualities, AirdropQuality.EPIC).phantomCreepers());
        assertEquals(2, qualityPlan(settings, qualities, AirdropQuality.LEGENDARY).phantomCreepers());
        assertEquals(3, qualityPlan(settings, qualities, AirdropQuality.GHISTIC).phantomCreepers());
    }

    @Test
    void qualityPlanCountsBothEntitiesInEachPhantomCreeperPair() {
        AirdropMobSettings settings = AirdropMobSettings.defaults();
        AirdropQualitySettings qualities = AirdropQualitySettings.defaults();

        for (AirdropQuality quality : AirdropQuality.values()) {
            AirdropDefenderMobs.Plan plan = qualityPlan(settings, qualities, quality);
            assertEquals(qualities.profile(quality).guardCount(), plan.totalEntities());
        }
    }

    @Test
    void qualityPlanCountsBothEntitiesInEachHorseRiderPair() {
        AirdropMobSettings settings = new AirdropMobSettings(
            true, 8, AirdropZombieSettings.defaults(),
            new AirdropHorseRiderSettings(true, 4, 100),
            new AirdropPhantomCreeperSettings(false, 0, 0, 0, 30, 12));
        AirdropQualityProfile profile = AirdropQualitySettings.defaults()
            .profile(AirdropQuality.EPIC);

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(
            settings, AirdropQuality.EPIC, profile, new Random(1));

        assertEquals(8, plan.totalEntities());
        assertEquals(2, plan.riders());
        assertEquals(4, plan.zombies());
    }

    @Test
    void phantomCreeperFireResistanceLastsThirtyMinutesByDefault() {
        assertEquals(36_000, AirdropMobSettings.defaults().phantomCreepers().fireResistanceTicks());
    }

    @Test
    void disabledMobsPlanNothing() {
        assertEquals(new AirdropDefenderMobs.Plan(0, 0),
            AirdropDefenderMobs.plan(AirdropMobSettings.disabled(), new Random(1)));
        assertEquals(new AirdropDefenderMobs.Plan(0, 0),
            AirdropDefenderMobs.plan(new AirdropMobSettings(false, 40,
                AirdropZombieSettings.defaults(), AirdropHorseRiderSettings.defaults()), new Random(1)));
    }

    @Test
    void zeroMaxTotalPlansNothing() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 0,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(true, 2, 100));

        assertEquals(new AirdropDefenderMobs.Plan(0, 0),
            AirdropDefenderMobs.plan(settings, new Random(1)));
    }

    @Test
    void zeroChanceNeverPlansRiders() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 40,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(true, 2, 0));

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(settings, new Random(1));

        assertEquals(20, plan.zombies());
        assertEquals(0, plan.riders());
    }

    @Test
    void fullChancePlansAllRiderSlotsWithinCapacity() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 40,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(true, 2, 100));

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(settings, new Random(1));

        assertEquals(20, plan.zombies());
        assertEquals(2, plan.riders());
    }

    @Test
    void maxTotalCapsZombiesFirstAndLeavesNoRoomForRiders() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 10,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(true, 2, 100));

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(settings, new Random(1));

        assertEquals(10, plan.zombies());
        assertEquals(0, plan.riders());
    }

    @Test
    void ridersAreCappedByTheRemainingBudget() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 21,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(true, 3, 100));

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(settings, new Random(1));

        assertEquals(20, plan.zombies());
        assertEquals(1, plan.riders(), "only one slot remains after the 20 zombies");
    }

    @Test
    void disabledRidersNeverPlanRiders() {
        AirdropMobSettings settings = new AirdropMobSettings(true, 40,
            new AirdropZombieSettings(20, 12, 40),
            new AirdropHorseRiderSettings(false, 2, 100));

        AirdropDefenderMobs.Plan plan = AirdropDefenderMobs.plan(settings, new Random(1));

        assertEquals(20, plan.zombies());
        assertEquals(0, plan.riders());
    }

    private static AirdropDefenderMobs.Plan qualityPlan(
        AirdropMobSettings settings,
        AirdropQualitySettings qualities,
        AirdropQuality quality
    ) {
        return AirdropDefenderMobs.plan(
            settings, quality, qualities.profile(quality), new Random(quality.ordinal()));
    }
}
