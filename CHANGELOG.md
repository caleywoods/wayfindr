# Changelog

## 1.7.0

### New

- **Sort your waypoint list.** Use the sort button to order waypoints A to Z, Z to A, or nearest first. Wayfindr remembers your choice.
- **Delete confirmation.** Deleting a waypoint now asks you to click twice, so you won't remove one by accident. You can turn this off in Settings.
- **See a waypoint's dimension.** When you select a waypoint, its details now show whether it's in the Overworld, the Nether, or the End.
- **A cleaner list.** The `[Personal]` and `[Shared]` tags are hidden by default now. If you liked them, turn them back on in Settings.
- **Sharing controls for server owners.** If you run a server, you can turn off waypoint sharing for everyone or stop specific players from sharing. This is a Minecraft 26.2 feature. See the README for how to set it up.

### Changed

- New waypoints from the "+ Add Waypoint" button now get a random color instead of always red, so they're easier to tell apart at a glance.

### Fixed

- **Teleporting across dimensions works.** Teleporting to a waypoint in the Nether or the End now takes you to the right place, instead of the same coordinates in the dimension you're already in.
- **Waypoints stay in their own dimension.** A Nether waypoint no longer appears in the Overworld, and an Overworld waypoint no longer appears in the Nether.
- **Shared waypoints don't get lost.** If a server turns down a waypoint you tried to share, it stays as a personal waypoint instead of disappearing.

### Improved

- **Smoother performance.** The navigation arrow is much lighter on your frame rate, the mod sends less data over the network on servers, and saving stays fast even with lots of waypoints.
