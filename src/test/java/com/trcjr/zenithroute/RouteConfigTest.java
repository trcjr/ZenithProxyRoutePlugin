package com.trcjr.zenithroute;

import com.trcjr.zenithroute.RouteConfig.RouteStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RouteConfigTest {
    @Test
    void moveFactoryCreatesMovementStep() {
        RouteStep step = RouteStep.move("Exit", "the_nether", 0, 120, -8220);

        assertEquals("Exit", step.name);
        assertEquals("move", step.type);
        assertEquals("the_nether", step.dimension);
        assertEquals("", step.targetDimension);
        assertFalse(step.isPortal());
    }

    @Test
    void portalFactoryPreservesBothDimensionsAndCoordinates() {
        RouteStep step = RouteStep.portal("Portal", "overworld", "the_nether", -351, 312, 1);

        assertEquals("Portal", step.name);
        assertEquals("portal", step.type);
        assertEquals("overworld", step.dimension);
        assertEquals("the_nether", step.targetDimension);
        assertEquals(-351, step.x);
        assertEquals(312, step.y);
        assertEquals(1, step.z);
        assertTrue(step.isPortal());
    }

    @Test
    void newConfigStartsSafeAndInactive() {
        RouteConfig config = new RouteConfig();

        assertTrue(config.steps.isEmpty());
        assertFalse(config.enabled);
        assertFalse(config.paused);
        assertFalse(config.portalTransitionArmed);
        assertEquals(0, config.currentIndex);
        assertEquals(2, config.moveArrivalRadius);
        assertEquals("STOPPED", config.executionState);
        assertEquals("", config.failureReason);
    }
}
