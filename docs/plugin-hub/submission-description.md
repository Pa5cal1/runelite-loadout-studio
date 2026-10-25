# Plugin Hub submission — Loadout Studio

**Name:** Loadout Studio  
**Author:** Pa5cal1  
**Version:** 0.9.1-rc.18  
**Description:** Create and organise gear loadouts with Wiki presets and owned alternatives

## Pull request title

Add Loadout Studio

## Pull request body

Loadout Studio creates, captures and organises gear loadouts in a character-specific sidebar library. Players can load an OSRS Wiki preset, review source-ranked alternatives from their locally observed gear and save their adapted setup.

Features include Inventory/Equipment editing, Rune Pouch and Quiver planning, sections/favorites, native chatbox item selection, drag-and-drop, comparison/highlighting, autosave and portable backups. Inventory Setups JSON, native Bank Tags and legacy Bank Tag Layouts exports are supported, with warnings about data lost during conversion.

The plugin does not depend on Inventory Setups or Bank Tag Layouts. Its independent Bank layout supports built-in Bank Tags enabled or disabled, Preset/Zigzag/custom positions and native item operations. Normal Bank stock is preferred; native Potion Storage widgets provide available selected-dose potions. No automatic withdrawal/equipping, injected input or direct action invocation is used. Bank menu entries that select setups/layouts are local controls; item interactions retain native operation routing.

Wiki access occurs only after explicit search/page actions. Responses are cached across restarts; refresh-on-expiry happens only on a later user open. The request queue has one in-flight request, minimum two-second spacing, a rolling ten-start/minute limit, bounded pending work and error backoff. Startup, typing, saved-setup opens, item images and gear checks make no Wiki requests. Gear and setups are not uploaded. Diagnostics are bounded, session-local and manually copied only. Donations unlock no features.

Validation: 289 Java 11 regression tests pass locally. The repository's Java 11 GitHub Actions checks passed for the reviewed rc.18 source. The player confirmed real food Withdraw-5, potion withdrawal/dose selection, autosave and the final Library section-drop flow. Documentation screenshots are refreshed offline renders using example data, clearly labeled as previews; they are not automated gameplay evidence. Cross-profile live races, every item variant and all third-party Bank-filter combinations are not exhaustively proven.

Please review the native Bank widget/operation integration, potion-storage adapter, cross-plugin read-only imports, network disclosure and the lifetime/cleanup of plugin-owned temporary tags. No new runtime dependency is requested; build type is standard. Bundled compatibility data retains the corresponding BSD notices.

Documentation and previews: https://github.com/Pa5cal1/runelite-loadout-studio#screenshots

Validation and limitations: https://github.com/Pa5cal1/runelite-loadout-studio/blob/main/docs/release-readiness.md

Generated-by: Codex

## Installation warning

This plugin sends your IP address, search queries and requested page titles to the OSRS Wiki. It does not upload your gear or saved setups.

The Hub controls the installation dialog and final warning wording. Submission is not approval. Reviewer changes will update the same pull request; a stable 1.0.0 will use its own tested source commit.
