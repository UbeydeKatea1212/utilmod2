# Util Mod (Fabric 1.21.1, Yarn, client-only)

A learning project showing three client-side building blocks.

| Feature | Package | Key idea |
|---|---|---|
| Block scanner | `scan` | Time-sliced on the client thread: nearest chunk first, palette pre-check, fixed per-tick budget |
| ESP / HUD | `render` | One batched line draw per frame in `WorldRenderEvents`, double-precision camera-relative coords |
| Freecam | `freecam` + `mixin` | Override `Camera#update` output, freeze player input, redirect mouse look, lock interactions |

## Layout

```
src/main/java/com/example/utilmod/
  UtilModClient.java          entrypoint, only wiring
  config/UtilModConfig.java   runtime settings
  scan/                       ScanTarget, ScanTargets, Match, BlockScanner
  render/                     LineBatch, EspRenderer, HudOverlay
  freecam/FreecamController   camera state + movement
  mixin/                      CameraMixin, EntityMixin, KeyboardInputMixin, MinecraftClientMixin
```

## Build and run

This archive has no Gradle wrapper jar. Either copy `gradlew`, `gradlew.bat` and `gradle/` from a fresh project
generated at https://fabricmc.net/develop/template, or run `gradle wrapper --gradle-version 8.10` with a local Gradle.
Then:

```
./gradlew runClient
```

Versions in `gradle.properties` are conservative defaults; bump them to the newest 1.21.1-compatible
Loom / Loader / Fabric API from https://fabricmc.net/develop.

Keys (rebindable in Controls > Util Mod): **F6** freecam, **F7** ESP on/off.

## Design notes

**Scanner.** Chunk and world data belong to the client thread, so a background thread would race with packet
handling. Instead the scanner is a cooperative job: each tick it processes chunks until `scanBudgetNanos` is used
(always at least one). `ChunkSection#hasAny(predicate)` tests the section palette, so empty-of-target sections
cost almost nothing. For block-entity blocks (spawners, chests) an even cheaper route is iterating
`chunk.getBlockEntities()` instead of scanning sections.

**ESP.** `LineBatch` converts world coords to camera-relative in `double` and draws everything in a single call.
Render state (blend, depth test, line width) is always restored afterwards.

**Freecam.** The player entity is never moved. The camera is overridden after vanilla computes it, so everything
that reads the camera (frustum, fog, the ESP renderer) follows automatically. Chunks still only load around the
real player, so the freecam can't see beyond the loaded area.

## Fair-play note

Freecam and block ESP are fine for singleplayer and for learning rendering. Many multiplayer servers prohibit
them, so check the rules of any server before using these features there.
