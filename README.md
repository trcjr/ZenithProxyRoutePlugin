# ZenithProxy Ordered Routes — rough POC

[![Build and test](https://github.com/trcjr/ZenithProxyRoutePlugin/actions/workflows/build.yml/badge.svg)](https://github.com/trcjr/ZenithProxyRoutePlugin/actions/workflows/build.yml)
[![Latest release](https://img.shields.io/github/v/release/trcjr/ZenithProxyRoutePlugin?include_prereleases)](https://github.com/trcjr/ZenithProxyRoutePlugin/releases)

A small proof of concept for navigating one unattended ZenithProxy bot through ordered 3D movement and portal-transition steps. It is based on [ZenithProxyExamplePlugin](https://github.com/rfresh2/ZenithProxyExamplePlugin) and currently targets the ZenithProxy `1.21.4` API.

## Scope

This version stores one route per proxy instance, advances after ZenithProxy reports successful path completion, persists the route/current index, supports explicit portal transitions, and supports pause/resume. It deliberately does not retry failures, manage several bots, monitor health/players, or claim to be safe for valuable inventories.

A portal step paths the bot into the specified portal block, waits up to 30 seconds for the expected dimension, and then advances. The dimension transition itself is authoritative; Baritone does not need to report exact-block path completion after the portal goal has been issued. It pauses on a wrong dimension or transition timeout.

The first dimension after the portal name is the dimension the bot is currently in; the second is the destination. For example, entering a Nether portal from the Overworld is `overworld nether`, not `nether overworld`.

## Build and install

### Install a release

1. Download the jar from [GitHub Releases](https://github.com/trcjr/ZenithProxyRoutePlugin/releases).
2. Place it in the `plugins` folder beside a **Java-channel** ZenithProxy launcher.
3. Restart ZenithProxy; plugins cannot be hot-reloaded.

### Build from source

```text
./gradlew build
```

The jar is written to `build/libs/`.

Tagged releases run the same clean test/build command in GitHub Actions, publish the jar, and attach a SHA-256 checksum file.

## Tests

```text
./gradlew clean test build
```

The automated suite currently covers:

- The original false portal-transition regression.
- Reversed source/target portal dimensions.
- Armed versus unarmed portal states.
- Correct source, target, and unrelated-dimension behavior.
- Movement and portal step construction.
- Dimension aliases and invalid dimensions.
- Safe initial configuration state.

These are deterministic unit tests. A live integration checklist is still required before publishing because an ordinary test process cannot simulate the server's portal timing and packets.

## Commands

```text
route move <name> <dimension> <x> <y> <z>
route portal <name> <fromDimension> <targetDimension> <x> <y> <z>
route radius <blocks>
route list
route start
route status
route pause
route resume
route stop
route clear
```

Alias: `rt`.

Ordinary movement steps use a configurable 3D arrival radius, defaulting to 2 blocks. Portal steps still require the exact portal block. Set the movement tolerance with `route radius 1` through `route radius 8`.

## Safe first test

Use an empty inventory and two nearby reachable positions:

```text
route clear
route move TestA overworld 10 64 10
route move TestB overworld 20 64 10
route list
route start
route status
```

After that succeeds, the Rally-01 route is:

```text
route clear
route move Rally01Exit nether 0 120 -8220
route move Rally01Turn nether 467 120 -8220
route move Rally01PortalApproach nether 467 120 -8315
route portal Rally01Portal nether overworld 467 120 -8320
route list
route start
```

Verify the actual portal block and its Y-level before using those example coordinates. Valid dimension names are `overworld`, `nether`/`the_nether`, and `end`/`the_end`.

## Spawn-to-base route shape

The intended full route can mix dimensions explicitly:

```text
route clear
route move SpawnEscape overworld X Y Z
route portal SpawnNetherPortal overworld nether X Y Z
route move HighwayJoin nether X Y Z
route move HighwayExit nether X Y Z
route move BasePortalApproach nether X Y Z
route portal BasePortal nether overworld X Y Z
route move BaseArrival overworld X Y Z
route list
route start
```

Use exact portal-block coordinates for `route portal`, with a separate nearby approach step when practical.
