# Loadout Studio — local RuneLite test plugin

Native Java 11 sidebar prototype for creating, capturing and organizing loadouts.
This is a development test build, not an approved Plugin Hub release.

Source repository: [Pa5cal1/runelite-loadout-studio](https://github.com/Pa5cal1/runelite-loadout-studio) (private test repository). GitHub publication is separate from Plugin Hub review.

## Repository structure

| Path | Contents |
| --- | --- |
| `src/main/java/com/loadoutstudio/` | RuneLite plugin source code |
| `src/test/` | Automated tests, test launcher and test resources |
| `docs/` | [Validation results and limitations](docs/VALIDATION.md) |
| `scripts/` | Windows helper to launch the built test client |
| `.github/workflows/` | GitHub Actions checks |
| `gradle/wrapper/` | Gradle wrapper runtime and configuration |

Build configuration, Gradle entry points, plugin metadata, license and agent guidance remain in the repository root. Run build commands from that root; the Windows launcher locates it relative to its own location.

## Start the local test

1. Run `scripts/Launch-Test.bat` after building. It uses RuneLite's installed Java runtime when available.
2. Enable **Loadout Studio (Test)** in plugin configuration. Open its gold and turquoise Inventory backpack icon in the sidebar.
3. The development launcher uses `test-home/.runelite`, separate from your normal RuneLite data. It does not migrate saved setups or login sessions.
4. For Jagex Accounts, follow the official [development-client instructions](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). Use the official authentication flow.

## Build

Use JDK 11 or newer and the included Gradle wrapper:

```powershell
.\gradlew.bat test shadowJar jar --no-daemon
.\scripts\Launch-Test.bat
```

Alternatively, with Docker Engine running, from the project directory:

```powershell
docker run --rm -v "${PWD}:/project" -w /project eclipse-temurin:11-jdk sh gradlew test shadowJar jar --no-daemon
```

The `*-all.jar` is the development client; the small regular JAR contains only this plugin. Do not put the development client JAR in the normal RuneLite plugins directory. `latest.release` follows RuneLite updates: rebuild if the client or cache changes. GitHub Actions runs tests on Java 11.

## Features

Version **0.4.0-test** provides a consistent native RuneLite sidebar with fixed navigation, a persistent Save area, compact Wiki results and collapsible details. Wiki requests are always available without a checkbox. The built-in **Bank Tags** plugin is a required dependency. See the [sidebar system design](docs/sidebar-system-design.md) for layout decisions and acceptance checks.

- **Library:** character-scoped storage in this plugin's own ConfigManager group, plus a separate guest library. Sections, search, favorites, archive/restore and complete JSON backup through the clipboard.
- **Builder:** a calm Inventory-first layout with Rune Pouch, Equipment, Spellbook, Quiver, Additional filtered items and Notes. Save and Find items remain above the scrolling loadout. Search, double-click or drag catalogue items into a setup. Move/swap compatible items between all five containers. Occasional editing and Bank controls are grouped in expandable panels. Ctrl+F opens search and Ctrl+S saves.
- **Quantities:** `16k` becomes `16000`; Rune pouch stacks cannot exceed 16000. Max runes fills existing Rune pouch stacks. Ignore Ammo quantities affects comparison and exported Ammo matching.
- **Bank:** enable **Filter Bank** per setup. **Preset** places Equipment left and Inventory right; **Zigzag** alternates between two rows with a selectable starting row. **Imported / custom** preserves existing layouts. **Apply filter** activates it immediately or arms it for your next Bank opening; **Clear filter** turns Filter Bank off in the draft. Save to retain these options.
- **Capture:** explicitly reads current gear, Rune pouch, Quiver and Spellbook. It never equips, withdraws or performs game actions.
- **Compare gear:** red borders mark exact slot/quantity differences. Fuzzy and Stack comparison flags are preserved for export but are not evaluated by this prototype comparison.
- **Wiki:** search content names, choose a result and optionally its Strategies subpage. Concrete setup choices load automatically in the sidebar. Select a named setup to open it in Builder. URL entry and pasted wikitext are under **URL / pasted wikitext**; **Reload setups** retries a request. Search covers Wiki article titles and is not restricted to a curated PvM catalogue. Redirect aliases such as Fractured archive strategies resolve to The Fractured Archive/Strategies without duplicating /Strategies.
- **Export:** Inventory Setups portable JSON, native RuneLite Bank Tags and legacy Bank Tag Layouts strings. Preview/copy the selected format. Import either format to convert it.

## Wiki behavior and privacy

Wiki networking is always available for this test build, as explicitly requested; there is no enable checkbox. Opening the plugin sends no Wiki requests. Typing at least two characters in Wiki search requests suggestions after a 350 ms pause. Searches and page fetches send your IP address, search text and requested page titles to `oldschool.runescape.wiki`; no gear, player name or library is uploaded. Requests are asynchronous, bounded in size and cancelled at shutdown. The obsolete wikiRequests setting is no longer read; existing saved libraries and setup settings are retained. Out-of-date search replies are ignored, and switching tabs or disposing the panel cancels scheduled suggestions. A precise Plugin Hub installation warning is prepared and Wiki information is available in a collapsed sidebar section. For Plugin Hub submission, include a plugin-level third-party-network warning in the hub manifest when this default-on feature is reviewed. Only this HTTPS Wiki host is allowed and HTTP redirects are rejected.

The parser reads concrete `Equipment`, `Inventory` and `Rune pouch` templates. Ranked `Recommended equipment` tables contain alternatives and are not converted in this test version. Wiki aliases are resolved by the MediaWiki API before the optional Strategies subpage is selected. A missing Strategies subpage falls back to the canonical article. The resolved page title is used for the setup name and source URL. Missing articles produce a page-not-found message; an HTTP failure reports its status without suggesting that every 404 is a redirect failure. Pasted wikitext works without networking.

The local game item catalogue becomes available after game data loads. Exact unique names resolve automatically; ambiguous variants remain `?` and require an Item ID choice. Empty Wiki slots remain empty. Equipment aliases (`torso`, `gloves`, `boots`) are recognized; tabber names identify variants, and a missing Rune pouch is not borrowed from the next variant. Missing rune or Ammo quantities require review before export. Unresolved drafts can still be saved.

## Importing into other plugins

- Inventory Setups: green import icon → **Import setup…** → **Enter setup data**. Its **+** button creates a setup from current gear and accepts a name, not JSON.
- RuneLite Bank Tags: right-click the Bank **+** tab → **Import tag tab**.
- Bank Tag Layouts: use its matching import action for legacy strings.

Bank Tags retain item membership and optional Bank positions. They do not encode worn slots, quantities, Spellbook, Notes or Rune pouch contents. Converting one creates a draft with items in Inventory and overflow in Additional filtered items. Supply missing details manually; conversion through Bank Tags is not lossless.

## Manual acceptance test

1. Enable the plugin, log in manually, capture gear, change its name and save under a section using Setup details. Restart and reopen it; check Favorites and archive/restore.
2. Switch character or RuneLite profile; confirm libraries remain separate. Guest drafts do not automatically move to a character library.
3. Edit an Inventory item; swap slots; move arrows into Quiver. Try a non-rune in Rune pouch and more than `16k` runes: both should be rejected.
4. Open Wiki and type a boss name and check the automatic suggestions, select its page to load the setup choices automatically. For Vorkath, check Ranged (crossbow), Ranged (blowpipe) and Melee, and open each in Builder. Quickly change and clear the query; old results must not replace the newest search. Check concrete presets and unresolved variants. Also try pasted wikitext under URL / pasted wikitext.
5. Import a known Inventory Setup and export it to the receiving plugin's actual import action. Check name, gear, quantities, Notes and layout. Test native Bank Tag and legacy layout separately. The library stays in Loadout Studio configuration. The built-in Bank Tags service controls the active Bank view; existing user tags are retained.
6. With the Bank open, expand Bank filtering and layout, then enable Filter Bank and switch Preset/Zigzag in both directions. Check Equipment, Inventory, Rune pouch, Quiver and Additional filtered items. Save/reopen the setup and confirm its choices persist. Close/reopen the Bank to check automatic activation.
7. Select a normal Bank Tag, then use Clear filter in Loadout Studio: the normal tag must stay open. Check cleanup when disabling the plugin, logging out and changing profiles. Unresolved Item IDs must block filtering. If Inventory Setups is also enabled, keep only one setup filter selected while testing.
8. Back up the library, add a temporary setup, then restore after reviewing the replacement prompt.

Automated checks validate serialization, limits, quantities, template columns, backups, Wiki host restrictions, layout positions, filter membership, delayed Bank opening, owned-tag cleanup and saved layout options. They do not verify behavior inside the game. No game input was automated.

The exact Plugin Hub installation warning is prepared in [the submission manifest example](docs/plugin-hub/loadout-studio.example). See [submission notes](docs/plugin-hub/README.md) before using it.

## Before public release

Full fuzzy gear comparison, ranked recommendation selection and authenticated cross-device storage are future work. This test uses RuneLite configuration and has no separate web account system. ConfigManager may synchronize according to RuneLite settings.

Publish to an empty source repository after reviewing the diff. Do not commit `test-home`, build outputs, sessions or credentials. After manual acceptance, review current [Plugin Hub requirements](https://github.com/runelite/plugin-hub) and submit a source commit for review. A GitHub repository is separate from Plugin Hub submission.

## Bank implementation notes

Filtering uses RuneLite's Bank Tags API and an in-memory, uniquely named custom tag. Display layouts are passed to the native layout engine; Bank quantities and withdrawal clicks remain handled by RuneLite. Native layout rendering may create a temporary layout under this unique tag, which is removed when the filter is cleared or the plugin stops. Existing user-created tags are not edited. Native Bank Tags keeps its normal active-tab state. There are no automatic withdrawals or equipment actions.

Bank matching canonicalizes noted items/placeholders and honors each item's Fuzzy flag via RuneLite's variation mapping. Amounts do not restrict Bank membership. Missing rune quantities therefore do not block filtering, while unresolved Item IDs do. Core Bank Tags handles missing items and additional matching variants. Gear comparison in the Builder still uses exact IDs/quantities.

Changing layout mode rebuilds the exported Bank positions. Merely opening an imported setup preserves its original layout; edits in Imported / custom mode keep surviving positions and append new items. Layout choices and Zigzag direction are stored per library entry, and Filter Bank is the portable setup's `fb` field. Older library backups remain readable. Bank Tag strings cannot carry these preferences, but retain the generated positions.

Layout behavior was checked against the [Inventory Setups layout utilities](https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/InventorySetupLayoutUtilities.java) and [RuneLite Bank Tags API](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/banktags/BankTagsService.java).

### Fast item editing

Open Builder > Find items to search the local game catalogue by English name or Item ID. Double-click a result or choose Add to Inventory to fill the next empty slot. Drag a result onto a slot to copy it there. Drag between loadout slots to move or swap; Equipment, Rune Pouch and Quiver validate compatible contents. Additional filtered items can also be moved. A full Inventory is never overwritten by Add to Inventory.

Ctrl+F focuses the relevant search field; Ctrl+S saves the current draft. Dragging affects this plugin's draft only. It does not withdraw, equip or move game items. Native dragging and actual sprites still require manual acceptance in the test client.
