## Wayfindr

<img src="https://github.com/caleywoods/wayfindr/blob/main/wayfindr_logo.png?raw=true" width="350" />

## What's this?
A Minecraft waypoints mod written in Kotlin targeting the [Fabric mod ecosystem](https://fabricmc.net/). Wayfindr allows you to create and manage waypoints throughout your Minecraft world.

## Features
* Create colored waypoints at your current position or at your crosshair location
* Manage waypoints through an in-game GUI
* Persistent waypoints that save between game sessions
* Configurable render distance and placement distance
* Customizable keybindings

## Commands
* `/waypoint add <name> [<color>]` - Create a colored marker at your current position
* `/waypoint delete <name>` - Remove a named waypoint

## Default Keybindings
* `N` - Quick add a waypoint of a random color at the crosshair location
* `M` - Open the waypoints manager to view and delete waypoints

## Configuration Options
* **Max Waypoint Render Distance** - How far away waypoints will be visible (default: 100 blocks)
* **Max Waypoint Placement Distance** - How far away you can place waypoints with the quick add key (default: 100 blocks)
* **Open Menu Key** - Keybinding to open the waypoints manager (default: M)
* **Quick Add Key** - Keybinding to quickly add a waypoint (default: N)

## Data Storage
Waypoints are stored in `C:\Users\{username}\.minecraft\config\wayfindr\waypoints.json` (paths may vary on other operating systems)

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

## Planned Features

* The ability to mark a waypoint as your current destination and have an on-screen arrow pointing towards it
* Treat a selected group of waypoints as a graph of connected nodes and determine the fastest route to visit each waypoint (traveling salesman problem)
* Using Fabric global world data to optionally store/sync waypoints to multiple clients