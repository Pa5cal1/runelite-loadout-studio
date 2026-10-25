# Item identity compatibility audit

Candidate: **0.9.1-rc.6**. Checked: **25 October 2026**.

## Outcome

Inventory Setups, Bank Tag Layouts and native RuneLite do not share a single list of "correct item IDs". They retrieve definitions through RuneLite ItemManager and use different identity rules for exact slots, ordinary Bank items and variant groups. Loadout Studio must preserve these boundaries rather than replacing every ID with a fuzzy base.

The audited Inventory Setups table has **83 custom fuzzy mappings**. Every entry matches the local adaptation. The upstream search table has **1,572 preference entries**, including legitimate imbued variants. These are not validity rules. Manual native selections remain authoritative after RuneLite canonicalization. Only **310 nonstandard defaults** are skipped by automatic Wiki resolution; the other entries receive a ranking penalty but remain available.

## Source revisions

| Source | Revision | Relevant behavior |
| --- | --- | --- |
| Inventory Setups | `16f2c0a319672c2e19738276a17de63aea7216fa` | Canonicalise manual search selection; use RuneLite plus custom fuzzy mappings; exclude temporary/duplicate search candidates |
| Bank Tag Layouts | `d26417d9c6b47f74cdbb685241987ddfd56a7706` | Canonicalise worn gear for layout generation; resolve placeholder IDs; use RuneLite variation groups |
| RuneLite | `fc8d18969e3028af21a78fae38c9ab6c8c7573b1` | ItemManager.canonicalize handles notes, placeholders and worn-weight variants; ItemVariationMapping supplies variant bases |
| Local API fixture | `runelite-api 1.13.3` | 19,997 declared constants, including zero; 19,996 distinct positive values; highest value 34,731 |

Sources are pinned snapshots inspected during this audit. They are not promises about future plugin or cache versions.

## What changed

### Ordinary item identity

The item index records named client definitions and caches RuneLite's canonical Bank ID on the client thread. Rendering, lookup and conversion use the client cache; no Wiki or per-item HTTP lookup was added.

Before copying or previewing an external Inventory Setups/Bank Tag code, the running plugin now checks that definitions have finished indexing and that every positive item/layout ID exists as a named definition. An unknown ID produces a visible error and clears the export preview; it is not sent to the clipboard. The Builder remains intact. Native Loadout Studio export remains available for recovery, including unresolved or future IDs.

This is a definition-existence check, not a statement that the item is owned, obtainable on the current account or appropriate for a fight. A definition can represent a context-specific game item. Explicit imports are preserved rather than silently converted by name.

### Bank export

Both Bank Tag formats now canonicalise IDs through the indexed client definitions, including IDs in the exported layout. Notes, Bank placeholders and worn-only versions therefore refer to the ordinary Bank item. The transformation operates on a copy; the draft, saved library and Inventory Setups JSON retain their exact IDs.

Native Bank variation membership uses the negative RuneLite base ID, matching TagManager's storage convention. Synthetic Inventory Setups comparison-group IDs are not substituted for real item IDs. Inventory Setups-specific fuzzy groups and RuneLite Bank groups can still differ, for example across god-themed equipment. The conversion review now makes that limitation explicit; converting formats is not a lossless way to carry all fuzzy semantics.

### Wiki and search selection

Automatic first-choice lookup skips 310 nonstandard default candidates and ranks the remaining definitions. Legitimate NMZ, Soul Wars and Emir's Arena imbued variants remain available. Manual native search choices are preserved after RuneLite canonicalization and are never rejected merely because they appear in the preference snapshot.

Older Wiki source choices are re-resolved only for a nonstandard automatic default or a targeted legacy ring migration. A saved legitimate imbued variant is preserved. Changes remain drafts until Save. This corrects the earlier blanket exclusion policy, which incorrectly rejected real items such as Salve amulet(ei).

## Offline evidence

