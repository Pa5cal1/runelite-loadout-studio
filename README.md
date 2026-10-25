# Loadout Studio

Create, capture and organise gear loadouts for Old School RuneScape. Find Wiki presets, adapt them to your owned gear and keep your setups in a character-specific library.

**Version: 0.9.1-rc.18.** Prepared for RuneLite Plugin Hub review; not yet approved or available through the Hub. The reviewed candidate version is retained for submission. Installation through the Hub will become available only after RuneLite accepts and publishes it.

Loadout Studio works independently of Inventory Setups and the standalone Bank Tag Layouts plugin. Built-in Bank Tags is optional. No Docker installation is required.

## Screenshots

Offline renders of the actual sidebar using RuneLite's neutral gray palette with gold accents and example loadouts. These are UI previews, not captures of a logged-in client or proof of live Bank behavior.

![Library, Builder, Wiki and Export previews](docs/screenshots/overview.png)

[Library](docs/screenshots/library.png) · [Builder](docs/screenshots/builder.png) · [Equipment and Notes](docs/screenshots/builder-details.png) · [Owned alternatives](docs/screenshots/alternatives.png) · [Wiki choices](docs/screenshots/wiki.png) · [Export](docs/screenshots/export.png) · [Compressed export](docs/screenshots/export-compressed.png) · [Diagnostics](docs/screenshots/diagnostics.png)

The gear-difference tooltip uses a compact count per container; individual slots keep their detailed explanations. [Tooltip preview](docs/screenshots/gear-differences-tooltip.png).

Settings previews below are generated from the current configuration definitions and default values using RuneLite styling. They are documentation previews, not captures of a running native configuration window.

<table>
<tr><th>Overview</th><th>Defaults and comparison</th><th>Bank, Wiki and editing</th></tr>
<tr><td><img src="docs/screenshots/settings.png" width="240" alt="Settings sections overview"></td><td><img src="docs/screenshots/settings-defaults.png" width="240" alt="Default loadouts, comparison and sidebar settings"></td><td><img src="docs/screenshots/settings-bank-wiki.png" width="240" alt="Hotkey, Bank, Wiki and editing settings"></td></tr>
</table>

## Getting started

Until Hub publication, build the development client using the instructions below. Enable **Loadout Studio** in RuneLite and click the Combat Tutor sword icon in the sidebar.

![Sidebar icon](src/main/resources/com/loadoutstudio/combat-tutor.png)

1. **Library:** click **New setup**, **Capture gear**, or import an existing setup.
2. **Builder:** name the setup and edit its items. Click **Save** to add a new draft to your library.
3. Saved setups open immediately. Subsequent changes autosave after a short pause; **Save** also saves immediately. **Setup details → Save copy** keeps a separate version.
4. Open the Bank and enable the setup's filter to gear up using the game's normal controls.

## Your setup library

Search saved setups by name, `item:<name or ID>` or `notes:<text>`. Click a setup's name to open it. Long names are shortened; hover to read the full name.

- **Favorites:** click the star, then use the Favorites filter.
- **Sections:** create or manage sections with the Library toolbar. You can also create a section from Builder's Setup details.
- **Move setups:** drag a setup name onto a section heading or another setup. Dropping on a setup moves it into that setup's section, without replacing the destination. Unfiled accepts moves too.
- **Copies:** duplicate imports and saved copies receive numbered names, such as `(1)` and `(2)`.
- **Archive:** the archive-box icon hides a setup from the active list. Enable Archived to restore it or permanently delete it with the trash icon. Permanent deletion asks for confirmation.
- **Sorting:** choose Alphabetical, Recent, Favorites first or Manual in RuneLite's Loadout Studio settings.

Libraries are separated by character and RuneLite profile. RuneLite configuration synchronisation follows your RuneLite settings. Use **Library tools → Copy library backup** before major changes; recovery is available if an edit cannot be saved normally.

## Editing a loadout

Builder shows Inventory, Rune Pouch, Equipment, Spellbook, Quiver, Additional filtered items and Notes. Item images come from the local game cache.

Click a question-mark slot or right-click **Update Slot from Search** to type in RuneLite's native chatbox item search. Right-click a slot to update from current gear, change its quantity or matching rule, or remove it. Hold Shift while right-clicking for supported **Update ALL slots** actions; review the replacement before applying it across setups.

**Find items** searches the local item catalogue. Drag an item into the desired slot or move/swap items between slots. Equipment, Pouch and Quiver slots check compatible item types. These actions edit the plan; they do not move your actual gear.

