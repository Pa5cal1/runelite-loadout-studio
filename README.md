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
2. Enable **Loadout Studio (Test)** in plugin configuration. Open its OSRS Combat Tutor sword icon in the sidebar.
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

Version **0.6.2-test** provides a consistent native RuneLite sidebar with fixed navigation, a persistent Save area, compact Wiki results and collapsible details. Wiki requests are always available without a checkbox. The built-in **Bank Tags** plugin is a required dependency. See the [sidebar system design](docs/sidebar-system-design.md) for layout decisions and acceptance checks.

- **Library:** character-scoped storage in this plugin's own ConfigManager group, plus a separate guest library. Sections, search, favorites, archive/restore and complete JSON backup through the clipboard.
- **Builder:** a calm Inventory-first layout with Rune Pouch, Equipment, Spellbook, Quiver, Additional filtered items and Notes. Save and Find items remain above the scrolling loadout. Search, double-click or drag catalogue items into a setup. Move/swap compatible items between all five containers. Occasional editing and Bank controls are grouped in expandable panels. Ctrl+F opens search and Ctrl+S saves.
- **Quantities:** `16k` becomes `16000`. Only Rune pouch stacks are capped at 16000 per rune type; Inventory stacks can exceed 16k. Enter `0` for unlimited matching (the item is required, its quantity is ignored). Max pouch runes fills existing Rune pouch stacks. Ignore Ammo quantities affects comparison and exported Ammo matching.
- **Bank:** enable **Filter Bank** per setup. **Preset** places Equipment left and Inventory right; **Zigzag** alternates between two rows with a selectable starting row. **Imported / custom** preserves existing layouts. **Apply filter** activates it immediately or arms it for your next Bank opening; **Clear filter** turns Filter Bank off in the draft. Save to retain these options.
- **Capture:** explicitly reads current gear, Rune pouch, Quiver and Spellbook. It never equips, withdraws or performs game actions.
- **Compare gear:** applies Fuzzy item variations (including Inventory Setups extra groups), None/Standard/Less_Than/Greater_Than stack rules, unordered Inventory, ignored Ammo quantities and Spellbook checks. Unresolved quantities remain choices; zero explicitly ignores quantity. Red borders mark differences.
- **Wiki:** search content names, choose a result and optionally its Strategies subpage. Concrete setup choices load automatically in the sidebar. Select a named setup to open it in Builder. URL entry and pasted wikitext are under **URL / pasted wikitext**; **Use cache** reloads choices using the cache when available. Search covers Wiki article titles and is not restricted to a curated PvM catalogue. Redirect aliases such as Fractured archive strategies resolve to The Fractured Archive/Strategies without duplicating /Strategies.
- **Export:** Inventory Setups portable JSON, native RuneLite Bank Tags and legacy Bank Tag Layouts strings. Preview/copy the selected format. Import either format to convert it.

## Wiki behavior and privacy

Wiki networking is always available for this test build, as explicitly requested; there is no enable checkbox. Opening the plugin sends no Wiki requests. Typing alone sends no request. Press Enter or Search Wiki to request suggestions; selecting a result requests its setup source. There is no startup fetch, periodic refresh or Wiki image loading. Requests use a descriptive User-Agent, a single queue, at least two seconds between starts and at most ten starts per rolling minute. Identical pending requests are combined. The bounded Wiki cache is saved in this plugin's own data directory and reused after restart. Typing offers local cached suggestions without HTTP. Search results stay fresh for six hours; page sources are marked stale after seven days but still open from cache. Only **Refresh Wiki** replaces an already cached page. Cached source dates are shown. No startup cache scan or background refresh occurs; disk work runs on the Wiki worker. Maximum 64 entries / four million response characters, with additional serialized file limits. A cache write failure is shown without breaking this session. Rate-limit and service errors pause requests and respect Retry-After without automatic retry. See [Wiki networking](docs/wiki-network-policy.md). Searches and page fetches send your IP address, search text and requested page titles to `oldschool.runescape.wiki`; no gear, player name or library is uploaded. Requests are asynchronous, bounded in size and cancelled at shutdown. The obsolete wikiRequests setting is no longer read; existing saved libraries and setup settings are retained. Out-of-date search replies are ignored, and switching tabs or disposing the panel cancels scheduled suggestions. A precise Plugin Hub installation warning is prepared and Wiki information is available in a collapsed sidebar section. For Plugin Hub submission, include a plugin-level third-party-network warning in the hub manifest when this default-on feature is reviewed. Only this HTTPS Wiki host is allowed and HTTP redirects are rejected.

The parser reads concrete `Equipment`, `Inventory` and `Rune pouch` templates. When no concrete preset exists, ranked `Recommended equipment` tables use the first listed item by default, as requested. Ranks remain editable alternatives. Source conditions and combinations remain in Notes; Inventory and quantities still need review. Wiki aliases are resolved by the MediaWiki API before the optional Strategies subpage is selected. A missing Strategies subpage falls back to the canonical article. The resolved page title is used for the setup name and source URL. Missing articles produce a page-not-found message; an HTTP failure reports its status without suggesting that every 404 is a redirect failure. Pasted wikitext works without networking.

