package com.trcjr.zenithroute;

import java.util.ArrayList;
import java.util.List;

public class RouteConfig {
    public final List<RouteStep> steps = new ArrayList<>();
    public boolean enabled = false;
    public boolean paused = false;
    public int currentIndex = 0;
    /** Ordinary move steps complete within this 3D block radius. Portal steps remain exact. */
    public int moveArrivalRadius = 2;
    /** True only after the current portal step has actually issued its Baritone goal. */
    public boolean portalTransitionArmed = false;

    public static final class RouteStep {
        public String name = "";
        public String type = "move";
        public String dimension = "overworld";
        public String targetDimension = "";
        public int x;
        public int y;
        public int z;

        public RouteStep() {}

        public static RouteStep move(String name, String dimension, int x, int y, int z) {
            return new RouteStep(name, "move", dimension, "", x, y, z);
        }

        public static RouteStep portal(String name, String dimension, String targetDimension, int x, int y, int z) {
            return new RouteStep(name, "portal", dimension, targetDimension, x, y, z);
        }

        private RouteStep(String name, String type, String dimension, String targetDimension, int x, int y, int z) {
            this.name = name;
            this.type = type;
            this.dimension = dimension;
            this.targetDimension = targetDimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public boolean isPortal() {
            return "portal".equals(type);
        }

        @Override
        public String toString() {
            String value = name + " (" + type + ", " + dimension + ") [" + x + ", " + y + ", " + z + "]";
            return isPortal() ? value + " -> " + targetDimension : value;
        }
    }
}
