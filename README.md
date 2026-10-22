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
2. Enable **Loadout Studio (Test)** in plugin configuration. Open its grid icon in the sidebar.
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

Version **0.2.0-test** adds live Bank filtering and saved Preset/Zigzag options. The built-in **Bank Tags** plugin is a required dependency.

- **Library:** character-scoped storage in this plugin's own ConfigManager group, plus a separate guest library. Sections, search, favorites, archive/restore and complete JSON backup through the clipboard.
- **Builder:** Equipment, 28 Inventory slots, Rune pouch, extra Quiver Ammo, Additional filtered items, Notes and Spellbook. Click slots to search items or enter an exact Item ID. Drag between Equipment, Inventory, Rune pouch and Quiver. Additional filtered items use click editing.
- **Quantities:** `16k` becomes `16000`; Rune pouch stacks cannot exceed 16000. Max runes fills existing Rune pouch stacks. Ignore Ammo quantities affects comparison and exported Ammo matching.
- **Bank:** enable **Filter Bank** per setup. **Preset** places Equipment left and Inventory right; **Zigzag** alternates between two rows with a selectable starting row. **Imported / custom** preserves existing layouts. **Apply filter** activates it immediately or arms it for your next Bank opening; **Clear filter** turns Filter Bank off in the draft. Save to retain these options.
- **Capture:** explicitly reads current gear, Rune pouch, Quiver and Spellbook. It never equips, withdraws or performs game actions.
- **Compare gear:** red borders mark exact slot/quantity differences. Fuzzy and Stack comparison flags are preserved for export but are not evaluated by this prototype comparison.
- **Wiki:** search content names, choose a result and optionally its Strategies subpage. Fetch it or paste wikitext offline. Search covers Wiki article titles and is not restricted to a curated PvM catalogue.
- **Export:** Inventory Setups portable JSON, native RuneLite Bank Tags and legacy Bank Tag Layouts strings. Preview/copy the selected format. Import either format to convert it.

## Wiki behavior and privacy

Wiki networking is disabled by default. Enabling it sends your IP address, search text and requested page titles to `oldschool.runescape.wiki`; no gear, player name or library is uploaded. Requests are asynchronous, bounded in size and cancelled at shutdown. Only this HTTPS Wiki host is allowed and HTTP redirects are rejected.

The parser reads concrete `Equipment`, `Inventory` and `Rune pouch` templates. Ranked `Recommended equipment` tables contain alternatives and are not converted in this test version. Some pages have no Strategies subpage: uncheck **Use Strategies subpage** and try the article itself. Pasted wikitext works without networking.

The local game item catalogue becomes available after game data loads. Exact unique names resolve automatically; ambiguous variants remain `?` and require an Item ID choice. Missing rune or Ammo quantities require review before export. Unresolved drafts can still be saved.

## Importing into other plugins

- Inventory Setups: green import icon → **Import setup…** → **Enter setup data**. Its **+** button creates a setup from current gear and accepts a name, not JSON.
- RuneLite Bank Tags: right-click the Bank **+** tab → **Import tag tab**.
- Bank Tag Layouts: use its matching import action for legacy strings.

Bank Tags retain item membership and optional Bank positions. They do not encode worn slots, quantities, Spellbook, Notes or Rune pouch contents. Converting one creates a draft with items in Inventory and overflow in Additional filtered items. Supply missing details manually; conversion through Bank Tags is not lossless.

## Manual acceptance test

1. Enable the plugin, log in manually, capture gear, change its name and save under a section. Restart and reopen it; check Favorites and archive/restore.
2. Switch character or RuneLite profile; confirm libraries remain separate. Guest drafts do not automatically move to a character library.
3. Edit an Inventory item; swap slots; move arrows into Quiver. Try a non-rune in Rune pouch and more than `16k` runes: both should be rejected.
4. Enable Wiki requests; search for a boss, select its page and fetch. Check concrete presets and unresolved variants. Also try pasted wikitext with networking disabled.
5. Import a known Inventory Setup and export it to the receiving plugin's actual import action. Check name, gear, quantities, Notes and layout. Test native Bank Tag and legacy layout separately. The library stays in Loadout Studio configuration. The built-in Bank Tags service controls the active Bank view; existing user tags are retained.
6. With the Bank open, enable Filter Bank and switch Preset/Zigzag in both directions. Check Equipment, Inventory, Rune pouch, Quiver and Additional filtered items. Save/reopen the setup and confirm its choices persist. Close/reopen the Bank to check automatic activation.
7. Select a normal Bank Tag, then use Clear filter in Loadout Studio: the normal tag must stay open. Check cleanup when disabling the plugin, logging out and changing profiles. Unresolved Item IDs must block filtering. If Inventory Setups is also enabled, keep only one setup filter selected while testing.
8. Back up the library, add a temporary setup, then restore after reviewing the replacement prompt.

Automated checks validate serialization, limits, quantities, template columns, backups, Wiki host restrictions, layout positions, filter membership, delayed Bank opening, owned-tag cleanup and saved layout options. They do not verify behavior inside the game. No game input was automated.

## Before public release

Full fuzzy gear comparison, ranked recommendation selection and authenticated cross-device storage are future work. This test uses RuneLite configuration and has no separate web account system. ConfigManager may synchronize according to RuneLite settings.

Publish to an empty source repository after reviewing the diff. Do not commit `test-home`, build outputs, sessions or credentials. After manual acceptance, review current [Plugin Hub requirements](https://github.com/runelite/plugin-hub) and submit a source commit for review. A GitHub repository is separate from Plugin Hub submission.

## Bank implementation notes

Filtering uses RuneLite's Bank Tags API and an in-memory, uniquely named custom tag. Display layouts are passed to the native layout engine; Bank quantities and withdrawal clicks remain handled by RuneLite. Native layout rendering may create a temporary layout under this unique tag, which is removed when the filter is cleared or the plugin stops. Existing user-created tags are not edited. Native Bank Tags keeps its normal active-tab state. There are no automatic withdrawals or equipment actions.

Bank matching canonicalizes noted items/placeholders and honors each item's Fuzzy flag via RuneLite's variation mapping. Amounts do not restrict Bank membership. Missing rune quantities therefore do not block filtering, while unresolved Item IDs do. Core Bank Tags handles missing items and additional matching variants. Gear comparison in the Builder still uses exact IDs/quantities.

Changing layout mode rebuilds the exported Bank positions. Merely opening an imported setup preserves its original layout; edits in Imported / custom mode keep surviving positions and append new items. Layout choices and Zigzag direction are stored per library entry, and Filter Bank is the portable setup's `fb` field. Older library backups remain readable. Bank Tag strings cannot carry these preferences, but retain the generated positions.

Layout behavior was checked against the [Inventory Setups layout utilities](https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/InventorySetupLayoutUtilities.java) and [RuneLite Bank Tags API](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/banktags/BankTagsService.java).
