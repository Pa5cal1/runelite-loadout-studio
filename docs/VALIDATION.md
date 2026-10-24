# Current validation — 0.8.2-test

- Native Java 11 test/jar/shadowJar: 167 tests, zero failures/errors/skipped.
- Inventory-to-Pouch drops still cap at 16000, but excess no longer creates an Inventory remainder. A different displaced rune swaps into the freed source slot, including with full Inventory. An oversized same-rune transfer clears the source without producing another stack.
- Regression coverage includes 16001, Integer.MAX_VALUE, occupied destinations, full Inventory, invalid items, normal swaps and stale drags. This changes setup planning only; no live game input is performed.
- Manually test the changed transfer in the test client before accepting this build. The client is not automatically restarted.

# Previous validation — 0.8.1-test

- Native Java 11 test/jar/shadowJar: 166 tests, zero failures/errors/skipped. Six new regressions cover concrete loadouts retaining ranked recommendations, tab and table-column isolation, Bank-order-independent source ordering, excluding unlisted family gear, preserving the concrete selection, and conditions/Inventory/Pouch retention.
- An additional local-only probe parsed the complete user-supplied Aquanites source successfully: two variants, seven ordered melee body choices and five ranged body choices, without HTTP. This probe is not part of the shipped source or production JAR.
- Source recommendations take precedence over bundled family fallbacks. Only observed and slot-compatible choices are offered; user application and existing stock checks remain explicit. Conditions stay in Notes and are not automatically evaluated.
- Reopen Aquanites from the existing cached source to test both variants. Older saved setups are not silently rewritten; open a fresh Wiki draft and Save a copy to acquire the recommendation lists. Confirm actual Bank observations and item IDs in the test client.

# Previous validation — 0.8.0-test

- Native Windows Java 11 test, jar and shadowJar passed: **160 tests, zero failures/errors/skipped**. The twenty additional regression tests cover live ring identity selection and legacy draft repair, nested template/dose resolution, meaningful variants, activity suggestions, automatic persistent cache refresh/fallback, local ordered alternatives, Notes preservation, numbered copies, partial/fuzzy Bank mapping, standalone Bank geometry, setup controls, blank quantity input, drag sprites and occupied Pouch transfers.
- Offline previews at 220/260 px were inspected for Library, Builder, Wiki, Export and compact alternatives. Main previews use RuneLiteLAF with a fake host. Disclosure headings, ordinary actions and Wiki presets differ visibly. They are not live game tests.
- Bank Tags is now optional. The independent ordinary-Bank path preserves real widget IDs, amounts and child indices; script callbacks and pure geometry have regression coverage. Correct native withdrawals, Bank service transitions and listener restoration remain manual acceptance items.
- A source refresh occurs only when the user opens an expired source (default seven days), or confirms Refresh Wiki. No startup request, per-item HTTP lookup, background refresh or image request was added.
- Production JAR targets class version 55 and contains no test classes or third-party Hub plugin classes. Versioned artifacts are copied to build/libs with matching hashes. Users need no Docker.
- Pending live acceptance: Windows drag image, direct native chat focus, Pouch/Quiver transfers, canonical item sprites/IDs, unresolved partial filters, Bank Tags disabled/enabled transitions and manual withdrawals, highlighting options, import sizing and game-window resizing. See local-acceptance.md. No Plugin Hub submission or visibility change performed.

# Previous validation — 0.7.3-test

