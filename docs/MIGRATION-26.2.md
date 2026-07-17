# Wayfindr — Migration to Minecraft 26.2

This document summarizes the work to port Wayfindr from **Minecraft 1.21.4** to
**Minecraft 26.2**. It is organized top-down: a high-level overview of what changed
and why, followed by per-system detail you can dive into as needed.

> TL;DR — 26.2 is not an incremental bump. Microsoft/Mojang **removed obfuscation**
> (no more Yarn mappings), the game now requires **Java 25**, and the client rendering
> + GUI + input stacks were rewritten ("Blaze3D rewrite"). Roughly every Minecraft API
> reference in the mod changed name, and the world/HUD renderers were rewritten against
> a new retained-mode "submit node" system.

---

## 1. High-level overview

| Area | Before (1.21.4) | After (26.2) | Impact |
|------|-----------------|--------------|--------|
| Java | 21 | **25** | Toolchain + local JDK |
| Mappings | Yarn (`net.minecraft.client.MinecraftClient`, …) | **Deobfuscated jars** (real Mojang names, no mappings artifact) | Every MC symbol renamed |
| Build (Loom) | `fabric-loom` + `mappings loom.officialMojangMappings()` | `net.fabricmc.fabric-loom`, **no `mappings` line**, `com.mojang:minecraft`, `implementation` (not `modImplementation`) | build.gradle rewrite |
| World rendering | Immediate mode (`Tessellator` / `BufferBuilder` / `RenderLayer.draw`) | **Submit-node system** (`LevelRenderEvents` + `SubmitNodeCollector`) | Renderer rewrite |
| GUI rendering | `GuiGraphics` drawn in `Screen.render()` | **Retained mode** (`GuiGraphicsExtractor` in `extractRenderState()`) | Screens rewrite |
| Input | `keyPressed(int,int,int)`, `mouseClicked(double,double,int)` | `keyPressed(KeyEvent)`, `mouseClicked(MouseButtonEvent, boolean)` | Screen overrides |
| HUD registration | `HudLayerRegistrationCallback` + `IdentifiedLayer` | `HudElementRegistry` + `HudElement` | HUD wiring |
| Networking | `CustomPayload` / `PacketCodec` / `PacketByteBuf` | `CustomPacketPayload` / `StreamCodec` / `FriendlyByteBuf` | Networking rewrite |
| Keybinds | `KeyBindingHelper` + `KeyBinding` + String category | `KeyMappingHelper` + `KeyMapping` + typed `KeyMapping.Category` | Keybind rewrite |
| Permissions | `player.hasPermissionLevel(2)` | `player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)` | Permission checks |

Everything else (waypoint model, persistence/JSON, config, command tree, save handlers)
was a mechanical rename of Minecraft types with the same logic.

---

## 2. The two structural surprises

### 2.1 Deobfuscation killed Yarn mappings

Historically Fabric mods compiled against **Yarn** mappings and Loom remapped the jar
back to obfuscated names at build time. Starting with 26.1, Mojang ships the game jars
**already deobfuscated** (real class/method names, no ProGuard obfuscation map in the
version manifest). Consequences:

* `loom.officialMojangMappings()` **fails** ("Failed to find official mojang mappings")
  because there is no mapping file to fetch — you must **remove the `mappings` line entirely**.
* The Loom plugin id changed to `net.fabricmc.fabric-loom` (the `-remap` variant is only
  for the still-obfuscated 1.21.x line).
* The Minecraft dependency coordinate changed from `net.minecraft:minecraft` to
  `com.mojang:minecraft`.
* Mod dependencies no longer need remapping, so `modImplementation` → `implementation`.

The 26.2 names are effectively Mojang's own source names, which are **not identical to
either Yarn or the old "mojmap"** you might remember. Examples encountered:

