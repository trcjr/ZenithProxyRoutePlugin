package com.trcjr.zenithroute.module;

import com.github.rfresh2.EventConsumer;
import com.trcjr.zenithroute.RouteConfig.RouteStep;
import com.zenith.event.client.ClientBotTick;
import com.zenith.feature.pathfinder.goals.Goal;
import com.zenith.feature.player.World;
import com.zenith.module.api.Module;

import java.util.List;

import static com.github.rfresh2.EventConsumer.of;
import static com.trcjr.zenithroute.OrderedRoutesPlugin.CONFIG;
import static com.trcjr.zenithroute.RouteLogic.PortalState.TRANSITION_COMPLETE;
import static com.trcjr.zenithroute.RouteLogic.normalizeDimension;
import static com.trcjr.zenithroute.RouteLogic.portalState;
import static com.trcjr.zenithroute.RouteLogic.goalFor;
import static com.zenith.Globals.BARITONE;

public class OrderedRouteModule extends Module {
    private static final int FAILURE_GRACE_TICKS = 40;
    private static final int PORTAL_TIMEOUT_TICKS = 20 * 30;

    private boolean goalIssued;
    private boolean arrivalPending;
    private boolean waitingForPortal;
    private int inactiveTicks;
    private int portalWaitTicks;
    private Goal activeGoal;

    @Override
    public boolean enabledSetting() { return CONFIG.enabled; }

    @Override
    public List<EventConsumer<?>> registerEvents() {
        return List.of(
            of(ClientBotTick.class, this::handleBotTick),
            of(ClientBotTick.Starting.class, e -> resetRuntimeState()),
            of(ClientBotTick.Stopped.class, e -> { stopOurGoal(); resetRuntimeState(); })
        );
    }

    @Override
    public synchronized void onEnable() { resetRuntimeState(); }

    @Override
    public synchronized void onDisable() { stopOurGoal(); resetRuntimeState(); }

    public synchronized void addMove(String name, String dimension, int x, int y, int z) {
        requireEditable();
        CONFIG.steps.add(RouteStep.move(name, normalizeDimension(dimension), x, y, z));
    }

    public synchronized void addPortal(String name, String fromDimension, String targetDimension, int x, int y, int z) {
        requireEditable();
        String from = normalizeDimension(fromDimension);
        String target = normalizeDimension(targetDimension);
        if (from.equals(target)) throw new IllegalArgumentException("Portal dimensions must be different");
        CONFIG.steps.add(RouteStep.portal(name, from, target, x, y, z));
    }

    public synchronized void setMoveArrivalRadius(int blocks) {
        if (blocks < 1 || blocks > 8) throw new IllegalArgumentException("Move arrival radius must be between 1 and 8 blocks");
        CONFIG.moveArrivalRadius = blocks;
    }

    public synchronized void clearRoute() {
        stopRoute();
        CONFIG.steps.clear();
        CONFIG.portalTransitionArmed = false;
    }

    public synchronized void startRoute() {
        if (CONFIG.steps.isEmpty()) throw new IllegalStateException("The route has no steps");
        CONFIG.currentIndex = 0;
        CONFIG.portalTransitionArmed = false;
        verifyCurrentStepDimension();
        CONFIG.paused = false;
        CONFIG.enabled = true;
        resetRuntimeState();
        syncEnabledFromConfig();
    }

    public synchronized void pauseRoute(String reason) {
        if (!CONFIG.enabled) throw new IllegalStateException("No route is active");
        CONFIG.paused = true;
        stopOurGoal();
        resetRuntimeState();
        warn("Ordered route paused: {}", reason);
    }

    public synchronized void resumeRoute() {
        if (!CONFIG.enabled || !CONFIG.paused) throw new IllegalStateException("No paused route is active");
        if (reconcilePortalAfterRestart()) return;
        if (CONFIG.currentIndex < CONFIG.steps.size()
            && currentStep().isPortal()
            && currentDimension().equals(currentStep().dimension)) {
            // We are still on the source side. Reissue the portal goal rather than
            // waiting forever on an old armed state from before the pause/restart.
            CONFIG.portalTransitionArmed = false;
        }
        verifyCurrentStepDimension();
        CONFIG.paused = false;
        resetRuntimeState();
    }

    public synchronized void stopRoute() {
        stopOurGoal();
        CONFIG.enabled = false;
        CONFIG.paused = false;
        CONFIG.currentIndex = 0;
        CONFIG.portalTransitionArmed = false;
        resetRuntimeState();
        syncEnabledFromConfig();
    }

    public synchronized String describeStatus() {
        if (CONFIG.steps.isEmpty()) return "Empty route";
        if (!CONFIG.enabled) return "Stopped; " + CONFIG.steps.size() + " configured steps";
        if (CONFIG.currentIndex >= CONFIG.steps.size()) return "Complete";
        String state = CONFIG.paused ? "Paused" : waitingForPortal ? "Waiting for portal transition" : goalIssued ? "Pathing" : "Ready";
        return state + " at step " + (CONFIG.currentIndex + 1) + "/" + CONFIG.steps.size() + ": " + currentStep();
    }