- Native Windows Java 11 test, jar and shadowJar passed: 140 tests, zero failures/errors/skipped. Nineteen added tests cover strict boolean/schema handling, favorite add/remove/save/copy/recovery and persistence failure, inline export feedback, portable amount-rule codes, aggregated replacement stock and its explicit config override, and native RuneLite theme layouts.
- The two defects reproduced during the 0.7.2 review now have permanent regression coverage. Missing optional flags remain accepted; present non-boolean flags are rejected before entering the editor. Stored metadata is checked before Gson can coerce it; recovery flags are checked before restoration.
- Replacement checks reserve amounts for all Inventory/Equipment slots sharing selected replacement IDs. Totals use long arithmetic, explicit zero reserves at least one item per slot, other storage is excluded, and failure leaves the draft intact. Single-item observation remains required even when combined stock checks are disabled in config.
- Reviewed 220/260 px offline layouts with RuneLiteLAF, plus local feedback previews. These are fake-host tests without game input or actual live sprites. They do not replace the pending user tests in local-acceptance.md.
- Versioned local artifacts are copied to build/libs with matching hashes. No Docker runtime is required. No Plugin Hub submission or repository visibility change is performed.

Earlier entries below record development history.

# Local validation — 0.2.0-test — 2026-10-22

- Docker Java 11: `sh gradlew test shadowJar jar --no-daemon --offline` succeeded.
- JUnit: 27 tests, zero failures/errors. Covers portable setup round-trip, native and legacy Bank Tag conversion, quantities and limits, malformed URLs, Wiki search JSON, multiline Wiki template pairing, library backups and section deletion, Bank Preset/Zigzag positions in both directions, exact/Fuzzy filter membership, delayed activation, owned-tag cleanup, size limits, saved preferences, and native Swing controls/profile reset with a fake host.
- Live public OSRS Wiki OpenSearch request for `Vorkath`: HTTP 200; returned Vorkath and Vorkath/Strategies among the article titles. This checks endpoint accessibility outside RuneLite; it does not prove the in-client network path.
- Plugin-only JAR class version 55 (Java 11); 24 classes; no Plugin service loader registration. Static source scan found no subprocess execution, reflection, native memory access, dynamic code loading or alternate HTTP stack.
- Narrow sidebar was rendered without a game client and inspected for Equipment/Inventory columns, search controls and scroll behavior. These screenshots use the test environment's Swing theme, not a running RuneLite client.
- `git diff --check` passed. Build outputs and test-home are ignored. Source repository: https://github.com/Pa5cal1/runelite-loadout-studio (private). No Plugin Hub submission performed.

Pending manual acceptance: actual startup/login, item catalogue and sprites, capturing Rune pouch/Quiver/Spellbook values, character/profile persistence, receiving-plugin imports, Wiki requests from inside RuneLite, and live Bank filtering/layout rendering. Test automatic filtering on Bank opening, both layouts/directions, Clear filter, preserving a normal Bank Tag, and cleanup at logout/profile switch/disable. The development launcher isolates its data in test-home. Follow the acceptance sequence in [README.md](../README.md).

Bank filtering now uses the built-in Bank Tags dependency, an in-memory custom tag, and the native layout engine. Layout choices are saved per setup; generated positions are exported. Existing user tags are not edited. This remains a prototype: gear comparison does not yet implement full fuzzy rules, and ranked Wiki recommendation tables still require a choice editor.

## Sidebar navigation update — 0.2.1-test — 2026-10-22

- Enlarged navigation to 13 pt bold sans-serif labels with a 28 px label height and wrapping tabs for the narrow sidebar.
- Existing Java 11 checks: 27 tests passed, no failures or errors. Builder and Wiki previews rendered without game interaction.
- The running 0.2.0-test client was left open at the user's request. The larger tabs still require manual confirmation in RuneLite after starting the new build.

## Sidebar icon update — 0.2.2-test — 2026-10-22

- Replaced the generic grid with a transparent 32 px pixel-art leather Inventory pack and Equipment sword.
- Rendered the same Java drawing code at 24 and 32 px on a dark background for visual review. No game interaction or client restart.

## Wiki search update — 0.2.3-test — 2026-10-22