| Yarn (1.21.4) | 26.2 |
|---------------|------|
| `MinecraftClient` | `net.minecraft.client.Minecraft` |
| `Text` | `net.minecraft.network.chat.Component` |
| `Vec3d` | `net.minecraft.world.phys.Vec3` |
| `Identifier` | `net.minecraft.resources.Identifier` (kept the name, new package) |
| `MatrixStack` | `com.mojang.blaze3d.vertex.PoseStack` |
| `PlayerEntity` | `net.minecraft.world.entity.player.Player` |
| `ServerPlayerEntity` | `net.minecraft.server.level.ServerPlayer` |
| `RaycastContext` | `net.minecraft.world.level.ClipContext` |
| `world.raycast(...)` | `level().clip(...)` |
| `player.eyePos` / `rotationVector` | `getEyePosition()` / `getViewVector(1f)` |
| `WorldSavePath.ROOT` | `LevelResource.ROOT` (`getWorldPath(...)`) |

Every source file was retranslated symbol-by-symbol, verified against the actual
deobfuscated jar with `javap`.

### 2.2 Java 25

Minecraft 26.2's version manifest declares `java-runtime` **major version 25**. The mod
now targets Java 25 everywhere: `release = 25`, Kotlin `jvmTarget = JVM_25`, mixin
`compatibilityLevel = JAVA_25`, and `fabric.mod.json` `"java": ">=25"`. Gradle itself
must run on JDK 25, which required Gradle **9.6.1** (wrapper bumped from 8.14) and
Kotlin **2.4.10** (from 2.1.10).

---

## 3. Rendering (the largest rewrite)

### 3.1 World-space waypoint beams — `WayfindrRenderer.kt`

The old renderer used immediate-mode drawing that no longer exists in 26.2:
`Tessellator.getInstance().begin(...)`, manual `BufferBuilder` vertices, and
`RenderLayer.getDebugQuads().draw(buffer)`. All of that was removed in the Blaze3D
rewrite.

26.2 uses a **retained-mode submit-node system**. Custom geometry is *submitted* during
a dedicated collection phase and batched/drawn by the engine later:

```kotlin
// Registered in WayfindrModClient
LevelRenderEvents.COLLECT_SUBMITS.register { context ->
    val poseStack   = context.poseStack()
    val collector   = context.submitNodeCollector()          // SubmitNodeCollector
    val cameraState = context.levelState().cameraRenderState  // for billboards
    val cameraPos   = context.gameRenderer().mainCamera().position()
    // ... per waypoint ...
}

// Inside WayfindrRenderer: emit the beam quads
collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads()) { pose, vertexConsumer ->
    vertexConsumer.addVertex(pose, x, y, z).setColor(r, g, b, a)
    // ... six faces of the beam box ...
}
```

Key API mappings:

* `Tessellator` / `BufferBuilder` → `SubmitNodeCollector.submitCustomGeometry(PoseStack, RenderType, CustomGeometryRenderer)`
* `RenderLayer.getDebugQuads()` → `RenderTypes.debugQuads()` (factories moved from
  `RenderType` to `RenderTypes`)
* `bufferBuilder.vertex(matrix, x, y, z).color(...)` → `vertexConsumer.addVertex(pose, x, y, z).setColor(r, g, b, a)`
  (colors are `0–255` ints now)
* `matrices.push()/pop()` → `poseStack.pushPose()/popPose()`

### 3.2 Floating waypoin­t name label

The old label used `Font.draw(...)` with an explicit `VertexConsumerProvider`
(`bufferBuilders.entityVertexConsumers`) and manual billboard math. That path is gone.

26.2 provides a ready-made, camera-facing name-tag primitive:

```kotlin
collector.submitNameTag(
    poseStack,                      // translated to waypoint + 2 blocks up
    Vec3.ZERO,                      // extra offset
    0x40000000,                     // translucent backdrop color
    Component.literal(waypointName),
    true,                           // see-through blocks
    0xF000F0,                       // full-brightness packed light
    cameraRenderState               // engine billboards toward this
)
```

This removed the manual yaw/pitch billboard trigonometry entirely — the engine orients
the text using `CameraRenderState`.

### 3.3 HUD navigation arrow — `WayfindrNavigationRenderer.kt` + `WayfindrModClient.kt`

Two changes:

* **Registration**: `HudLayerRegistrationCallback` + `IdentifiedLayer` were replaced by
  `HudElementRegistry.addLast(Identifier, HudElement)`, where `HudElement` is
  `extractRenderState(GuiGraphicsExtractor, DeltaTracker)`.
