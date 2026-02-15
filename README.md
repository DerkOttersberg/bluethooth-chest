# Bluethooth Chest

Craft straight from nearby chests at the vanilla crafting table.

## Features
- Automatically pulls ingredients from nearby chests (radius 16).
- Recipe book reflects nearby storage availability.
- Nearby items panel with counts, search, scroll, and category sorting.
- Click an item to highlight all chests containing it (5 seconds).
- Tooltip on hover and click feedback (sound + pulse).
- One-set crafting with spacebar.

## Development
- Minecraft: 1.20.1
- Fabric Loader: 0.15.11
- Yarn: 1.20.1+build.10
- Fabric API: 0.92.2+1.20.1
- Java: 17

## Run (client)
1. Run `gradlew runClient` from the project root.
2. Open a world and use a crafting table near chests to test.

## Testing
```bash
./gradlew test
```
