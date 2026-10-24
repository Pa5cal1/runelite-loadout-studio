# Asset credits

`src/main/resources/com/loadoutstudio/combat-tutor.png` is the OSRS Combat Tutor map icon requested by the user. Source: [Combat Tutor icon on OSRS Wiki](https://oldschool.runescape.wiki/w/File:Combat_Tutor_icon.png), image URL https://oldschool.runescape.wiki/images/Combat_Tutor_icon.png. RuneScape artwork is copyright Jagex. The project license does not transfer ownership of Jagex artwork. Review artwork permissions before public distribution.

The small PNG is bundled in the plugin and scaled with nearest-neighbor rendering. Runtime code does not fetch it or other Wiki images. Other sidebar controls are locally drawn; item sprites use RuneLite ItemManager.

## Inventory Setups compatibility mapping

SetupVariationMapping adapts the extra item-group table from [InventorySetupsVariationMapping](https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/InventorySetupsVariationMapping.java), retrieved 2026-10-23. Copyright (c) 2019, dillydill123; BSD 2-Clause. The full license is included in src/main/resources/com/loadoutstudio/inventory-setups-LICENSE.txt and packaged in the JAR. The plugin does not download or execute source code at runtime.

## Bolt Pouch mapping

GearContainers bundles the bolt-type-to-Item-ID mapping from Inventory Setups Bolts.java (retrieved 2026-10-23, BSD-2-Clause). Its full notice is retained in the source and the existing packaged inventory-setups-LICENSE.txt. This is bundled compatibility data, not a runtime dependency or download. Combat attack-option labels are read from the local game-cache DBTable; no Inventory Setups classes are loaded.

## Documentation screenshots

Nine PNGs in docs/screenshots are offline renders of the actual Loadout Studio Swing sidebar with Loadout Studio's component-local theme over RuneLiteLAF and explicit example data. The diagnostics view uses example counters and the same actual sidebar support controls. Three additional settings previews are generated from the current config declarations and default values; they reconstruct the documentation view rather than capturing the native ConfigPanel. They are labeled UI previews, not live game screenshots. They contain no player-specific state.

Item artwork is copyright Jagex. The small sprite inputs were obtained once for documentation preparation from RuneLite's static item-icon service, for example https://static.runelite.net/cache/item/icon/12931.png. They were reused locally to render the previews. Individual input sprites and the local renderer are not bundled into the plugin. Runtime sprites continue to use RuneLite ItemManager; these screenshots add no Wiki or image requests to the plugin. The project license does not transfer ownership of RuneScape artwork.

Settings previews use RuneLite fonts, styling and the section-arrow resource from the RuneLite client. See the [RuneLite source and license](https://github.com/runelite/runelite). No additional runtime image request is introduced.

The 0.9 sidebar wordmark treatment, route glyphs and checkbox controls are locally drawn Java UI code. They introduce no downloaded fonts or image requests. Settings documentation continues to represent the native RuneLite configuration style.