The local game item catalogue becomes available after game data loads. Wiki alternatives default to the first listed item. Equal-name game variants default to the first local catalogue result (stable ascending Item ID); explicit IDs already in a setup are retained. Empty Wiki slots remain empty. Equipment aliases (`torso`, `gloves`, `boots`) are recognized; tabber names identify variants, and a missing Rune pouch is not borrowed from the next variant. Missing rune or Ammo quantities require review before export. Unresolved drafts can still be saved.

## Importing into other plugins

- Inventory Setups: green import icon → **Import setup…** → **Enter setup data**. Its **+** button creates a setup from current gear and accepts a name, not JSON.
- RuneLite Bank Tags: right-click the Bank **+** tab → **Import tag tab**.
- Bank Tag Layouts: use its matching import action for legacy strings.

Bank Tags retain item membership and optional Bank positions. They do not encode worn slots, quantities, Spellbook, Notes or Rune pouch contents. Converting one creates a draft with items in Inventory and overflow in Additional filtered items. Supply missing details manually; conversion through Bank Tags is not lossless.

## Manual acceptance test

1. Enable the plugin, log in manually, capture gear, change its name and save under a section using Setup details. Restart and reopen it; check Favorites and archive/restore.
2. Switch character or RuneLite profile; confirm libraries remain separate. Guest drafts do not automatically move to a character library.
3. Edit an Inventory item; swap slots; move arrows into Quiver. Try a non-rune or more than `16k` in Rune pouch: both should be rejected. Inventory runes such as `200k` must be accepted; `0` must display unlimited and export with quantity comparison disabled.
4. Open Wiki and type a boss name, then press Enter or Search Wiki and check the suggestions, select its page to load the setup choices automatically. For Vorkath, check Ranged (crossbow), Ranged (blowpipe) and Melee, and open each in Builder. Quickly change and clear the query; old results must not replace the newest search. Check concrete presets and unresolved variants. Also try pasted wikitext under URL / pasted wikitext.
5. Import a known Inventory Setup and export it to the receiving plugin's actual import action. Check name, gear, quantities, Notes and layout. Test native Bank Tag and legacy layout separately. The library stays in Loadout Studio configuration. The built-in Bank Tags service controls the active Bank view; existing user tags are retained.
6. With the Bank open, expand Bank filtering and layout, then enable Filter Bank and switch Preset/Zigzag in both directions. Check Equipment, Inventory, Rune pouch, Quiver and Additional filtered items. Save/reopen the setup and confirm its choices persist. Close/reopen the Bank to check automatic activation.
7. Select a normal Bank Tag, then use Clear filter in Loadout Studio: the normal tag must stay open. Check cleanup when disabling the plugin, logging out and changing profiles. Unresolved Item IDs must block filtering. If Inventory Setups is also enabled, keep only one setup filter selected while testing.
8. Back up the library, add a temporary setup, then restore after reviewing the replacement prompt.

Automated checks validate serialization, limits, quantities, template columns, backups, Wiki host restrictions, layout positions, filter membership, delayed Bank opening, owned-tag cleanup and saved layout options. They do not verify behavior inside the game. No game input was automated.

The exact Plugin Hub installation warning is prepared in [the submission manifest example](docs/plugin-hub/loadout-studio.example). See [submission notes](docs/plugin-hub/README.md) before using it.

## Before public release

Automatic interpretation of complex Wiki conditions and authenticated cross-device storage remain outside this test version. This test uses RuneLite configuration and has no separate web account system. ConfigManager may synchronize according to RuneLite settings.

