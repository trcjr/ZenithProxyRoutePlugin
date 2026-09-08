package com.trcjr.zenithroute;

import com.trcjr.zenithroute.RouteConfig.RouteStep;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.pathfinder.goals.GoalBlock;
import com.zenith.feature.pathfinder.goals.GoalNear;

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
}
