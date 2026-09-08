# Changelog

## 0.3.0 — 2026-09-07

- Validates complete routes before starting.
- Rejects broken dimension continuity, duplicate names, invalid types, invalid dimensions, and out-of-range coordinates.
- Adds `route validate`.
- Adds explicit execution state and failure-reason fields to status output.
- Expands deterministic validation regression coverage.

## 0.2.3 — 2026-09-07

Initial public prerelease.

- Executes ordered movement steps for one ZenithProxy bot.
- Supports explicit Overworld, Nether, and End route dimensions.
- Supports explicit portal-transition steps.
- Persists route configuration and progress.
- Supports list, start, status, pause, resume, stop, and clear operations.
- Uses a configurable arrival radius for ordinary movement.
- Requires exact positioning for portal steps.
- Pauses on path failure, transition timeout, or unexpected dimensions.
- Includes regression coverage for false and reversed portal transitions.
