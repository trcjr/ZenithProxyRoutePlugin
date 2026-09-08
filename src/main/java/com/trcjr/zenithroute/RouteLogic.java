package com.trcjr.zenithroute;

import com.trcjr.zenithroute.RouteConfig.RouteStep;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalBlock;
import com.zenith.feature.pathfinder.goals.GoalNear;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure route decisions kept separate so they can be tested without a live proxy. */
public final class RouteLogic {
    private RouteLogic() {}

    public enum PortalState {
        NOT_A_PORTAL,
        READY_TO_PATH,
        WAITING_FOR_TRANSITION,
        TRANSITION_COMPLETE,
        WRONG_DIMENSION
    }

    public enum ExecutionState {
        STOPPED,
        READY,
        PATHING,
        WAITING_FOR_PORTAL,
        PAUSED,
        FAILED,
        COMPLETE
    }

    public record ValidationResult(List<String> errors, List<String> warnings) {
        public boolean valid() { return errors.isEmpty(); }

        public String describe() {
            StringBuilder result = new StringBuilder(valid() ? "Route is valid" : "Route is invalid");
            for (String error : errors) result.append("\nERROR: ").append(error);
            for (String warning : warnings) result.append("\nWARNING: ").append(warning);
            return result.toString();
        }
    }

    public static PortalState portalState(RouteStep step, boolean transitionArmed, String currentDimension) {
        if (!step.isPortal()) return PortalState.NOT_A_PORTAL;
        if (currentDimension.equals(step.dimension)) {
            return transitionArmed ? PortalState.WAITING_FOR_TRANSITION : PortalState.READY_TO_PATH;
        }
        if (transitionArmed && currentDimension.equals(step.targetDimension)) {
            return PortalState.TRANSITION_COMPLETE;
        }
        return PortalState.WRONG_DIMENSION;
    }

    public static String normalizeDimension(String dimension) {
        if (dimension == null) throw new IllegalArgumentException("Dimension cannot be null");
        return switch (dimension.toLowerCase()) {
            case "overworld", "ow" -> "overworld";
            case "nether", "the_nether" -> "the_nether";
            case "end", "the_end" -> "the_end";
            default -> throw new IllegalArgumentException("Unknown dimension: " + dimension);
        };
    }

    public static Goal goalFor(RouteStep step, int moveArrivalRadius) {
        if (moveArrivalRadius < 1 || moveArrivalRadius > 8) {
            throw new IllegalArgumentException("Move arrival radius must be between 1 and 8 blocks");
        }
        return step.isPortal()
            ? new GoalBlock(step.x, step.y, step.z)
            : new GoalNear(step.x, step.y, step.z, moveArrivalRadius * moveArrivalRadius);
    }

    public static ValidationResult validateRoute(List<RouteStep> steps, int moveArrivalRadius) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (steps == null || steps.isEmpty()) {
            errors.add("route has no steps");
            return new ValidationResult(List.copyOf(errors), List.copyOf(warnings));
        }
        if (moveArrivalRadius < 1 || moveArrivalRadius > 8) {
            errors.add("move arrival radius must be between 1 and 8 blocks");
        }

        Set<String> names = new HashSet<>();
        String expectedDimension = null;
        for (int i = 0; i < steps.size(); i++) {
            RouteStep step = steps.get(i);
            String label = "step " + (i + 1);
            if (step == null) {
                errors.add(label + " is null");
                continue;
            }
            if (step.name == null || step.name.isBlank()) {
                errors.add(label + " has a blank name");
            } else if (!names.add(step.name.toLowerCase(Locale.ROOT))) {
                errors.add(label + " has duplicate name '" + step.name + "'");
            }
            if (!"move".equals(step.type) && !"portal".equals(step.type)) {
                errors.add(label + " has unknown type '" + step.type + "'");
                continue;
            }

            String dimension = validatedDimension(step.dimension, label + " source", errors);
            if (dimension != null && expectedDimension != null && !dimension.equals(expectedDimension)) {
                errors.add(label + " starts in " + dimension + " but the previous step leaves the bot in " + expectedDimension);
            }
            if (dimension != null) validateCoordinates(step, dimension, label, errors);

            if (step.isPortal()) {
                String target = validatedDimension(step.targetDimension, label + " target", errors);
                if (dimension != null && dimension.equals(target)) {
                    errors.add(label + " portal source and target dimensions are identical");
                }
                expectedDimension = target;
                if (i == 0 || steps.get(i - 1) == null || steps.get(i - 1).isPortal()) {
                    warnings.add(label + " portal has no immediately preceding movement approach step");
                }
            } else {
                if (step.targetDimension != null && !step.targetDimension.isBlank()) {
                    errors.add(label + " movement step must not have a target dimension");
                }
                expectedDimension = dimension;
            }
        }
        return new ValidationResult(List.copyOf(errors), List.copyOf(warnings));
    }

    private static String validatedDimension(String value, String label, List<String> errors) {
        try {
            return normalizeDimension(value);
        } catch (IllegalArgumentException e) {
            errors.add(label + " dimension is invalid: " + value);
            return null;
        }
    }

    private static void validateCoordinates(RouteStep step, String dimension, String label, List<String> errors) {
        if (step.x < -30_000_000 || step.x > 30_000_000 || step.z < -30_000_000 || step.z > 30_000_000) {
            errors.add(label + " X/Z coordinates are outside Minecraft world limits");
        }
        int minY = "overworld".equals(dimension) ? -64 : 0;
        int maxY = "overworld".equals(dimension) ? 319 : 255;
        if (step.y < minY || step.y > maxY) {
            errors.add(label + " Y=" + step.y + " is outside " + dimension + " bounds " + minY + ".." + maxY);
        }
    }
}