- Existing Wiki networking setting defaults to enabled at the user's explicit request; previously saved choices are honored. Its config warning and the inline networking notice remain.
- Suggestions start at two characters after a 350 ms debounce. Enter/Search performs an immediate search. Request revisions reject out-of-order replies, including returning to the same query; disposal, scope reload and leaving Wiki invalidate scheduled searches.
- Java 11 automated checks: 28 tests, zero failures/errors, including debounce, response ordering, minimum query length, disabling and disposal. Fake-host tests make no Wiki requests.
- Live public OpenSearch request for `Vork` returned Vorkath, Vorkath/Strategies and Vorki. This verifies the endpoint outside RuneLite; in-client behavior still needs manual acceptance.
- No game input or client restart was automated. Public Plugin Hub submission must include the corresponding third-party-network warning for review.

## Wiki page choices and empty-slot fix — 0.2.4-test — 2026-10-22

- Reproduced a failing regression on Vorkath template excerpts: JsonArray.set received Java null for empty slots, which later failed during item resolution with a message-less NullPointerException.
- Empty slots now use JsonNull. Equipment torso/gloves/boots aliases are mapped to the correct slots; variants retain tabber names and missing Rune pouches are not taken from the next variant.
- Selecting a search result automatically schedules its page fetch and displays concrete setup buttons in the sidebar. Choosing a setup opens Builder. URL/pasted source controls are collapsed under an advanced button; Reload setups is a retry action. Stale page responses, scope changes and disposal cannot replace current choices.
- Java 11 build and all 31 tests passed. New checks cover the failing null case, equipment aliases, tabber pairing, automatic page fetch, concrete variant buttons, opening Melee and ignoring stale errors.
- The full live Vorkath/Strategies wikitext was also parsed separately and yielded exactly Ranged (crossbow), Ranged (blowpipe), and Melee. The sidebar was rendered with a fake host for visual inspection. No game input or in-game import was automated.
- In-client acceptance remains manual: restart with scripts/Launch-Test.bat, search Vorkath, select Vorkath/Strategies, choose one of the three setups, and review Equipment/Inventory, unresolved template items, quantities and Spellbook in Builder.

## Wiki aliases and Strategies selection — 0.2.5-test — 2026-10-22

- Confirmed live: Fractured archive redirects to The Fractured Archive; Fractured archive strategies redirects to The Fractured Archive/Strategies. Appending /Strategies to the latter alias creates a missing page.
- Page content is now requested through the fixed-host MediaWiki API with redirects=1. Canonical page titles are resolved before an optional Strategies subpage is requested. Missing subpages fall back to the article; previously generated alias/Strategies URLs can recover via the original alias. Canonical titles and URLs are used for setup naming and source notes.
- HTTP redirects remain disabled. Missing pages and HTTP failures now have distinct, accurate messages.
- Java 11 build succeeded and 39 tests passed. Coverage includes both aliases, optional-subpage selection, missing-subpage fallback, old malformed URLs, canonical setup names and a real loopback HTTP 302 server confirming one request with no redirect follow-up.
- Parsed the full live API response for The Fractured Archive/Strategies: three concrete example setups, all named The Fractured Archive, with no Java-null slots. No in-game input, profile changes or client restart was performed.
- Manual acceptance: restart using scripts/Launch-Test.bat, search fractured archive, choose either the content alias or strategies alias, and verify the available setups. The active older client remains unchanged.

## Sidebar redesign and always available Wiki — 0.3.0-test — 2026-10-22