### Quantities and matching

- Enter a number or shorthand such as `15k`. Quantity input starts empty by default.
- **0** means unlimited matching: the item must be present, but the amount is ignored.
- Inventory stacks have no 16k limit. A real Rune Pouch holds at most **16,000 runes per type**.
- A planned Pouch amount such as `16001` is retained in Loadout Studio. Actual-pouch comparison and Inventory Setups export use a maximum of 16,000. Moving runes into the planned Pouch clears their source Inventory slot; it does not create a remainder stack.
- **Toggle Fuzzy** accepts supported related variants and displays a yellow star on the slot. Without Fuzzy, Slayer helmet variants remain exact. Exact chosen IDs are preserved in saved setups and exports.
- **Stack comparison** selects Ignore amount, Exactly, At least or At most.

The setup toolbar controls Bank filtering, unordered missing-item comparison, highlighting, highlight color and favorite status. Default settings apply when creating new blank or captured setups; they do not overwrite an existing setup's saved choices.

**Loadout options**, **Bank filtering and layout**, and **Setup details** hold less frequent actions. Notes grow with their text and use the sidebar's main scrollbar. Wiki Notes contain a short source/preset reference; detailed conditions remain on the source page.

## Finding Wiki setups

1. Open **Wiki**, enter an activity and press **Search Wiki** or Enter.
2. Select a result. A Strategies subpage is preferred when present; otherwise the main page is used. Technical pages are filtered out where recognised.
3. Choose one of the available setups. Source labels such as Melee, Magic and Ranged (crossbow) are retained.
4. Review items, quantities, Spellbook and source conditions, then save your own copy.

Typing alone uses local cached suggestions. It does not send live autocomplete requests. Known item alternatives start with the first source choice; a `?` requires a manual item choice. A recommendation-only page may not include an Inventory. Presets are starting points, not a guarantee that the gear is suitable for your account.

**URL / pasted wikitext** supports specific pages and offline pasted source text.

### Cache and privacy

Pages are cached automatically and reused after restart. The default freshness interval is seven days. An expired source refreshes only when you open it; **Refresh Wiki** explicitly requests an update. Cached data remains available when an update fails.

Starting the client, opening the sidebar, opening saved setups and typing do not trigger Wiki requests. Explicit searches and source selections can send search text and requested page titles to the OSRS Wiki, which also receives your IP address. Gear and saved setups are not uploaded. Item images and item lookup do not use the Wiki.

Requests are queued with one in flight, at least two seconds between starts, a ten-start rolling-minute limit, bounded pending work and server-error backoff. These limits apply per client. See the [Wiki request policy](docs/wiki-network-policy.md).

## Owned gear alternatives

Open the Bank at least once during the session. Loadout Studio observes local Bank, Inventory and Equipment data, including available Potion Storage information. Other storage is not included; closed-Bank observations can be stale.

**My gear alternatives** offers a small list of owned replacements in the selected Wiki setup's recommendation order. Where no source list exists, a limited bundled family may be offered. Requirements and source conditions still need review. Repeated identical food slots share one replacement choice.

Choose replacements and click **Use selected alternatives**. Items are not silently replaced by suggestions. Combined stock is checked before applying changes by default. Saved setups autosave the result; **Undo alternatives** is available until subsequent edits invalidate it. An owned Divine rune pouch can replace the planned ordinary Rune pouch while retaining its rune types and amounts.

Automatic local discovery, list length, expansion and stock checking can be configured. No alternatives means the block stays collapsed. No additional Wiki request is needed to check your gear.

## Bank filtering and layouts

With the Bank open, right-click **Show worn items**:

- **Loadout setups:** opens one of the available saved-setup shortcuts.
- **Loadout:** contains Preset, Zigzag top/bottom first, Filter Bank, Clear filter, Show bank and Return to loadout.

These are sibling menus. **Show bank** lets you browse your normal Bank temporarily; **Return to loadout** restores the selected setup. The saved filter preference controls returning when the Bank is reopened.

**Preset** places Equipment on the left and Inventory on the right, with Pouch runes below Equipment. **Zigzag** alternates between rows. Imported/custom layouts retain their recorded positions. Dragging in the active layout edits positions and switches to Custom; changes autosave for saved setups.

The view works with built-in Bank Tags enabled or disabled. Repeated owned-item positions provide native Withdraw actions against the actual Bank stack. Missing items are dim visual placeholders and cannot be withdrawn. Displayed Bank amounts are available stock, not requested setup quantities.