Do not commit `test-home`, build outputs, sessions or credentials. After manual acceptance, review current [Plugin Hub requirements](https://github.com/runelite/plugin-hub) and submit a source commit for review. A GitHub repository is separate from Plugin Hub submission.

## Bank implementation notes

Filtering uses RuneLite's Bank Tags API and an in-memory, uniquely named custom tag. Display layouts are passed to the native layout engine; Bank quantities and withdrawal clicks remain handled by RuneLite. Native layout rendering may create a temporary layout under this unique tag, which is removed when the filter is cleared or the plugin stops. Existing user-created tags are not edited. Native Bank Tags keeps its normal active-tab state. There are no automatic withdrawals or equipment actions.

Bank matching canonicalizes noted items/placeholders and honors each item's Fuzzy flag via RuneLite's variation mapping. Amounts do not restrict Bank membership. Missing rune quantities therefore do not block filtering, while unresolved Item IDs do. Core Bank Tags handles missing items and additional matching variants. Gear comparison applies the saved Fuzzy, Stack comparison and unordered highlighting choices. Bank Tag receivers use their own variation rules; retaining a Fuzzy flag does not guarantee identical custom groups across different plugins.

Changing layout mode rebuilds the exported Bank positions. Merely opening an imported setup preserves its original layout; edits in Imported / custom mode keep surviving positions and append new items. Layout choices and Zigzag direction are stored per library entry, and Filter Bank is the portable setup's `fb` field. Older library backups remain readable. Bank Tag strings cannot carry these preferences, but retain the generated positions.

Layout behavior was checked against the [Inventory Setups layout utilities](https://github.com/dillydill123/inventory-setups/blob/master/src/main/java/inventorysetups/InventorySetupLayoutUtilities.java) and [RuneLite Bank Tags API](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/banktags/BankTagsService.java).

### Fast item editing

Open Builder > Find items to search the local game catalogue by English name or Item ID. Double-click a result or choose Add to Inventory to fill the next empty slot. Drag a result onto a slot to copy it there. Drag between loadout slots to move or swap; Equipment, Rune Pouch and Quiver validate compatible contents. Additional filtered items can also be moved. A full Inventory is never overwritten by Add to Inventory.

Ctrl+F focuses the relevant search field; Ctrl+S saves the current draft. Dragging affects this plugin's draft only. It does not withdraw, equip or move game items. Native dragging and actual sprites still require manual acceptance in the test client.

## Native slot controls

Right-click a slot to replace it from RuneLite chatbox search or the corresponding current container, change quantity, toggle Fuzzy/Stack comparison or remove it. Search uses RuneLite's chatbox, not a separate Choose item window. Notes start at ten rows and grow without an inner scrollbar. Item counts are painted inside each slot. Bank filtering shows the readable loadout name while retaining a unique internal tag. The sword asset is bundled locally; see [asset credits](docs/ASSETS.md).

## Existing RuneLite setups and recovery — 0.6.2-test

- Library → Import icon → **From installed plugins**, or Library tools → **From installed plugins**, reads saved data in the current RuneLite profile only after you click. Search/select entries, inspect the preview, then **Save copies**. **Open copy** opens a separate editable draft. No original setup, section or Bank Tag is changed.
- Modern Inventory Setups V3 and V2 serialized setups are supported. Save copies retains names, slots, quantities, matching flags, notes, available layouts, favorites and section names. Identical portable copies are skipped; bulk copies are validated atomically. V1 data should first be migrated/exported through Inventory Setups. Corrupt/unsupported entries produce warnings; portable JSON import remains available.
- Native Bank Tags and legacy Bank Tag Layouts are supported. Item membership, negative-ID variation flags and available positions are read. Bank Tags cannot reconstruct worn gear, amounts, Rune Pouch contents or Spellbook. The import preview and export notice explain this before use. The receiving plugin's actual import action remains necessary.
- Dirty Builder changes have a separate recovery record under Loadout Studio's own character/profile configuration. **Restore draft** appears in Library after reopening; restoring never silently replaces a saved entry. Save clears the recovery. Discard recovery is explicit. RuneLite may synchronize configuration according to the user's existing RuneLite settings; Loadout Studio has no account/backend service.
- Unresolved Wiki slots are grouped in a **choices needed** guide above the loadout. It opens native chatbox search or quantity input for the next unresolved slot. Unknown quantities start blank; they are not converted to zero/unlimited or 16k by default.

Before broad distribution, complete the [manual acceptance matrix](docs/local-acceptance.md). Offline tests do not prove operation in a logged-in game client.

### Item images and unresolved Wiki choices

Version 0.6.2-test repaints asynchronously loaded RuneLite sprites as soon as they arrive. Item images use the local game cache, never Wiki image requests. Known Wiki choices use their first item automatically, including unresolved choices in older saved copies when opened. A `?` with a source label and “choose an exact Item ID” remains only when no supported item name can be resolved. Use the choices guide or right-click **Update Slot from Search** to choose the exact variant in RuneLite's chatbox. Imported concrete positive Item IDs use the sprite path directly. Existing JVMs need a manual restart to run a newer JAR.

### Default Wiki items — 0.6.2-test

The first listed recommendation is selected automatically. For a combination, its first component fills the slot; the complete combination and source conditions remain in Notes. Same-name variants use the first local catalogue result in stable Item ID order. This is a user-selected default, not a claim that every variant is equivalent. Existing positive Item IDs and quantities are preserved. Older unresolved saved copies are resolved locally when opened and only persist if saved.

Bundled first-option mappings: `Cheap food` → Manta ray; `Cheap prayer` → Prayer potion(4). These are fixed first-listed defaults rather than a live cheapest-price calculation. Their template definitions were checked once during development; the plugin adds no template, price or image requests. Unsupported source expressions still require an item choice. Missing quantities are retained as unknown.

### Archive and permanent deletion — 0.6.2-test

The box icon archives an active setup. In the Archived view, the curved arrow restores it and the red trash icon permanently deletes it after confirmation. Cancel is the default action. Deletion only affects the selected archived setup; sections and other setups remain. If that setup is open, the confirmation also explains that its Builder draft and unsaved changes will be discarded. Failed storage leaves it intact, and switching profile during confirmation cancels the action.
