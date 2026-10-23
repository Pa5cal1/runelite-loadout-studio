# Asset credits

`src/main/resources/com/loadoutstudio/combat-tutor.png` is the OSRS Combat Tutor map icon requested by the user. Source: [Combat Tutor icon on OSRS Wiki](https://oldschool.runescape.wiki/w/File:Combat_Tutor_icon.png), image URL https://oldschool.runescape.wiki/images/Combat_Tutor_icon.png. RuneScape artwork is copyright Jagex. The project license does not transfer ownership of Jagex artwork. Review artwork permissions before public distribution.

The small PNG is bundled in the plugin and scaled with nearest-neighbor rendering. Runtime code does not fetch it or other Wiki images. Other sidebar controls are locally drawn; item sprites use RuneLite ItemManager.

## Inventory Setups compatibility mapping

SetupVariationMapping adapts the extra item-group table from [InventorySetupsVariationMapping](https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/InventorySetupsVariationMapping.java), retrieved 2026-10-23. Copyright (c) 2019, dillydill123; BSD 2-Clause. The full license is included in src/main/resources/com/loadoutstudio/inventory-setups-LICENSE.txt and packaged in the JAR. The plugin does not download or execute source code at runtime.

## Bolt Pouch mapping

GearContainers bundles the bolt-type-to-Item-ID mapping from Inventory Setups Bolts.java (retrieved 2026-10-23, BSD-2-Clause). Its full notice is retained in the source and the existing packaged inventory-setups-LICENSE.txt. This is bundled compatibility data, not a runtime dependency or download. Combat attack-option labels are read from the local game-cache DBTable; no Inventory Setups classes are loaded.