- Replaced visible wrapping tabs with fixed Library/Builder/Wiki/Export navigation. Navigation and actions share RuneLite's native bold font, dark surfaces, spacing and focus behavior; the active view has a gold accent. Only this panel is themed, leaving other plugins unchanged.
- Library has fixed search/actions, saved sections and paired Favorite/Archive actions. Builder keeps Save/Save copy outside its scroll viewport, prioritizes Equipment/Inventory and preserves expanded optional sections after edits. Export separates controls, preview and collapsible instructions. Wiki has bounded suggestions, visible setup choices and collapsed technical details.
- Removed the Wiki enable checkbox and config gate. The obsolete wikiRequests flag is not read. Opening the plugin alone still makes no request; typing and page selection use the existing debouncing and stale-response checks.
- Reverted the Hub manifest example to the precise warning about IP, queries and page titles. It does not claim that account data is uploaded.
- Java 11 build passed: 40 tests, zero failures/errors. Layout checks cover 220 and 260 px sidebar widths, fixed navigation bounds, native font family, visible Wiki choice hit areas, Save outside the viewport, persistent expanded Bank settings and disabled export before a draft exists.
- All four views at both widths were rendered and visually inspected without game input. The first preview exposed a stale zero-height layout for dynamic Wiki choices; explicit invalidation fixed it and a regression assertion now covers visible dimensions.
- Manual acceptance remains pending in RuneLite: reopen using scripts/Launch-Test.bat, confirm the LOADOUT STUDIO 0.3 header, edit/save a setup, search Wiki without enabling a checkbox, verify Bank layouts and try both export formats. No current client restart or game action was performed.

## Wiki item recognition - 0.3.1-test - 2026-10-22

- Item names now come from RuneLite ItemComposition.getMembersName(), keeping exact Wiki matches independent of the F2P `(Members)` display suffix. Confirmed the API contract against upstream ItemComposition.java; Context7 returned no matching snippet for this API query.
- The catalogue excludes noted copies and placeholders before exact-name matching. Exact resolution scans all candidates rather than a capped search result. Genuine same-name variants remain unresolved instead of selecting the lowest ID.
- A Wiki choice waits for batched game-item indexing before constructing the draft. Late results are rejected after a different choice, page, profile, draft or disposal.
- Regression coverage uses the supplied Vorkath Melee template fixture: Dragon hunter lance, Torva platebody and Anglerfish resolve despite noted duplicates, and Equipment/Inventory sprite requests reach the host. Dynamic Cheap food/Cheap prayer templates remain explicit unresolved choices. Quantities and Spellbook are not guessed.
- Tests use an offline fake host and representative item metadata; they do not validate real game sprites or every cache variant. In-client acceptance remains required: start 0.3.1-test, choose Vorkath/Strategies -> Melee and verify concrete Equipment and Inventory icons. Click remaining question marks to choose actual variants or dynamic template contents.
- Existing client/profile data were not changed or restarted. The local build is separate from the GitHub repository and Plugin Hub.
- Final Docker Java 11 build passed: 44 JUnit tests, zero failures/errors; test, shadowJar and jar succeeded. Versioned 0.3.1-test artifacts copied to build/libs; Launch-Test.bat targets the new build. git diff --check passed.

## Compact Library rows - 0.3.2-test - 2026-10-22

- Saved loadouts occupy one 32 px row: a clickable name, a favorite star and a trash icon. No separate Open button or second action row. Full names remain intact in stored/exported data and are available in tooltips and accessible labels; visible names end with `...` only when necessary to fit the current sidebar width.
- Icon controls reserve 28 px hit areas and cannot be pushed out of view by long titles. The name button remains keyboard accessible. Star and trash/restore are drawn locally without font glyph or network dependencies.
- The trash control retains the existing reversible archive behavior. Archived entries expose a restore icon; no permanent deletion or profile migration is introduced.
- Existing sidebar checks cover 220 px and 260 px widths, matching row alignment, available title width, favorite persistence and archive/restore persistence. Preview images use an offline Swing host, not a running RuneLite client.
- In-client acceptance: click a saved name to open it, toggle its star, move it to Archived with the trash icon, then enable Archived and restore it. Verify title truncation and full-name tooltip in a narrow sidebar. Current client is left running.
- Final Docker Java 11 validation: all 44 tests passed with zero failures/errors; test, shadowJar and jar succeeded. Preview inspected at 220 px; versioned artifacts copied to build/libs and Launch-Test.bat targets 0.3.2-test. No client restart or in-game interaction.

## Calm loadout Builder - 0.3.3-test - 2026-10-22

