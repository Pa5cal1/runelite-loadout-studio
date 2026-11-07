# Plugin Hub submission — Loadout Studio

**Name:** Loadout Studio  
**Author:** Pa5cal1  
**Version:** 0.9.1-rc.18  
**Description:** Create and organise gear loadouts with Wiki presets and owned alternatives  
**Status:** Submitted in [PR #19421](https://github.com/runelite/plugin-hub/pull/19421); review pending.

## Pull request title

Add Loadout Studio

## Pull request body

Loadout Studio lets players create and organise character-specific gear loadouts, load Wiki presets and choose alternatives from their owned gear. It includes sections, favourites, autosave, drag-and-drop and Inventory Setups/Bank Tags import and export.

Bank layouts work with built-in Bank Tags enabled or disabled, including Potion Storage. Withdrawals use native item operations; the plugin does not automate withdrawal or equipping.

Wiki requests follow explicit user actions and use a persistent cache and rate limits. Gear and saved setups are not uploaded. Diagnostics stay local and are shared only when the user copies them.

All 289 Java 11 tests, RuneLite Checkstyle and the Plugin Hub build pass for the pinned source. Food/potion withdrawals, autosave and Library section drops have also been tested by the player.

[Features and screenshots](https://github.com/Pa5cal1/runelite-loadout-studio#screenshots) · [Validation and limitations](https://github.com/Pa5cal1/runelite-loadout-studio/blob/main/docs/release-readiness.md) · [API review notes](https://github.com/Pa5cal1/runelite-loadout-studio/blob/main/docs/release-readiness.md#api-review)

Please review the native Bank/Potion Storage integration and Wiki access. The plugin uses the standard build and adds no runtime dependencies.

## Installation warning

This plugin sends your IP address, search queries and requested page titles to the OSRS Wiki. It does not upload your gear or saved setups.

The Plugin Hub controls the installation dialog and reviews the final warning wording.