| Check | Coverage |
| --- | --- |
| Custom mapping parity | All 83 source-table entries compared with the local table |
| Fuzzy behavior | Every integer from zero through 34,731 checked against the upstream rule: RuneLite mapping first, extra mapping only if unchanged |
| Inventory serialization | All 19,996 positive declared IDs exported and imported in batches; exact values remain unchanged |
| Automatic preferences | All 1,572 snapshot entries retained; only 310 nonstandard defaults skipped |
| Manual selection | All 19,996 positive fixture IDs preserved by the selection policy |
| Legitimate imbues | Three Salve amulet(ei) variants inserted, saved and exported; saved source choices preserved; mask, helm and ring families covered |
| Canonical Bank export | Both formats, every supported container and layout; original draft/Inventory IDs retained |
| Charges and Fuzzy | Exact ring charge ID preserved; native Fuzzy membership uses RuneLite's base |
| Old temporary source choice | Corrected draft and custom position, original source unchanged until Save |
| Invalid external export | Unknown ID blocked before clipboard; native recovery export still available |
| Loading and shutdown | Export waits for index readiness; clearing the index invalidates readiness |

These are offline codec, policy and UI-boundary tests. No game input was automated and no live item container was exhaustively enumerated. The fixture includes declared constants rather than every unnamed or future cache record. It does not prove that every item name, icon, equipment slot or charged variant has the expected meaning in a live client.

## Manual confirmation and maintenance

Test ordinary/worn Graceful, a noted item, a Bank placeholder, a low-charge teleport item, normal Ultor/Magus rings, Rune Pouch and Quiver ammunition. Compare Inventory Setups output with exact source IDs and both Bank Tag outputs with the expected ordinary Bank IDs. Verify the visible conversion warning before relying on Fuzzy behavior across formats.

After RuneLite/cache or upstream changes, rerun the full suite, compare the pinned source tables and review new same-name definitions. The runtime definition index follows the user's actual client, but the curated automatic preferences and upstream snapshot still need maintenance. Future content and removed/renamed constants prevent any permanent 100-percent-correctness guarantee.

## Source links

- [Inventory Setups variation mapping](https://github.com/dillydill123/inventory-setups/blob/16f2c0a319672c2e19738276a17de63aea7216fa/src/main/java/inventorysetups/InventorySetupsVariationMapping.java).
- [Inventory Setups search exclusions](https://github.com/dillydill123/inventory-setups/blob/16f2c0a319672c2e19738276a17de63aea7216fa/src/main/java/inventorysetups/chatbox/InventorySetupsChatboxItemSearchFilter.java).
- [Bank Tag Layouts layout generation](https://github.com/geheur/bank-tag-custom-layouts/blob/d26417d9c6b47f74cdbb685241987ddfd56a7706/src/main/java/com/banktaglayouts/LayoutGenerator.java).
- [RuneLite ItemManager](https://github.com/runelite/runelite/blob/fc8d18969e3028af21a78fae38c9ab6c8c7573b1/runelite-client/src/main/java/net/runelite/client/game/ItemManager.java).
- [RuneLite TagManager](https://github.com/runelite/runelite/blob/fc8d18969e3028af21a78fae38c9ab6c8c7573b1/runelite-client/src/main/java/net/runelite/client/plugins/banktags/TagManager.java).

The exclusion data is attributed to Inventory Setups; its BSD-2 redistribution notice is bundled. There is no runtime dependency on Inventory Setups or Bank Tag Layouts, and no remote table download is introduced.


### Trouver variant checks

The rc.6 offline suite includes all 145 declared Trouver-related IDs from the pinned API fixture, including locked, broken/mangled definitions and the parchment itself. Explicit selection and both export round trips preserve their IDs. Dedicated cape cases cover Infernal max cape (l), Fire max cape (l), Fire cape (l) and Infernal cape (l): exact-name lookup resolves them, exact Bank filtering retains the selected locked item and its layout reference, and does not silently substitute another cape. This tests identity policy and offline boundaries; it does not prove every definition is wearable or owned. Confirm locked cape selection, sprite, save/reopen and filtering in the actual client.
