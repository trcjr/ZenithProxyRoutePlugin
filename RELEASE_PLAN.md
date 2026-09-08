# Release readiness plan

This is the source of truth for promoting ZenithProxyRoutePlugin from prerelease to stable. Keep `v0.x` releases marked as prerelease until every `1.0.0` gate is complete.

## Current status

- Current public version: `v0.3.0` prerelease
- Next milestone: `v0.4.0`
- Supported build target: ZenithProxy `1.21.4`, Java release channel
- Automated baseline: 15 passing tests before the `0.3.0` work began
- Proven live behavior: ordinary movement, Overworld → Nether transition, and post-transition continuation

## 0.3.0 — validation and observable state

- [x] Create and publish this release-readiness plan
- [x] Validate the whole route before movement starts
- [x] Reject discontinuous source/target dimensions
- [x] Reject duplicate or blank step names
- [x] Reject unknown step types and dimensions
- [x] Reject coordinates outside dimension/world limits
- [x] Add `route validate`
- [x] Add explicit `STOPPED`, `READY`, `PATHING`, `WAITING_FOR_PORTAL`, `PAUSED`, `FAILED`, and `COMPLETE` states
- [x] Preserve and display the latest failure reason
- [ ] Add automated regression tests for every validation rule and state transition
- [ ] Run the live movement and portal smoke tests
- [x] Publish `v0.3.0` prerelease after automated checks pass; remaining live checks stay tracked below

## 0.4.0 — persistence and restart recovery

- [ ] Confirm how ZenithProxy plugin configuration is flushed to disk
- [ ] Persist progress immediately after every meaningful transition
- [ ] Recover while pathing a normal step
- [ ] Recover before entering a portal
- [ ] Recover while waiting inside a portal
- [ ] Recover immediately after a dimension transition
- [ ] Prevent both skipped and duplicated steps after an unclean shutdown
- [ ] Add simulated restart tests
- [ ] Complete the live restart test matrix

## 0.5.0 — reusable route definitions

- [ ] Separate saved route definitions from active execution state
- [ ] Support multiple named routes
- [ ] Add create, show, delete, copy, and start-by-name commands
- [ ] Prevent edits to a route used by an active execution
- [ ] Define configuration migration and backup behavior
- [ ] Add configuration round-trip and migration tests

## 0.6.0 — path ownership and integration hardening

- [ ] Detect when another module or user replaces the active goal
- [ ] Pause with `GOAL_REPLACED` rather than hanging or advancing
- [ ] Define behavior when a controlling player connects
- [ ] Define disconnect and reconnect behavior
- [ ] Exercise high-latency and long-distance routes
- [ ] Add a test adapter around ZenithProxy pathing and world state
- [ ] Simulate a complete Overworld → Nether → Overworld route

## Documentation and distribution

- [x] Rewrite README as user documentation rather than a test handoff
- [x] Keep experimental procedures in `TESTING.md`
- [ ] Add a compatibility table
- [ ] Document configuration location, backups, and upgrades
- [ ] Document every command with an example
- [ ] Document all known limitations
- [x] Build and test workflow runs on pushes and pull requests
- [x] Tagged release workflow builds, tests, and attaches the jar
- [x] Release includes SHA-256 checksums
- [ ] Add contribution and issue-reporting guidance

## Stable `1.0.0` gate

- [ ] All automated tests pass on a clean GitHub runner
- [ ] Both portal directions pass repeatedly on the intended server
- [ ] Full spawn-to-base route passes repeatedly with no manual intervention
- [ ] Failure cases pause with actionable messages
- [ ] Restart cases neither skip nor duplicate route steps
- [ ] Route configuration survives upgrade from the previous prerelease
- [ ] README matches the released command surface
- [ ] Supported ZenithProxy and Minecraft versions are explicit
- [ ] No unresolved critical or high-severity bugs
- [ ] The exact release jar checksum is recorded

## Working rule

Do not solve unrelated bot safety concerns here. Health monitoring, player detection, multi-account coordination, combat, and inventory policy remain outside this plugin unless route execution itself requires a narrowly scoped behavior.