* **Drawing**: `DrawContext` → `GuiGraphicsExtractor`. The arrow's software triangle
  rasterizer still works because `GuiGraphicsExtractor.fill(...)` exists; 2D rotation now
  uses `context.pose()` which returns a **`Matrix3x2fStack`** (`pushMatrix()/popMatrix()/
  translate/rotate` in radians) instead of a `PoseStack`. Text uses
  `context.text(font, str, x, y, color, shadow)`.

---

## 4. Screens / GUI — retained mode

`WayfindrGui.kt`, `WayfindrConfigScreen.kt`, `WayfindrRenameScreen.kt`.

The GUI moved from *immediate* drawing to *retained* render-state extraction. The hook
you override changed:

```kotlin
// before
override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) { ... }
// after
override fun extractRenderState(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, delta: Float) { ... }
```

Widget + draw API renames:

| Before | After |
|--------|-------|
| `ButtonWidget` / `.builder` / `.dimensions()` | `Button` / `.builder` / `.bounds()` |
| `TextFieldWidget` / `.text` / `.setChangedListener` | `EditBox` / `.value` / `.setResponder` |
| `SliderWidget` | `AbstractSliderButton` |
| `addDrawableChild` / `remove` | `addRenderableWidget` / `removeWidget` |
| `textRenderer` | `font` |
| `context.drawCenteredTextWithShadow` | `context.centeredText` |
| `context.drawTextWithShadow` | `context.text(..., shadow=true)` |
| `close()` | `onClose()` |
| `shouldPause()` | `isPauseScreen()` |
| `client.setScreen(...)` | `minecraft.setScreenAndShow(...)` |

### Input events

The biggest screen gotcha: input handlers now take **event records** instead of loose
primitives.

| Before | After |
|--------|-------|
| `keyPressed(keyCode, scanCode, modifiers)` | `keyPressed(event: KeyEvent)` → `event.key()`, `event.scancode()`, `event.modifiers()` |
| `mouseClicked(x, y, button)` | `mouseClicked(event: MouseButtonEvent, doubleClick: Boolean)` → `event.x()`, `event.y()`, `event.button()` |
| `mouseReleased(x, y, button)` | `mouseReleased(event: MouseButtonEvent)` |
| `mouseDragged(x, y, button, dx, dy)` | `mouseDragged(event: MouseButtonEvent, dragX, dragY)` |
| `mouseScrolled(x, y, h, v)` | unchanged |

---

## 5. Networking — `WayfindrNetworking.kt`

Payload API renamed and restructured:

| Before | After |
|--------|-------|
| `CustomPayload` | `CustomPacketPayload` |
| `CustomPayload.Id<T>` + `getId()` | `CustomPacketPayload.Type<T>` + `type()` |
| `PacketCodec<PacketByteBuf, T>` | `StreamCodec<RegistryFriendlyByteBuf, T>` |
| `PacketCodec` anonymous class | `StreamCodec.of(encoder, decoder)` (SAM lambdas) |
| `buf.writeString` / `readString` | `buf.writeUtf` / `readUtf` |
| `Identifier.of(ns, path)` | `Identifier.fromNamespaceAndPath(ns, path)` |
| `PayloadTypeRegistry.playS2C()` / `playC2S()` | `.clientboundPlay()` / `.serverboundPlay()` |

Example:

```kotlin
data class WaypointSyncPayload(val data: String) : CustomPacketPayload {
    override fun type() = ID
    companion object {
        val ID = CustomPacketPayload.Type<WaypointSyncPayload>(WAYPOINT_SYNC_ID)
        val CODEC: StreamCodec<RegistryFriendlyByteBuf, WaypointSyncPayload> = StreamCodec.of(
            { buf, payload -> buf.writeUtf(payload.data) },
            { buf -> WaypointSyncPayload(buf.readUtf()) }
        )
    }
}
```

`WayfindrNetworkClient.kt` / `WayfindrNetworkServer.kt` needed only minor changes — the
Fabric `ClientPlayNetworking` / `ServerPlayNetworking` receiver API is stable, and the
payload `.ID` (now a `Type`) is still what you register receivers against. Server-side,
`ServerPlayerEntity` → `ServerPlayer`, `server.playerManager.playerList` →
`server.playerList.players`, and the permission check was updated (see §6).

