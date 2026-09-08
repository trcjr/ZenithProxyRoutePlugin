# Pre-publication test plan

Run every live test with a disposable account and empty inventory. Record the ZenithProxy version, Minecraft protocol version, server, route commands, and relevant logs.

## Automated gate

```text
./gradlew clean test build
```

Required result: all tests pass and the plugin jar is produced.

## Live movement tests

- [ ] Two reachable movement steps advance in order.
- [ ] An unreachable movement step pauses instead of skipping.
- [ ] `route pause` stops the active plugin-owned goal.
- [ ] `route resume` continues the same step.
- [ ] `route stop` returns the route to step 1 without deleting it.
- [ ] Restarting ZenithProxy preserves the configured steps and current index.

## Live portal tests

- [ ] Overworld → Nether transition advances exactly once.
- [ ] Nether → Overworld transition advances exactly once.
- [ ] A reversed portal declaration is rejected/paused before pathing.
- [ ] Standing in the target dimension before a portal step does not count as success.
- [ ] A valid approach step followed by a portal step keeps the bot in the portal long enough to transition.
- [ ] A broken or obstructed portal pauses after the timeout.
- [ ] Arrival in an unexpected dimension pauses the route.
- [ ] Restart while waiting inside a portal neither skips nor duplicates the step.
- [ ] Restart immediately after transition recognizes an armed, completed transition and advances once.

## Route integrity tests

- [ ] `route list` shows correct order, types, dimensions, and coordinates.
- [ ] An empty route cannot start.
- [ ] Active routes cannot be edited.
- [ ] Invalid dimension names are rejected.
- [ ] Portal source and target dimensions cannot be identical.
- [ ] Completing the last step disables the route cleanly.

## Publication gate

- [ ] Automated build is green.
- [ ] Both portal directions pass on the intended server.
- [ ] Restart cases pass.
- [ ] README commands match the built jar.
- [ ] Known limitations and supported ZenithProxy/Minecraft versions are documented.
- [ ] Version is tagged only after the tested jar checksum is recorded.