- Reordered the Builder to match the supplied Inventory Setups reference: Inventory, Rune Pouch, Equipment, Spellbook, Quiver, Additional filtered items and Notes. Each section uses a centered neutral heading and consistent spacing.
- Inventory is a fixed centered 4 x 7 grid (176 x 308 px). Equipment keeps the familiar 11-position silhouette (132 x 220 px). Item cells are 44 px, flat dark tiles with subtle dividers instead of prominent button frames. Hover and difference highlights remain; empty cells show no distracting plus/slot text, with tooltips and accessible labels explaining the edit action.
- The setup name and Save share a compact fixed header. Save copy, Compare and Export are grouped under Setup actions. Max runes and Ammo quantity preferences live under Loadout options; Bank layout/filter controls retain their own disclosure.
- Additional filtered items and editable Notes are visible in the main loadout flow. Empty additional-item cells are clickable; Quiver remains available for dragging/editing even when empty. All stored Item IDs, quantities, layouts and import/export formats are unchanged.
- Existing offline Swing previews and layout checks cover 220 and 260 px widths, section order and exact grid sizes. Manual acceptance remains necessary for actual RuneLite sprite rendering, hover states, clicks and dragging; no game input or client restart is automated.
- Final Docker Java 11 build passed: all 44 tests with zero failures/errors; test, shadowJar and jar succeeded. Versioned 0.3.3-test artifacts copied to build/libs; launcher updated. The 220 px Builder preview was visually inspected; 220 and 260 px grid geometry and section order passed. Current client was not restarted.

## Library main screen and support link - 0.3.4-test - 2026-10-22

- Library is the explicit startup view. A compact icon toolbar provides New setup, Capture gear, Import setup and Sections. Search has an inline magnifying glass and Clear search control; favorite/archive filters and grouped single-line setup entries stay available.
- The global title bar includes an upper-right coffee icon and visible Buy a coffee label. Its fixed destination is https://buymeacoffee.com/pascalvdbe9. The real host calls RuneLite LinkBrowser.browse only after clicking this control. No automatic browser opening, embedded payment flow, HTTP request or setup/account data is added.
- Context7 found the official RuneLite library but no matching LinkBrowser snippet. The static browse(String) signature and HTTP(S) handling were checked against the upstream LinkBrowser.java source before using the API.
- Existing offline Swing coverage checks the support action through a fake host, the exact fixed destination, compact toolbar size, search clearing, narrow-width alignment and saved Library behavior. It does not open the external website or interact with the game.
- Manual acceptance: open the plugin, confirm Library is the main view, search and clear, open a setup by name, and click Buy a coffee to verify the browser destination. Existing client remains running; no Plugin Hub submission performed.
- Final Java 11 validation: 44 tests passed, zero failures/errors; shadowJar and jar succeeded. Versioned 0.3.4-test artifacts copied to build/libs and launcher targets the new version. The 220 px main-screen preview was visually inspected; narrow-width layout checks also passed at 260 px. No browser or running game was automated.

## Usability and draft transfer optimization - 0.4.0-test - 2026-10-22

- Added a brighter gold/turquoise Inventory badge, inspected at actual sidebar sizes. No remote icon assets or animated attention effect.
- Builder Find items searches the local catalogue, supports double-click/Add to Inventory and COPY drags into loadout slots. Ctrl+F focuses context search and Ctrl+S saves. Full Inventory quick-add preserves all existing slots.
- Slot-to-slot MOVE now includes Additional filtered items, with preflight type/quantity checks and no mutation on invalid Equipment, Rune Pouch or Quiver drops. Per-draft tokens reject stale transfer payloads. Duplicate additional-item insertion is rejected to preserve map semantics.
- Drag initiation uses a left-button movement threshold and disarms the click action. It only affects plugin Swing controls. Live mouse gesture acceptance remains pending with the user.
- Same-draft rerendering preserves scroll position; new drafts reset it. Unsaved state remains visible in the setup-name tooltip.
- The System Design note records the rationale, data boundaries, limitations and manual acceptance sequence. The reference DOCX is unchanged; its preview renderer is unavailable because LibreOffice is absent.
- Final Java 11 build: 50 tests passed, zero failures/errors; test, shadowJar and jar succeeded. Includes actual offline TransferHandler imports, catalogue COPY and slot MOVE, rejected incompatible/stale inputs, full-Inventory preservation, keyboard routing and narrow sidebar previews. The open Find items layout bug and unreadable default list colors were found in visual QA and corrected. Versioned 0.4.0-test artifacts copied to build/libs; launcher updated. No game interaction or client restart.

