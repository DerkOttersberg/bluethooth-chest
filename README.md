# Bluethooth Chest

Bluethooth Chest lets the vanilla crafting table use items stored in nearby chests.

## What the mod does

When you open a crafting table, the mod scans nearby storage and treats those items as available for crafting. You can still craft normally from your own inventory, but you no longer need to move every ingredient out of nearby chests first.

## Main features

- Pulls crafting ingredients from nearby chests while using the normal vanilla crafting table.
- Supports both your own inventory and nearby storage at the same time.
- Updates the recipe book so craftable recipes turn available when nearby chests contain the required items.
- Shows a nearby-items panel with item counts, search, scrolling, and sorted entries.
- Lets you click an item in the nearby panel to highlight the chest or double chest that contains it.
- Renders a filled highlight and optional distance label for highlighted storage.
- Includes a cancel button that returns nearby-sourced crafting ingredients back to chest storage.
- Keeps cancel behavior server-side to avoid item duplication and item loss edge cases.
- Adds Mod Menu settings for highlight color, highlight duration, nearby radius, opacity, distance labels, panel default state, and refresh interval.
- Includes a spacebar shortcut for quick one-set recipe filling.

## Settings

If Mod Menu is installed, the mod exposes a config screen with:

- highlight color
- highlight duration
- nearby distance radius
- highlight opacity
- distance label toggle
- nearby panel default open or closed
- auto refresh interval

## Notes

- Nearby crafting works through crafting tables, not as a global inventory extension.
- Highlighting supports double chests.
- On singleplayer, nearby radius follows your local config. On a dedicated server, the server config controls the actual scan radius.

## Development

- Minecraft: 1.21.11
- Fabric Loader: 0.18.4
- Yarn: 1.21.11+build.4
- Fabric API: 0.141.1+1.21.11
- Java: 21

## Run the client

1. Run `gradlew runClient` from the project root.
2. Open a world.
3. Place a crafting table near one or more chests.
4. Open the crafting table and test nearby crafting, recipe book updates, chest highlighting, and the nearby panel.
