## Wayfindr

### What's this?
A minecraft waypoints mod. Creating a mod has been on my to-do list for quite awhile so this project was created to finally fulfill that. It's written in Kotlin and targets the [Fabric mod ecosystem](https://fabricmc.net/).

### What's it do?
Right now, it creates a red cube at 0, 95, 0 (likely in the sky near your spawn point) and responds to `/waypoint add <name>` and `/waypoint delete <name>` with text. My plan for features is:

* A command syntax like `/waypoint add <name> [color]` and `/waypoint delete <name>`
* Storing waypoints in a JSON file (see example below)
* A first, just displaying waypoints on the minimap which probably has limited range so eventually...
* A rendering system allowing light beams to be rendered if desired, per waypoint using Fabric `BufferBuilder` and hooks
* A custom settings UI where the user can add, delete, or change the properties of a waypoint
* Stretch feature: The ability to mark a waypoint as your current destination and have an on-screen arrow pointing towards it or a hotkey that when held/toggled will show the arrow
* Stretch feature: Treat a selected group of waypoints as a graph of connected nodes and determine the fastest route to visit each waypoint, aka the travelling salesman problem
* Stretch feature: Using Fabric global world data to optionally store/sync waypoints to multiple clients