Potions use ordinary Bank stock first; available Potion Storage supplies the native selected dose when no matching Bank stock is present. Native Doses choices are supported. This does not automatically combine multiple sources in one withdrawal.

**Respect other Bank filters** is enabled by default. Selecting an unrelated tag can pause Loadout Studio. Use an explicit Filter Bank action to switch back. If Inventory Setups keeps reapplying its filter, close its setup or configure its Require Active Panel for Filtering option yourself. Loadout Studio does not change other plugins' settings.

## Comparing gear

Differences update from local gear/container changes. **Compare** requests a fresh check. The compact count tooltip summarises containers, while slot tooltips give item-specific details. Highlighting and missing-items-only matching are per-setup options.

Ammo quantity handling and replacement-stock checking are general plugin settings. Rune Pouch comparison respects actual capacity independently of planned amounts. Matching groups can differ in other plugins.

## Importing, exporting and backups

Use **Library → Import** to paste Inventory Setups JSON, native Bank Tag text, legacy Bank Tag Layouts text or Loadout Studio JSON. **From installed plugins** reads supported data from the current RuneLite profile and saves independent copies; originals remain unchanged.

Open **Export** and select:

| Format | Use |
| --- | --- |
| Inventory Setup JSON | Inventory Setups' green import icon → Import setup → Enter setup data |
| Bank Tag (RuneLite) | Bank's + tab → Import tag tab |
| Bank Tag Layouts | That plugin's compatible legacy import action |
| Loadout Studio JSON | Full-fidelity sharing and re-import in Loadout Studio |

**Bank Tags cannot retain worn-slot assignments, quantities, Spellbook or Notes.** Rune types can be included as Bank items, but their Pouch assignment/amounts are lost. Importing a tag puts items into Inventory, with overflow in Additional filtered items. Review conversion warnings and fill in missing details.

**Pretty Print** makes JSON easier to read. **Compress** is available only for Loadout Studio JSON and creates a `loadoutstudio:gz1:` code. External-plugin codes keep their expected plain format. Preview and Copy use the same selected representation.

Use **Library tools → Share setups / section** to share selected entries, or **Copy library backup** for a whole-library backup. Backup restore is a separate flow that asks before replacing your library. Review Notes and setup contents before sharing.

## Settings and support

Open RuneLite's plugin list and click Loadout Studio's configuration cog for defaults, comparison rules, library sorting, the Bank hotkey, alternatives, Wiki freshness, editing and diagnostics settings.

Report bugs or suggestions through [GitHub issues](https://github.com/Pa5cal1/runelite-loadout-studio/issues). Include the plugin/RuneLite version, expected behaviour and reproduction steps.

**Library tools → Diagnostics / report problem** shows a bounded session-only technical report. Inspect it before copying. Opening the issue form does not attach or send it. There is no automatic report upload or telemetry endpoint. Disable **Record local diagnostics** to clear and stop collection. Diagnostic reports exclude player names, setup contents, item IDs, searches and raw error text.

If you would like to support development, [buy me a coffee](https://buymeacoffee.com/pascalvdbe9). Donations are optional and unlock no features.

## Development and validation

Use JDK 11. From the repository root:

```powershell
.\gradlew.bat test jar shadowJar --no-daemon
.\scripts\Launch-Test.bat
```

On Linux/macOS: `sh gradlew test jar shadowJar --no-daemon`.

The regular JAR contains the plugin; the `*-all.jar` is the separate development client. For Jagex Accounts, follow RuneLite's [development-client login instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). The Hub uses the standard build; development/test launcher code is not part of its production plugin.

- [Current release evidence and remaining limits](docs/release-readiness.md)
- [Manual test scenarios and acceptance record](docs/local-acceptance.md)
- [Item identity compatibility](docs/item-id-compatibility.md)
- [Detailed validation history](docs/VALIDATION.md)
- [Plugin Hub submission](docs/plugin-hub/README.md)
- [Asset and license credits](docs/ASSETS.md)

## Latest changes

**0.9.1-rc.18:** drag a Library setup onto another setup to move it to that setup's section. Includes the earlier native Bank/potion interactions, autosave, exact/Fuzzy matching, supported cosmetic/max-cape variants, compact Wiki Notes and cache controls. The user confirmed the final section-drop gesture and food/potion withdrawal flows in the test client. See the validation history for earlier candidate changes.