## Native slot editing and responsible Wiki access — 0.5.0-test — 2026-10-23

- Java 11 offline validation: 59 tests, zero failures/errors. Chatbox callbacks are tested through a fake host; no game inputs were automated.
- Right-click slot menus cover search, capture from the matching container, quantity, Fuzzy, Stack comparison and removal. Old draft callbacks cannot overwrite a new draft. Production inputs use RuneLite ChatboxItemSearch and ChatboxPanelManager; the separate Choose item dialog is removed.
- Inventory rune stacks of 200k are accepted. Explicit zero means unlimited matching and exports with Stack comparison None. Only Rune Pouch retains its 16k cap; 16001 is rejected without modifying the slot. The button now reads Max pouch runes (16k).
- Counts, including the infinity symbol, are painted within each cell. Notes have a ten-row minimum and grow with content with only the outer sidebar scrollbar.
- Wiki interceptor tests cover zero constructor traffic, duplicate coalescing, cache hits, two-second production spacing and Retry-After cooldown. Typing alone does not search; Enter/Search does. No startup polling or remote item sprites.
- Bank title shows a readable loadout name only while this plugin's owned tag is active; ordinary tags are left alone. The bundled OSRS Combat Tutor sword icon replaces the previous custom badge.
- In-client acceptance remains necessary: right-click Inventory/Equipment slots, replace through chatbox, enter 200k Inventory runes and 0, verify Rune Pouch cap and inside-cell labels, inspect long Notes and the active Bank title. Restart the test client manually when ready. No client restart or profile migration was performed.

## Persistent cache, local imports and delayed sprites — 0.6.0-test — 2026-10-23

- RuneLite AsyncBufferedImage is now attached through its addTo helper, so delayed item sprites repaint their actual slot. Previously ImageIcon wrapped an initially transparent image without subscribing the button to completion. A dedicated regression paints a delayed sprite, triggers completion and verifies both repaint and visible pixels without another UI event. This is separate from unresolved ID-zero Wiki slots, which retain their source label and require an exact variant choice.
- Persistent bounded Filepath Wiki cache loads lazily after user input, never at startup. Cached pages remain usable after seven days; only explicit Refresh Wiki updates them. Local suggestions cause no HTTP. Dates, cache failures and stale status are visible. Tests cover a second client instance, stale cache, explicit refresh, malformed disk data and local-only suggestions.
- Read-only import from this RuneLite profile's Inventory Setups V3/V2 and native Bank Tags/legacy layouts creates independent copies with a conversion review. Source configuration is never modified. Available sections, favorites and layouts are preserved where represented. No profile or credential files are scanned.
- Draft recovery is scoped to profile/character and offered explicitly. Comparison evaluates Fuzzy, stack rules, unordered inventory and Spellbook; native extra variation mappings retain their BSD attribution. Ranked Wiki alternatives are presented as choices, never guessed. Unknown quantities remain blank until entered. Bank Tag conversion explains its missing Equipment/quantity/Spellbook/Notes information.
- Narrow sidebar previews verify import controls at 220 and 260 px and cache controls at 220 px. Names/tooltips render plain text; imported labels cannot create Swing HTML/image requests.
- Live RuneLite acceptance remains pending. No game inputs were automated and no client was started or restarted. See local-acceptance.md for the exact user test matrix.

