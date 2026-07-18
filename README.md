<h1>
<p align="center">
  <img src="https://github.com/caleywoods/wayfindr/blob/main/wayfindr_logo.png?raw=true" alt="Logo" width="350">
  <br>Wayfindr
</h1>
<p align="center">
  A Minecraft waypoints mod written in Kotlin targeting the <a href="https://fabricmc.net/">Fabric mod ecosystem</a>.
  <br>
  Create and manage waypoints throughout your Minecraft world.
</p>

## Screenshots

### Waypoints in the World
![Waypoints in the world](https://github.com/caleywoods/wayfindr/blob/main/res/Waypoints.png?raw=true)

### Waypoint navigation
![Waypoints in the world](https://github.com/caleywoods/wayfindr/blob/main/res/Navigation.png?raw=true)

### Waypoint Manager
![Waypoint manager interface](https://github.com/caleywoods/wayfindr/blob/main/res/Waypoint%20manager.png?raw=true)

### Configuration Screen
![Configuration options](https://github.com/caleywoods/wayfindr/blob/main/res/Config.png?raw=true)

## Features
* Create colored waypoints at your current position or at your crosshair location
* Show/Hide waypoints
* Teleport to waypoints (operators / singleplayer with cheats; toggleable in config)
* Manage waypoints through an in-game GUI
* Per world persistent waypoints that save between game sessions
* Configurable waypoint render distance and placement distance
* Customizable keybindings
* Navigation guidance with on-screen arrow pointing to selected waypoints
* Server-synchronized waypoints that can be shared with other players
* Distinction between personal and shared waypoints
* Real-time waypoint updates across all connected clients
* Optional automatic death waypoints to help recover lost items

## Requirements
* Minecraft 1.21.4 or 1.21.5
* Fabric Loader 0.16.10+
* Fabric API 0.119.2+ (use the build matching your Minecraft version)
* Fabric Language Kotlin 1.13.1+ (use the build matching your Minecraft version)

## Installation
1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version (1.21.4 or 1.21.5)
2. Download and place the following mods in your `mods` folder:
   - [Fabric API](https://modrinth.com/mod/fabric-api) (choose the version for your Minecraft version)
   - [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin/versions) (choose the version for your Minecraft version)
   - Wayfindr (this mod)
3. Launch Minecraft with the Fabric profile

## Commands
* `/waypoint add <name> [<color>]` - Create a colored marker at your current position
* `/waypoint delete <name>` - Remove a named waypoint

## Default Keybindings
* `N` - Quick add a waypoint of a random color at the crosshair location
* `M` - Open the waypoints manager to view and delete waypoints

## Changing Keybindings
Use Minecraft's Controls menu:
1. Open Options > Controls > Key Binds
2. Search for "Wayfindr"
3. Rebind "Open Menu" and "Quick Add" as desired

Note: The Wayfindr Settings screen does not change keybinds; use the Controls menu instead.

## Configuration Options
* **Max Waypoint Render Distance** - How far away waypoints will be visible (default: 100 blocks)
* **Max Waypoint Placement Distance** - How far away you can place waypoints with the quick add key (default: 100 blocks)
* **Death Waypoint** - When enabled, automatically creates a waypoint at your death location (default: Disabled)
* **Teleport Button** - When enabled, shows a Teleport button in the waypoint details for operators (and singleplayer with cheats). Runs `/tp`, so it only works for players with permission level 2+ (default: Enabled)

## Navigation Guidance
The waypoint navigation feature helps guide you to your selected destination:

1. Select a waypoint for navigation by clicking the `>` button next to the waypoint in the list (it changes to `*` while active)
2. Alternatively, select a waypoint and click the "Navigate to Waypoint" button in the details panel
3. An arrow will appear at the top of your screen pointing toward the selected waypoint
4. The distance to the waypoint is displayed below the arrow
5. To stop navigation guidance, click the `*` button again or click "Stop Navigation" in the details panel

## Server Waypoint Sharing
Wayfindr supports sharing waypoints with other players on multiplayer servers:

1. Create a waypoint as usual through the GUI or command
2. Shared waypoints are automatically synchronized with all players on the server
3. All players on the server will receive the shared waypoint
4. Shared waypoints are marked with a `[Shared]` tag in the waypoint list (personal ones show `[Personal]`)
5. Only the waypoint creator or server operators can modify or delete shared waypoints

## Data Storage
* **Client-side waypoints** are stored in `C:\Users\{username}\.minecraft\config\wayfindr\waypoints.json` (paths may vary on other operating systems)
* **Server-side shared waypoints** are stored in the server's world directory under `wayfindr/shared_waypoints.json`

## Technical Architecture
Wayfindr is built with a client-server architecture that enables both personal and shared waypoints:

### Core Architecture
* **Client-side**: Manages personal waypoints and renders all waypoints in the world
* **Server-side**: Acts as the source of truth for shared waypoints
* **Networking**: Custom packet system for synchronizing waypoints between server and clients

### Waypoint Synchronization
1. When a player joins a server, they receive all shared waypoints
2. When a waypoint is shared, all connected clients receive it in real-time
3. Updates to shared waypoints are broadcast to all players
4. Deletion of shared waypoints is synchronized across all clients

### Key Components

#### WaypointManager `WayfindrWaypointManager.kt`
The central component that manages waypoints on the client side:
* Stores and manages both personal and shared waypoints
* Handles waypoint creation, updating, and deletion
* Provides navigation functionality
* Interfaces with the save file handler for persistence

#### Networking `WayfindrNetworking.kt`
Defines the network protocol for waypoint synchronization:
* Establishes communication channels for different waypoint operations
* Implements packet codecs for serializing/deserializing waypoint data
* Registers payload types for client-server communication

#### Network Client `WayfindrNetworkClient.kt`
Handles client-side network operations:
* Processes incoming waypoint packets from the server
* Sends waypoint changes to the server
* Merges server waypoints with local waypoints
* Handles synchronization conflicts

#### Server Waypoint Manager `ServerWaypointManager.kt`
Manages shared waypoints on the server side:
* Maintains the authoritative list of shared waypoints
* Validates waypoint operations based on permissions
* Broadcasts waypoint changes to all connected clients
* Persists shared waypoints to the server's world directory

## Development

### Running the mod locally

To run the mod in a development environment:

1. Clone the repository
2. Open a terminal in the project directory
3. Run the following command:

```bash
./gradlew runclient
```

On Windows, use:

```bash
gradlew.bat runclient
```

This will launch Minecraft with the mod installed in a development environment.

### Version Management

To update the mod version using semantic versioning:

1. Open a terminal in the project directory
2. Run one of the following commands:

```bash
# Default: Bump minor version (1.0.0 -> 1.1.0)
./gradlew setversion

# Bump major version (1.0.0 -> 2.0.0)
./gradlew setversion --args="major"

# Bump patch version (1.0.0 -> 1.0.1)
./gradlew setversion --args="patch"

# Show current version
./gradlew showversion
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

### Building the mod

To build the mod for actual use:

1. Open a terminal in the project directory
2. Run the following command:

```bash
./gradlew build
```

On Windows, use:

```bash
gradlew.bat build
```

3. The compiled mod JAR file will be located in `build/libs/` directory
4. Copy the JAR file (not the ones with `-sources` or `-dev` in the name) to your Minecraft's `mods` folder

### Building for Minecraft 1.21.5

Keep 1.21.4 as your default in `gradle.properties`. To build a 1.21.5 jar without changing defaults, pass -P overrides and a versioned archive name:

```bash
./gradlew build \
  -Pminecraft_version=1.21.5 \
  -Pyarn_mappings=1.21.5+build.1 \
  -Pfabric_version=0.119.2+1.21.5 \
  -Parchives_base_name=wayfindr-mc1.21.5
```

On Windows:

```bash
gradlew.bat build ^
  -Pminecraft_version=1.21.5 ^
  -Pyarn_mappings=1.21.5+build.1 ^
  -Pfabric_version=0.119.2+1.21.5 ^
  -Parchives_base_name=wayfindr-mc1.21.5
```

### Running locally for Minecraft 1.21.5

To run the dev client for 1.21.5 with overrides:

```bash
./gradlew runclient \
  -Pminecraft_version=1.21.5 \
  -Pyarn_mappings=1.21.5+build.1 \
  -Pfabric_version=0.119.2+1.21.5
```

On Windows:

```bash
gradlew.bat runclient ^
  -Pminecraft_version=1.21.5 ^
  -Pyarn_mappings=1.21.5+build.1 ^
  -Pfabric_version=0.119.2+1.21.5
```

## Minecraft 26.2

Minecraft 26.2 is a major toolchain break from the 1.21.x line: Mojang now ships
**deobfuscated** jars (no Yarn/obfuscation mappings) and the game requires **Java 25**.
On the `mc26.2` branch these settings are already the defaults in `gradle.properties`,
so no `-P` overrides are needed — but you must build and run with a JDK 25.

### Prerequisites

* **JDK 25** (Temurin 25 or equivalent). Minecraft 26.2 will not run on Java 21.
* Gradle wrapper (bundled) — pulls Gradle 9.6.1 automatically.

Install Temurin 25 on macOS:

```bash
brew install --cask temurin@25
```

Point Gradle at JDK 25 for the commands below. The simplest way is to prefix each
command with `JAVA_HOME`:

```bash
JAVA_HOME="/path/to/jdk-25" ./gradlew <task>
```

(Find your JDK 25 path on macOS with `/usr/libexec/java_home -v 25`.)

### Building for 26.2

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew build
```

The compiled jar is `build/libs/wayfindr-<version>.jar` (ignore the `-sources` jar).
Copy it into your Minecraft `mods` folder alongside:

* [Fabric API](https://modrinth.com/mod/fabric-api) for 26.2 (`0.155.2+26.2` or newer)
* [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) (`1.13.13+kotlin.2.4.10` or newer)

### Running the dev client for 26.2

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 25)" ./gradlew runClient
```

This launches Minecraft 26.2 with the mod loaded in a development environment.

### 26.2 toolchain summary

| Component | 1.21.x | 26.2 |
|-----------|--------|------|
| Java | 21 | **25** |
| Loom plugin | `fabric-loom` | `net.fabricmc.fabric-loom` |
| Mappings | Yarn | none (deobfuscated jars) |
| Minecraft dep | `net.minecraft:minecraft` | `com.mojang:minecraft` |
| Mod dependencies | `modImplementation` | `implementation` |

> Note: because 26.2 uses a different mappings/toolchain than the 1.21.x versions,
> the `mc26.2` branch is kept separate from the `main` (1.21.4/1.21.5) branch rather
> than sharing a single build.

## Planned Features

* ~~The ability to mark a waypoint as your current destination and have an on-screen arrow pointing towards it~~ ✓ Added!
* ~~Using Fabric global world data to optionally store/sync waypoints to multiple clients~~ ✓ Added!
* Support NeoForge
* Support all 1.21.x versions of Minecraft
* Treat a selected group of waypoints as a graph of connected nodes and determine the fastest route to visit each waypoint (traveling salesman problem)
* Waypoint categories and filtering options
* Custom waypoint icons and shapes