---

## 6. Keybinds & permissions

**Keybinds** (`WayfindrKeybinds.kt`): the module was renamed from
`fabric-key-binding-api-v1` to `fabric-key-mapping-api-v1`:

| Before | After |
|--------|-------|
| `KeyBindingHelper.registerKeyBinding` | `KeyMappingHelper.registerKeyMapping` |
| `KeyBinding` | `KeyMapping` |
| `InputUtil.Type.KEYSYM` | `InputConstants.Type.KEYSYM` |
| String category (`"category.wayfindr.general"`) | typed `KeyMapping.Category.register(Identifier)` |
| `keyBinding.wasPressed()` | `keyMapping.consumeClick()` |

**Permissions** (`WayfindrNetworkServer.kt`): op-level checks became named permissions:

```kotlin
// before: player.hasPermissionLevel(2)
player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
```

---

## 7. Miscellaneous renames

* `Text.literal` → `Component.literal`; `text.string` → `component.getString()`
* `player.world` → `player.level()`; `world.registryKey.value` → `level().dimension().identifier()`
* `player.pos` → `player.position()`; `player.yaw` → `player.getYRot()`
* `player.sendMessage(text, false)` → `player.sendSystemMessage(component)`;
  actionbar (`true`) → `player.sendOverlayMessage(component)`
* `player.abilities.creativeMode` → `player.abilities.instabuild`
* `client.networkHandler` → `client.connection`; `sendChatCommand` → `sendCommand`
* `CommandManager` → `Commands`; `ServerCommandSource` → `CommandSourceStack`;
  `source.sendFeedback` → `source.sendSuccess`
* The no-op `ExampleMixin` was **removed** (its `loadWorld` target was renamed and it did
  nothing).

---

## 8. Files changed

Build / metadata:
* `build.gradle`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`
* `src/main/resources/fabric.mod.json`, `src/main/resources/wayfindr.mixins.json`

Rewritten (structural):
* `WayfindrRenderer.kt` (new, in `src/main`), `WayfindrNavigationRenderer.kt`,
  `WayfindrModClient.kt`, `WayfindrGui.kt`, `WayfindrConfigScreen.kt`,
  `WayfindrRenameScreen.kt`, `WayfindrNetworking.kt`, `WayfindrKeybinds.kt`

Mechanical rename:
* `WayfindrCommands.kt`, `WayfindrRaycast.kt`, `WayfindrWaypointManager.kt`,
  `WayfindrNetworkServer.kt`, `WayfindrSaveFileHandler.kt`, `ServerWaypointSaveHandler.kt`

Unchanged (no Minecraft API surface):
* `Wayfindr.kt`, `WayfindrConfig.kt`, `UUIDSerializer.kt`, `WayfindrDataGenerator.kt`,
  `ServerWaypointManager.kt`, `WayfindrNetworkClient.kt`

Removed:
* `src/main/java/net/dfnkt/wayfindr/mixin/ExampleMixin.java`

---

## 9. Verification & known caveats

**Verified:** clean compile (`./gradlew build`) and a full in-world run
(`./gradlew runClient`) on JDK 25 — the mod loads, registers keybinds/networking/HUD/
world-render events, joins a world, and exits cleanly with no exceptions.

**Not yet eyeball-verified:** the actual on-screen appearance of the beam quads, the
floating name label, and the HUD arrow. The code paths run without error, but colors /
positioning / billboard behavior should be sanity-checked in-game. Two spots to watch:

* `submitNameTag`'s `Vec3` parameter is treated here as an *offset* (passed `Vec3.ZERO`
  with the pose translated to the waypoint). If labels appear mispositioned, that offset
  is the first thing to adjust.
* The nav arrow uses a per-pixel software rasterizer (many `fill` calls); functional but
  not the most efficient. Could later be replaced with a textured sprite via
  `GuiGraphicsExtractor.blitSprite`.

**Follow-ups not done here:**
* The `main` branch still targets 1.21.4/1.21.5 (Yarn). Because the toolchains are
  incompatible, 26.2 lives on its own `mc26.2` branch rather than a shared multi-version
  build.
