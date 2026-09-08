package com.trcjr.zenithroute;

import com.trcjr.zenithroute.RouteConfig.RouteStep;
import org.junit.jupiter.api.Test;

import static com.trcjr.zenithroute.RouteLogic.PortalState.*;
import static org.junit.jupiter.api.Assertions.*;

class RouteLogicTest {
    @Test
    void unarmedPortalInTargetDimensionIsNotComplete() {
        RouteStep step = RouteStep.portal("OWToNether", "overworld", "the_nether", 10, 70, 20);

        assertEquals(WRONG_DIMENSION, RouteLogic.portalState(step, false, "the_nether"));
    }

    @Test
    void reproducesAndPreventsOriginalReversedPortalBug() {
        RouteStep reversed = RouteStep.portal("BadStep", "the_nether", "overworld", -351, 312, 1);

        assertEquals(WRONG_DIMENSION, RouteLogic.portalState(reversed, false, "overworld"));
    }

    @Test
    void unarmedPortalInSourceDimensionIsReadyToPath() {
        RouteStep step = RouteStep.portal("OWToNether", "overworld", "the_nether", 10, 70, 20);

        assertEquals(READY_TO_PATH, RouteLogic.portalState(step, false, "overworld"));
    }

    @Test
    void armedPortalInSourceDimensionWaits() {
        RouteStep step = RouteStep.portal("OWToNether", "overworld", "the_nether", 10, 70, 20);

        assertEquals(WAITING_FOR_TRANSITION, RouteLogic.portalState(step, true, "overworld"));
    }

    @Test
    void issuedPortalGoalFollowedByTargetDimensionCompletesEvenWithoutPathCompletion() {
        RouteStep step = RouteStep.portal("OWToNether", "overworld", "the_nether", 10, 70, 20);

        assertEquals(TRANSITION_COMPLETE, RouteLogic.portalState(step, true, "the_nether"));
    }

    @Test
    void unrelatedDimensionNeverCompletesPortal() {
        RouteStep step = RouteStep.portal("OWToNether", "overworld", "the_nether", 10, 70, 20);

        assertEquals(WRONG_DIMENSION, RouteLogic.portalState(step, false, "the_end"));
        assertEquals(WRONG_DIMENSION, RouteLogic.portalState(step, true, "the_end"));
    }

    @Test
    void normalMovementStepIsNotPortalState() {
        RouteStep step = RouteStep.move("Highway", "the_nether", 0, 120, -1000);

        assertEquals(NOT_A_PORTAL, RouteLogic.portalState(step, false, "the_nether"));
    }

    @Test
    void dimensionAliasesNormalizeToZenithNames() {
        assertEquals("overworld", RouteLogic.normalizeDimension("overworld"));
        assertEquals("overworld", RouteLogic.normalizeDimension("OW"));
        assertEquals("the_nether", RouteLogic.normalizeDimension("nether"));
        assertEquals("the_nether", RouteLogic.normalizeDimension("the_nether"));
        assertEquals("the_end", RouteLogic.normalizeDimension("end"));
        assertEquals("the_end", RouteLogic.normalizeDimension("the_end"));
    }

    @Test
    void unknownDimensionIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> RouteLogic.normalizeDimension("moon"));
        assertThrows(IllegalArgumentException.class, () -> RouteLogic.normalizeDimension(null));
    }

    @Test
    void movementGoalAcceptsNearbyReachableFeetPosition() {
        RouteStep step = RouteStep.move("Approach", "overworld", -356, 312, 0);
        var goal = RouteLogic.goalFor(step, 2);

        assertTrue(goal.isInGoal(-355, 312, 0));
        assertTrue(goal.isInGoal(-356, 311, 0));
        assertFalse(goal.isInGoal(-353, 312, 0));
    }

    @Test
    void portalGoalRemainsExactDespiteMovementRadius() {
        RouteStep step = RouteStep.portal("Portal", "overworld", "the_nether", -351, 312, 1);
        var goal = RouteLogic.goalFor(step, 8);

        assertTrue(goal.isInGoal(-351, 312, 1));
        assertFalse(goal.isInGoal(-350, 312, 1));
        assertFalse(goal.isInGoal(-351, 311, 1));
    }

    @Test
    void invalidMovementRadiusIsRejected() {
        RouteStep step = RouteStep.move("Approach", "overworld", 0, 64, 0);

        assertThrows(IllegalArgumentException.class, () -> RouteLogic.goalFor(step, 0));
        assertThrows(IllegalArgumentException.class, () -> RouteLogic.goalFor(step, 9));
    }
}
