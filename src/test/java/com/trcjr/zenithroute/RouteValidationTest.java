package com.trcjr.zenithroute;

import com.trcjr.zenithroute.RouteConfig.RouteStep;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteValidationTest {
    @Test
    void acceptsContinuousOverworldNetherOverworldRoute() {
        var steps = List.of(
            RouteStep.move("SpawnApproach", "overworld", 10, 70, 10),
            RouteStep.portal("SpawnPortal", "overworld", "the_nether", 11, 70, 10),
            RouteStep.move("Highway", "the_nether", 100, 120, -1000),
            RouteStep.portal("BasePortal", "the_nether", "overworld", 101, 120, -1000),
            RouteStep.move("Base", "overworld", 808, 70, -8000)
        );

        var result = RouteLogic.validateRoute(steps, 2);

        assertTrue(result.valid(), result.describe());
        assertTrue(result.warnings().isEmpty(), result.describe());
    }

    @Test
    void rejectsReversedPortalAfterOverworldMove() {
        var steps = List.of(
            RouteStep.move("Approach", "overworld", -356, 312, 0),
            RouteStep.portal("Reversed", "the_nether", "overworld", -351, 200, 1)
        );

        var result = RouteLogic.validateRoute(steps, 2);

        assertFalse(result.valid());
        assertTrue(result.describe().contains("starts in the_nether"));
        assertTrue(result.describe().contains("leaves the bot in overworld"));
    }

    @Test
    void rejectsStepAfterPortalInWrongDimension() {
        var steps = List.of(
            RouteStep.move("Approach", "overworld", 0, 70, 0),
            RouteStep.portal("Portal", "overworld", "the_nether", 1, 70, 0),
            RouteStep.move("WrongSide", "overworld", 8, 70, 0)
        );

        assertFalse(RouteLogic.validateRoute(steps, 2).valid());
    }

    @Test
    void rejectsBlankAndDuplicateNamesCaseInsensitively() {
        var steps = List.of(
            RouteStep.move("", "overworld", 0, 70, 0),
            RouteStep.move("Same", "overworld", 1, 70, 0),
            RouteStep.move("same", "overworld", 2, 70, 0)
        );

        String description = RouteLogic.validateRoute(steps, 2).describe();
        assertTrue(description.contains("blank name"));
        assertTrue(description.contains("duplicate name"));
    }

    @Test
    void rejectsUnknownStepType() {
        RouteStep step = RouteStep.move("Mystery", "overworld", 0, 70, 0);
        step.type = "teleport";

        assertTrue(RouteLogic.validateRoute(List.of(step), 2).describe().contains("unknown type"));
    }

    @Test
    void rejectsUnknownDimension() {
        RouteStep step = RouteStep.move("Moon", "moon", 0, 70, 0);

        assertTrue(RouteLogic.validateRoute(List.of(step), 2).describe().contains("dimension is invalid"));
    }

    @Test
    void rejectsSamePortalSourceAndTarget() {
        RouteStep step = RouteStep.portal("Loop", "overworld", "overworld", 0, 70, 0);

        assertTrue(RouteLogic.validateRoute(List.of(step), 2).describe().contains("identical"));
    }

    @Test
    void validatesDimensionSpecificYBounds() {
        assertFalse(RouteLogic.validateRoute(List.of(RouteStep.move("TooHighNether", "the_nether", 0, 312, 0)), 2).valid());
        assertTrue(RouteLogic.validateRoute(List.of(RouteStep.move("HighOverworld", "overworld", 0, 312, 0)), 2).valid());
        assertFalse(RouteLogic.validateRoute(List.of(RouteStep.move("TooLowOverworld", "overworld", 0, -65, 0)), 2).valid());
    }

    @Test
    void rejectsWorldBorderAndInvalidRadius() {
        RouteStep step = RouteStep.move("Far", "overworld", 30_000_001, 70, 0);

        String description = RouteLogic.validateRoute(List.of(step), 0).describe();
        assertTrue(description.contains("world limits"));
        assertTrue(description.contains("arrival radius"));
    }

    @Test
    void warnsWhenPortalHasNoApproach() {
        var result = RouteLogic.validateRoute(
            List.of(RouteStep.portal("Portal", "overworld", "the_nether", 0, 70, 0)), 2);

        assertTrue(result.valid(), result.describe());
        assertEquals(1, result.warnings().size());
    }

    @Test
    void rejectsEmptyAndNullRoutes() {
        assertFalse(RouteLogic.validateRoute(List.of(), 2).valid());
        assertFalse(RouteLogic.validateRoute(null, 2).valid());
    }

    @Test
    void rejectsNullStepWithoutCrashing() {
        List<RouteStep> steps = new ArrayList<>();
        steps.add(null);

        var result = RouteLogic.validateRoute(steps, 2);

        assertFalse(result.valid());
        assertTrue(result.describe().contains("is null"));
    }
}