    private synchronized void handleBotTick(ClientBotTick event) {
        if (!CONFIG.enabled || CONFIG.paused) return;
        if (CONFIG.currentIndex >= CONFIG.steps.size()) { completeRoute(); return; }

        if (reconcilePortalAfterRestart()) return;

        RouteStep step = currentStep();
        if (step.isPortal() && CONFIG.portalTransitionArmed) waitingForPortal = true;
        if (!currentDimension().equals(step.dimension)) {
            pauseInternal("dimension mismatch: expected=" + step.dimension + ", current=" + currentDimension());
            return;
        }

        if (waitingForPortal) {
            if (++portalWaitTicks >= PORTAL_TIMEOUT_TICKS) {
                pauseInternal("portal transition timed out after 30 seconds at " + step.name);
            }
            return;
        }

        if (arrivalPending) {
            arrivalPending = false;
            goalIssued = false;
            activeGoal = null;
            inactiveTicks = 0;
            if (step.isPortal()) {
                waitingForPortal = true;
                portalWaitTicks = 0;
                info("Arrived at portal step {}; waiting for dimension {}", step.name, step.targetDimension);
            } else {
                advanceStep();
            }
            return;
        }

        if (!goalIssued) { issueCurrentGoal(); return; }

        if (BARITONE.isGoalActive(activeGoal) || BARITONE.isActive()) {
            inactiveTicks = 0;
        } else if (++inactiveTicks >= FAILURE_GRACE_TICKS) {
            pauseInternal("step became inactive without successful completion: " + step.name);
        }
    }

    /** Handles a dimension transition that occurred while waiting or across a proxy restart. */
    private boolean reconcilePortalAfterRestart() {
        if (CONFIG.currentIndex >= CONFIG.steps.size()) return false;
        RouteStep step = currentStep();
        if (portalState(step, CONFIG.portalTransitionArmed, currentDimension()) == TRANSITION_COMPLETE) {
            info("Portal transition complete: {} -> {}", step.dimension, step.targetDimension);
            waitingForPortal = false;
            advanceStep();
            return true;
        }
        return false;
    }

    private void issueCurrentGoal() {
        RouteStep step = currentStep();
        Goal issuedGoal = goalFor(step, CONFIG.moveArrivalRadius);
        activeGoal = issuedGoal;
        goalIssued = true;
        if (step.isPortal()) CONFIG.portalTransitionArmed = true;
        inactiveTicks = 0;
        info("Pathing route step {}/{}: {}", CONFIG.currentIndex + 1, CONFIG.steps.size(), step);
        BARITONE.pathTo(issuedGoal).addExecutedListener(future -> {
            synchronized (OrderedRouteModule.this) {
                if (CONFIG.enabled && !CONFIG.paused && issuedGoal.equals(future.getGoal())) arrivalPending = true;
            }
        });
    }

    private void advanceStep() {
        CONFIG.currentIndex++;
        CONFIG.portalTransitionArmed = false;
        resetRuntimeState();
        if (CONFIG.currentIndex >= CONFIG.steps.size()) completeRoute();
        else info("Advancing route to step {}/{}", CONFIG.currentIndex + 1, CONFIG.steps.size());
    }

    private void completeRoute() {
        info("Ordered route completed all {} steps", CONFIG.steps.size());
        CONFIG.enabled = false;
        CONFIG.paused = false;
        CONFIG.portalTransitionArmed = false;
        resetRuntimeState();
        syncEnabledFromConfig();
    }

    private void pauseInternal(String reason) {
        CONFIG.paused = true;
        stopOurGoal();
        resetRuntimeState();
        warn("Ordered route paused: {}", reason);
    }

    private void verifyCurrentStepDimension() {
        reconcilePortalAfterRestart();
        if (CONFIG.currentIndex >= CONFIG.steps.size()) return;
        if (!currentDimension().equals(currentStep().dimension)) {
            throw new IllegalStateException("dimension mismatch: expected=" + currentStep().dimension + ", current=" + currentDimension());
        }
    }

    private void requireEditable() {
        if (CONFIG.enabled) throw new IllegalStateException("Stop the active route before editing it");
    }

    private RouteStep currentStep() { return CONFIG.steps.get(CONFIG.currentIndex); }
    private String currentDimension() { return World.getCurrentDimension().name(); }

    private void stopOurGoal() {
        if (activeGoal != null && BARITONE.isGoalActive(activeGoal)) BARITONE.stop();
    }

    private void resetRuntimeState() {
        goalIssued = false;
        arrivalPending = false;
        waitingForPortal = false;
        inactiveTicks = 0;
        portalWaitTicks = 0;
        activeGoal = null;
    }
}
