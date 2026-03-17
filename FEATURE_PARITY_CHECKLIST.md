# Forge Port Feature Checklist

Source parity reference: ../FEATURE_PARITY_SPEC.md

## Completed in this cycle
- New standalone Forge 1.21.11 project created in this folder.
- Mod metadata renamed to derk_easy_inventory_crafter / Bluethooth Chest.
- Custom JSON config system implemented with defaults, clamping, and save/load.
- Nearby inventory scanner implemented.
- Packet channel implemented (request nearby, nearby payload, highlight request/response, return nearby).
- Nearby sync service implemented on server side.
- Client nearby-state cache implemented, including locate aim and particle trail behavior.
- Crafting and inventory nearby-panel UI implemented (button, search, categorized sorting, scrolling, click-highlight, count overlay).
- Client input routing mixins implemented for panel click/scroll/type handling and recipe-book key interception.
- Client auto-refresh loop while crafting/inventory screens are open is implemented.
- Runtime highlight feedback implemented (particle-based chest highlight + optional distance readout + snap/locate integration).
- Project builds successfully with `gradlew build`.
- Client runtime launches with `gradlew runClient` (no hard crash observed in launch smoke test).

## Still to port for full parity
- Crafting-table withdrawal tracking and rollback/cancel mechanics in crafting menu internals.
- Nearby-assisted autofill integration into recipe placement flow.
- Recipe finder augmentation in crafting menus (server and client parity behavior).
- Result-slot and autofill-triggered nearby refresh hooks.
- Filled world-space box renderer parity for highlights (current implementation uses particle aura + distance readout).
- Double chest merged bounding highlight parity for world-space renderer path.
- Config GUI screens (main settings + color picker) and in-game entrypoint.
- Spacebar one-set recipe quick place behavior.
- All lifecycle edge cases and cleanup parity from Fabric mixins.

## Validation gates pending
- Build success after each subsystem addition.
- runClient and dedicated runtime log checks after each subsystem.
- End-to-end parity test pass against all items in FEATURE_PARITY_SPEC.md.
