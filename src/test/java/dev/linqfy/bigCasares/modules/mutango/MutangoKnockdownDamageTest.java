package dev.linqfy.bigCasares.modules.mutango;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MutangoKnockdownDamageTest {
    @Test
    void creditsOnlyTheHealthActuallyRemovedByKnockdown() {
        List<Double> credited = new ArrayList<>();
        assertTrue(MutangoCombat.knockDownAndCredit(20.0, () -> true, () -> 1.0, credited::add));
        assertEquals(List.of(19.0), credited);
    }

    @Test
    void doesNotCreditWhenNoKnockdownOccurs() {
        List<Double> credited = new ArrayList<>();
        assertFalse(MutangoCombat.knockDownAndCredit(20.0, () -> false, () -> 1.0, credited::add));
        assertTrue(credited.isEmpty());
    }

    @Test
    void noHealthLossProducesNoCredit() {
        List<Double> credited = new ArrayList<>();
        assertTrue(MutangoCombat.knockDownAndCredit(1.0, () -> true, () -> 1.0, credited::add));
        assertTrue(credited.isEmpty());
    }
}