- Final Java 11 offline build: **81 tests passed, zero failures/errors**; test, shadowJar and jar succeeded. Both 0.6.0-test artifacts copied to build/libs. In-game acceptance is not implied by these checks.

## First listed Wiki items — 0.6.1-test — 2026-10-23

- The user explicitly selected first-item defaults. Ranked recommendations now default to their first source alternative and retain all source conditions in Notes. For a combination, its first component fills the slot; complete combination requirements remain in Notes.
- Equal-name variants use the first local catalogue result in stable ascending Item ID order. The strict unique-name lookup remains available separately. Explicit positive Item IDs and quantities remain unchanged.
- Opening an older unresolved saved copy resolves supported names locally after catalogue readiness and requires Save to persist. Per-draft guards reject late callbacks. Bundled Cheap food / Cheap prayer defaults select Manta ray / Prayer potion(4) without template or price requests. Unsupported source expressions remain repairable; unknown amounts remain unknown.
- Final native Windows Java 11 validation: 84 tests passed, zero failures/errors; test, shadowJar and jar succeeded. Includes recommendation order, duplicate-name ordering, old-copy isolation, slot positions, fixed-ID preservation, unknown quantities and zero HTTP during default resolution. The first sandboxed run blocked an atomic temporary-file rename; the same unmodified cache test passed outside the sandbox.
- Versioned 0.6.1 artifacts copied to build/libs. Manual RuneLite acceptance of actual item sprites remains pending; no game input was automated.

## Archive controls and permanent deletion — 0.6.2-test — 2026-10-23

- Active setups now use an archive-box icon. Archived setups expose distinct restore and red trash actions alongside Favorite; all controls fit at 220 and 260 px with truncated titles and full tooltips.
- Permanent deletion requires an archived entry and explicit confirmation with Cancel as the default. Other sections/setups remain unchanged. Failed persistence leaves the entry intact. The dialog captures profile ownership to avoid deleting across a profile switch.
- Deleting the matching open archived setup closes its Builder draft, clears its recovery and owned Bank filter, and invalidates pending draft callbacks. The confirmation warns about discarding unsaved changes. Unrelated drafts remain open.
- Final native Windows Java 11 build: 88 tests passed, zero failures/errors; test, shadowJar and jar succeeded. New tests cover cancellation, active/missing entries, stale deletion, storage failure, matching-editor cleanup and narrow archived rows. Both active and archived previews were visually inspected.
- Versioned 0.6.2 test artifacts copied to build/libs. Manual confirmation in RuneLite is still required; no game input or existing user setup was operated on.

## Player workflow review — 0.6.3-test — 2026-10-23

- Reviewed the four sidebar routes through offline Swing fixtures. Added fixed-header Bank filter and Compare actions, direct starting actions in an empty Library, and a reset for empty filtered views.
- Search and Favorites temporarily reveal matching rows in collapsed sections without altering the saved collapse preference.
- Capture callbacks now require the original profile owner, draft session and edit revision, preventing a delayed response from replacing intervening edits or a newly opened setup.
- Native Windows Java 11: test, shadowJar and jar passed; 93 tests, zero failures/errors/skipped. Five new workflow regressions cover collapsed search/favorites, empty filters, explicit Wiki access, narrow fixed actions and delayed captures. Visually inspected Library, Builder, Wiki and Export at 220 px; fixed controls also verified at 260 px.
- Versioned test artifacts copied to build/libs. Offline fixtures are not live RuneLite acceptance: actual sprites, Bank behavior, game-side capture and receiving-plugin imports still require the user. No game input was automated.

## Local gearing and owned alternatives — 0.7.0-test — 2026-10-23

