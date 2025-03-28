## Wayfindr

<img src="https://github.com/caleywoods/wayfindr/blob/main/wayfindr_logo.png?raw=true" width="350" />

### What's this?
A minecraft waypoints mod. Creating a mod has been on my to-do list for quite awhile so this project was created to finally fulfill that. It's written in Kotlin and targets the [Fabric mod ecosystem](https://fabricmc.net/).

### What's it do?
Right now you can:

* `/waypoint add <name> [<color>]` to create a colored marker at the player position
* `/waypoint delete <name>` to remove a named waypoint

My plan for features is:

* Storing waypoints in a JSON file (currently just stored per session)
* A custom settings UI where the user can add, delete, or change the properties of a waypoint
* Stretch feature: The ability to mark a waypoint as your current destination and have an on-screen arrow pointing towards it or a hotkey that when held/toggled will show the arrow
* Stretch feature: Treat a selected group of waypoints as a graph of connected nodes and determine the fastest route to visit each waypoint, aka the travelling salesman problem
* Stretch feature: Using Fabric global world data to optionally store/sync waypoints to multiple clients