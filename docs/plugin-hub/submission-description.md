# Proposed Plugin Hub listing

**Name:** Loadout Studio  
**Author:** Pa5cal1  
**Candidate:** 0.9.1-rc.12  
**First stable release after acceptance:** 1.0.0

**Short description:** Create and organise gear loadouts with Wiki presets and owned alternatives

This is original wording in the concise, feature-led style of the reference listings. It does not copy their descriptions or imply affiliation.

## Proposed submission description

Loadout Studio lets players create, capture and organise gear loadouts in a character-specific sidebar library. Players can search an OSRS Wiki activity, choose a preset and review source-ranked alternatives from their observed Bank and gear before saving a personal copy.

The Builder includes Inventory, Equipment, Rune Pouch, Quiver, Notes, manual chatbox item editing and drag-and-drop. Preset and Zigzag Bank views, favorites, sections, comparison flags, recovery and portable backups support repeated gearing. Inventory Setups JSON and Bank Tag formats are supported with conversion-loss warnings.

Inventory Setups and Bank Tag Layouts are not runtime dependencies. Built-in Bank Tags integration is optional; an independent Bank view handles its disabled state. Wiki traffic is explicit and cached, with a single bounded request queue, no startup polling and no image/per-item Wiki service. Diagnostics stay local and are copied only after user review. Support donations do not unlock functionality.

Please review the Wiki network disclosure, chatbox inputs, Bank widget/layout integration, cross-plugin data import and plugin-owned temporary Bank Tag lifecycle. The acceptance record and precise source revision should be supplied with the actual submission. This document is preparation, not an open PR or approval claim.

## Version policy

Use `0.9.1-rc.12` for this candidate. Increment the `rc` number for fixes and retest before stable release. After manual acceptance, update build metadata, diagnostics, User-Agent, launcher, changelog and manifest pin together to `1.0.0`. Later compatible features increment the minor number; fixes increment the patch number. A version label alone does not trigger a Hub update: the manifest must point to the reviewed commit.

## Installation warning proposal

This plugin sends your IP address, search queries and requested page titles to the OSRS Wiki. It does not upload your gear or saved setups.

The Hub owns the warning dialog and final wording. No extra installation dialog is added to the client.