- Native Windows Java 11 test, shadowJar and jar passed: 115 tests, zero failures/errors/skipped. Twenty-two new regressions cover owned/source/same-slot alternatives, placeholders, unknown Bank state, explicit selection, stale drafts, Undo bounds, selective upgrade and failed persistence, selective sharing, independent copies, item/Notes search, custom layout readback, unordered extras/duplicates, optional Bolt Pouch and portable attack-option data.
- Native layout changes are observed only for the owned active tag and transferred to a session/revision-guarded Custom draft; the user must Save. Other tags remain unchanged.
- Inventory/Equipment and tracked varbit/parent-varp changes coalesce through a dirty flag. Only changed snapshots reach Swing; borders/status update without rebuilding Notes. Login/profile epochs reject old snapshots. Bank observations are client-thread confined and exclude zero-amount placeholders; no new Wiki or image requests are introduced.
- Owned alternatives have no automatic selection. Source alternatives, bundled groups and same-slot-only fallbacks are distinguished. Current availability is checked before applying selected changes. Quantities/rules are retained; incompatible non-stackable replacement amounts are rejected. Other storage and stale closed-Bank observations remain explicitly outside authoritative ownership.
- Recent, Favorites-first, Manual ordering, item:/notes: searches, exact-ID bulk upgrade with guarded Undo, selected setup/section bundles, a configurable Bank-only hotkey and favorite/recent Bank-menu shortcuts are present. Bank-menu and key-event integration still need live user acceptance.
- Build dependencies contain RuneLite client and normal development/test dependencies only. The complete test JAR contains zero inventorysetups or com/banktaglayouts classes. Plugin bytecode has no class dependency on those hub plugins. The declared native Bank Tags dependency remains intentional. Existing data imports and portable codecs remain optional interoperability.
- Visually inspected the 220 px alternative flow and Library/Builder layouts; layout fixtures also check 260 px. Versioned artifacts copied to build/libs and hashes verified. No game input was automated; real Bank persistence, potion storage, live container/style changes and optional-hub-disabled startup still require manual confirmation.

## Native plugin preferences — 0.7.1-test — 2026-10-23

- Moved general preferences to native ConfigSection groups: Default loadouts, Comparison, Sidebar, Hotkeys and Bank. Existing filterHotkey/bankMenu keys remain unchanged. Defaults keep previous behavior unless explicitly changed.
- Removed the Library sorting dropdown and Ammo preference checkbox from the sidebar. Sorting persists in config; row movement selects Manual there. Removed duplicate Compare/Export/Bank actions; Save copy is under Setup details. Current-setup layout overrides remain.
- General display/comparison changes update the panel without recreating Notes or rewriting stored/imported setup values. Draft recovery retains its recorded Ammo choice until another setup/global preference change. Only known preference keys refresh the panel; library/recovery config writes are excluded.
- New-loadout defaults are applied to cloned blank/captured data only. Empty new drafts retain the chosen layout mode/direction even when their cells are identical.
- Native Windows Java 11 test/shadowJar/jar succeeded: 120 tests, zero failures/errors/skipped. Five new regressions cover default creation, source isolation, live preference refresh, Ammo versus Rune Pouch rules, optional header visibility and removal of duplicate controls. 220 px Library/Builder previews visually inspected; existing width checks also cover 260 px.
- Versioned test artifacts copied to build/libs and hashes verified. Native RuneLite settings rendering, live preference interaction and old-key retention still require manual user acceptance. No game interaction was automated.

## Styling consistency — 0.7.2-test — 2026-10-23

- Slot context menus (including stack submenus) and Library ordering menus now use the sidebar font, dark control background and consistent menu padding. Disabled actions retain native disabled rendering.
- Standard and owned-alternative list renderers use the RuneScape body font and keep HTML disabled. Import, share and upgrade review content receives the same form theme; native dialog buttons remain managed by RuneLite.
- Native Windows Java 11 test/shadowJar/jar succeeded: 121 tests, zero failures/errors/skipped. The new regression checks actual slot/submenu and Library menus. Existing offline layout tests cover 220/260 px; Library, Builder, Wiki, Export and slot-menu previews were visually reviewed.
- Versioned local artifacts copied and hashes matched. Offline Swing fixtures do not validate live RuneLite look-and-feel, game sprites or real input; manual acceptance remains required.
