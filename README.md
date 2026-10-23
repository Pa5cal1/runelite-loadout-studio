# Loadout Studio

Create and organise gear setups for Old School RuneScape. Find loadouts on the OSRS Wiki, edit your Inventory and Equipment, and use your setups with Bank Tags or Inventory Setups.

**Current version: 0.7.0-test.** Loadout Studio is a local test plugin and is not yet available through the RuneLite Plugin Hub.

Loadout Studio works without the Inventory Setups and standalone Bank Tag Layouts hub plugins. It owns its library, editor, comparison and codecs. Bank filtering/layouts require RuneLite's built-in **Bank Tags** plugin. Optional imports create independent copies.

## Contents

- [Getting Started](#getting-started)
- [Creating a Setup](#creating-a-setup)
- [Your Setup Library](#your-setup-library)
- [Editing Items](#editing-items)
- [Finding Wiki Setups](#finding-wiki-setups)
- [Bank Filtering and Layouts](#bank-filtering-and-layouts)
- [Comparing Your Gear](#comparing-your-gear)
- [Importing and Exporting](#importing-and-exporting)
- [Saved Setups and Backups](#saved-setups-and-backups)
- [Support and Suggestions](#support-and-suggestions)
- [Development](#development)
- [Changelog](#changelog)

## Getting Started

Build and launch the test client using the instructions under [Development](#development). Enable **Bank Tags** and **Loadout Studio (Test)** in RuneLite, then click the sword icon in the sidebar.

![Loadout Studio sidebar icon](src/main/resources/com/loadoutstudio/combat-tutor.png)

The sidebar has four tabs:

- **Library** — find and organise your saved setups.
- **Builder** — view and edit the selected loadout.
- **Wiki** — search for an activity and choose a setup.
- **Export** — copy a setup in the format you want to use.

## Creating a Setup

To save the gear you are wearing, open **Library** and click the camera button, **Capture gear**. This creates a draft from your current Inventory, Equipment, Rune Pouch, Quiver and Spellbook.

To start from scratch, click the plus button, **New setup**.

Give the setup a name in **Builder**, make any changes you need, then click **Save**. To keep the original and save another version, expand **Setup actions** and choose **Save copy**.

You can also create a setup from the [Wiki](#finding-wiki-setups) or [import one](#importing-and-exporting).

## Your Setup Library

Click a setup's name to open it. Use the search bar to find setups by name, `item:<name or ID>` or `notes:<text>`. Choose Alphabetical, Recent, Favorites first or Manual sorting. Right-click a setup name to move it up/down within its section; moving selects Manual order. Recent is recorded per character/profile. Long names are shortened in the list; hover over a name to see it in full.

### Favorites

Click the star beside a setup to mark it as a favorite. Enable **Favorites** to show only your favorites.

### Sections

Use the **Sections** button at the top of Library to create, rename or remove a section. To assign a setup, open it, expand **Setup details**, choose a section and click **Save**.

Click a section heading to expand or collapse it. Search and Favorites temporarily expand matching sections; clearing those filters restores your previous section state. Setups without a section appear under **Unfiled**. Removing a section moves its setups back to Unfiled.

### Archiving and Deleting

Click the archive-box icon beside a setup to move it out of the active list. Enable **Archived** to view archived setups.

Archived setups have a restore arrow and a red trash icon:

- Click the **restore arrow** to return a setup to the active list.
- Click the **trash icon** to delete it permanently. You will be asked to confirm, with **Cancel** selected by default.

If the setup is open in Builder, the confirmation also explains that its draft and unsaved changes will be discarded.

## Editing Items

Open a setup in **Builder** to see its Inventory, Rune Pouch, Equipment, Spellbook, Quiver, Additional filtered items and Notes.

Right-click an item slot to replace it, change its quantity, adjust matching options or remove it. **Update Slot from Search** opens RuneLite's item search in the chatbox. You can also update a slot from the corresponding item in your current Inventory or Equipment.

Expand **Find items** to search the local item list by English name or Item ID. Double-click a result, or select **Add to Inventory**, to fill the next empty Inventory slot.

### Dragging Items

Drag an item from Find items into a slot to add it to your setup. Drag between slots to move or swap items. Equipment, Rune Pouch and Quiver slots check that the item fits.

These edits change the loadout you are building. Withdraw and equip your gear through the usual RuneLite controls.

### Quantities and Rune Pouch

Enter a number such as `15000`, or use a shortcut such as `15k`.

- **0** means unlimited matching: the item must be present, but its quantity is ignored.
- Rune Pouch slots have a maximum of **16,000 runes per type**.
- Inventory stacks can exceed 16,000.

Expand **Loadout options** and select **Max pouch runes (16k)** to fill the quantities of existing Rune Pouch entries. Missing Wiki quantities still need to be entered before export.

### Additional Filtered Items and Notes

Use **Loadout options → Add filtered item** for supplies you want to see in the Bank filter, such as pre-pots, without adding them to the main loadout.

Write instructions in **Notes** below the items. The notes area grows with the text and uses the sidebar's main scrollbar.

**Ctrl+F** focuses the relevant search field. **Ctrl+S** saves the draft.

### Owned Gear Alternatives

Open **My gear alternatives → Check my Bank** while logged in. Open the Bank at least once in the current session first. The plugin reads local Bank, Inventory and Equipment data; Bank placeholders do not count as owned items. Closed-Bank results show the observation time and can be out of date. Other storage is not included.

Missing items can offer Wiki alternatives, a small bundled gear/supply group, or other observed Equipment for the same slot. Each suggestion states its basis. Same-slot choices do not verify combat style, requirements, ammo, spells or boss suitability. The first Wiki choice remains the default; each replacement starts at **Keep original**. Select the replacements you want, then click **Use selected alternatives**. Availability is checked again before applying. Review and Save; **Undo alternatives** is available until another draft edit makes it unsafe. Rune types are never freely substituted. No Wiki request is needed for this operation.

### Upgrading Saved Setups

Save your current draft, then use **Library tools → Upgrade item in setups**. Choose the old and new item using chatbox search, select the active setups to change and confirm the preview. Only exact Item IDs are replaced; amounts, matching rules, Notes and layout positions are retained. Replacement Equipment must use the same slot. Review combat style and requirements yourself. **Undo last upgrade** works while no later library/draft change would be lost.

### Bolt Pouch and Attack Option

Capture gear includes a carried Bolt Pouch and the current attack option when available from the game cache. Existing Bolt Pouch entries can be edited like other slots. **Loadout options** can add four Bolt Pouch slots, capture the current attack option or clear that preference. The saved attack option is checked, never changed in game.

## Finding Wiki Setups

1. Open **Wiki** and enter an activity, such as Vorkath.
2. Press **Enter** or click **Search Wiki**.
3. Select a page. Leave **Use Strategies subpage** enabled to look for its strategy page.
4. Choose one of the available setups to open it in Builder.
5. Review the loadout and click **Save**.

The resolved Wiki page title becomes the setup name. You can rename it in Builder.

Known alternatives use the first listed item by default. When several game items share a name, the first local catalogue result is used. Items you have already selected by Item ID stay unchanged. You can replace any default through item search.

`Cheap food` defaults to Manta ray and `Cheap prayer` to Prayer potion(4). These are fixed choices, rather than a live price comparison. Source conditions and gear combinations remain in Notes. A `?` means that a source expression could not be resolved; use item search to finish that slot. A page containing only gear recommendations still needs its Inventory, quantities and Spellbook reviewed.

For a specific page, expand **URL / pasted wikitext**. Pasted wikitext can be used offline.

### Cached Pages

Previously fetched pages are kept locally and reused after restarting the client. **Use cache** reopens the stored version. **Refresh Wiki** requests an updated version after confirmation. The panel shows when the source was fetched and whether it is out of date.

Starting the client, opening saved setups and typing in the search field do not send Wiki requests. Typing can suggest pages already in the local cache. An explicit search or page selection can contact the Wiki. The Wiki receives your IP address, search text and requested page title. Item images come from RuneLite's game cache.

See the [Wiki request policy](docs/wiki-network-policy.md) for the full caching and request limits.

## Bank Filtering and Layouts

**Bank Tags must be enabled.**

Open a setup and use **Filter Bank** / **Clear filter** above the loadout for quick access. To choose a layout, expand **Bank filtering and layout**:

- **Preset** places Equipment on the left and Inventory on the right.
- **Zigzag** alternates items between two rows. Choose which row starts first.
- **Imported / custom** keeps an imported layout's positions.

Drag items in your active Bank layout to customise their positions. These changes update the Builder draft; click **Save** to retain them across reopening/restarts. The mode becomes Custom. Choosing Preset/Zigzag explicitly regenerates positions.

A configurable **Toggle Bank filter** hotkey works only while the Bank is open and is unset by default. The worn-items button can offer up to five favorite/recent setup shortcuts; disable **Bank setup shortcuts** in config if desired.

Click **Apply filter** to show the setup's items in an open Bank, or apply it when you next open the Bank. Click **Clear filter** to turn filtering off for the draft. Save the setup to keep these preferences.

The filter includes Equipment, Inventory, Rune Pouch, Quiver and Additional filtered items. The Bank title displays the setup name. If you are also using another setup plugin, keep one setup filter selected at a time.

## Comparing Your Gear

Gear differences update from local Inventory, Equipment, Rune Pouch, Quiver, Bolt Pouch, Spellbook and attack-option changes. Unchanged snapshots do not rebuild the sidebar; editing Notes keeps the same editor. The fixed status and slot tooltips explain differences. **Compare** also requests a fresh check. Actual event handling still requires manual RuneLite acceptance.

Unordered Inventory matching ignores extra items and requires distinct matches for duplicate required items. Exact and Fuzzy/quantity rules remain per item.

Right-click a slot to change its matching options:

- **Toggle Fuzzy** allows supported related variants, such as different charges or potion doses.
- **Stack comparison** controls whether the amount is ignored, must match exactly, or is checked against a minimum or maximum.

**Ignore Ammo quantities** is under Loadout options. Imported unordered Inventory matching is also respected. Spellbook differences are checked when the setup specifies one.

Matching rules can differ between receiving plugins, particularly for custom Fuzzy groups.

## Importing and Exporting

### From Other Plugins

In Library, click **Import setup → From installed plugins**. Select the setups you want, review them, then choose **Save copies**. **Open copy** opens one as an editable draft.

Loadout Studio reads supported Inventory Setups, Bank Tags and legacy Bank Tag Layouts data from the current RuneLite profile. It creates independent copies and retains available section names, favorites and layouts. The originals remain in their own plugins.

If an entry is unsupported, export it through its original plugin and use **Paste JSON / Bank Tag** instead.

### Share Selected Setups or a Section

Use **Library tools → Share setups / section**, select individual setups or a section, and confirm. The bundle includes the selected setups and their Notes. Importing this bundle adds independent copies and retains existing setups; it does not replace the whole library. Whole-library backups retain their separate restore flow.

### From a Code

Click **Import setup → Paste JSON / Bank Tag**, paste an Inventory Setups export or Bank Tag code, and review the imported setup.

### Exporting a Setup

Open **Export** and choose a format. Click **Show code** to preview it or **Copy code** to copy it:

- **Inventory Setup JSON** — portable JSON for the Inventory Setups plugin.
- **Bank Tag (RuneLite)** — a native RuneLite Bank Tag, with its Bank layout.
- **Bank Tag Layouts** — a legacy layout string for that plugin's matching import action.

To import the result in Inventory Setups, use its green import icon → **Import setup… → Enter setup data**. For native Bank Tags, right-click the Bank's **+** tab and select **Import tag tab**.

**Bank Tags do not store worn Equipment slots, quantities, Spellbook, Notes or Rune Pouch contents.** Converting a Bank Tag back to an Inventory Setup puts its items in Inventory, with overflow in Additional filtered items. Fill in the missing details before using it as a complete loadout.

## Saved Setups and Backups

Your library is separated by RuneLite profile and character, with a separate library when no character is selected. RuneLite may synchronise configuration according to your RuneLite settings.

Use **Library tools → Copy library backup** to keep a backup of your saved setups.

If you leave unsaved Builder changes, Library can offer **Restore draft** when you return. Restoring opens the draft for review; click **Save** to keep it in your library.

## Support and Suggestions

Have a suggestion or found a bug? Open an [issue](https://github.com/Pa5cal1/runelite-loadout-studio/issues) with the activity name, what you expected and the steps needed to reproduce it.

### Donations

If you would like to support development, you can [buy me a coffee](https://buymeacoffee.com/pascalvdbe9). Donations are optional.

## Development

<details>
<summary>Building and testing the local client</summary>

Use JDK 11 and run these commands from the repository root:

```powershell
.\gradlew.bat test shadowJar jar --no-daemon
.\scripts\Launch-Test.bat
```

On Linux or macOS, build with `sh gradlew test shadowJar jar --no-daemon`.

The `*-all.jar` is the development client; the regular JAR contains the plugin. Run the development client separately from your usual RuneLite installation. For Jagex Accounts, follow RuneLite's [development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts).

Source is under `src/main/java/com/loadoutstudio/`, with tests under `src/test/`. The launcher is in `scripts/` and supporting documentation is in `docs/`.

Automated tests cover data formats, item matching, layouts, caching and sidebar behaviour. In-client acceptance is still required before a Plugin Hub submission.

- [Manual test guide](docs/local-acceptance.md)
- [Validation results](docs/VALIDATION.md)
- [Sidebar design](docs/sidebar-system-design.md)
- [Plugin Hub submission notes](docs/plugin-hub/README.md)

</details>

## Changelog

### 0.7.0-test

- Offer explicitly selected alternatives from locally observed gear, with source/group/same-slot explanations, availability recheck and guarded Undo.
- Update gear differences from local changes, fix unordered extra-item behavior and add Bolt Pouch/attack-option support.
- Save native Bank layout edits back into the active draft as Custom.
- Add Recent, Favorites-first and Manual ordering, item/Notes searches, bulk item upgrade with preview/Undo, and selected sharing bundles.
- Add configurable Bank filter hotkey and favorite/recent Bank-menu shortcuts.
- Keep Inventory Setups and standalone Bank Tag Layouts optional; the only plugin dependency is built-in Bank Tags.

### 0.6.3-test

- Keep Bank filtering and gear comparison accessible above the scrolling loadout.
- Reveal matching setups in collapsed sections while searching or filtering Favorites, and restore the previous section state afterwards.
- Add direct starting actions to an empty Library and a clear-filters action for empty search results.
- Reject a delayed Capture gear result if the Builder, profile or open setup changed while it was loading.

### 0.6.2-test

- Separate archive, restore and permanent-delete actions.
- Confirmation before deleting an archived setup.

### 0.6.1-test

- First-listed defaults for Wiki alternatives and common food/prayer templates.
- Local resolution of unfinished items in older saved setups.

### 0.6.0-test

- Persistent Wiki cache with source dates and explicit refresh.
- Import copies from installed Inventory Setups and Bank Tags plugins.
- Unsaved draft recovery, conversion previews and expanded gear comparison.
- Repaint item slots when RuneLite finishes loading their images.

## Credits

Thanks to the [RuneLite](https://github.com/runelite/runelite), [Inventory Setups](https://github.com/dillydill123/inventory-setups) and [Bank Tag Layouts](https://github.com/geheur/bank-tag-custom-layouts) projects. See [asset and code credits](docs/ASSETS.md) for attribution and licences.
