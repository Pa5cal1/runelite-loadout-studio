# Loadout Studio sidebar system design

This design applies the supplied System Design template topics to the requested RuneLite sidebar implementation. The retained Word reference is unchanged. The user requested a working plugin rather than a Word document; this implementation note records the decisions and acceptance checks.

## 1 Abstract

Use a fixed two-by-two navigation grid with the component-local Loadout Studio font and warm charcoal/gold palette. Native settings keep RuneLite fonts. Give each view one main task. Keep Save visible during Builder scrolling. Wiki searching is always available without a permission checkbox; network traffic begins only when the user searches or requests a page.

## 2 Goals and non goals

Make controls readable at 220 to 260 px panel widths. Keep Library, Builder, Wiki and Export in a stable order. Remove duplicated persistent guidance and avoid stretching empty fields. Preserve saved libraries, import formats, drag and drop, Bank filtering, layout modes, Rune pouch and Quiver editing. No game actions or account-data uploads are introduced.

## 3 Background and problem

Wrapped Swing tabs reordered themselves and used a font different from the rest of the sidebar. Wiki had an unnecessary checkbox, oversized empty areas and technical controls before the results. Builder placed optional settings before Equipment. Long footer text competed with task content.

## 4 Proposed architecture

StudioPanel owns a fixed navigation header, a content view and a two-line status footer. The existing tab selection model remains internal; its tab strip is hidden. Only this panel is themed, leaving other RuneLite plugins unchanged. Library uses fixed controls above its growing saved-setup list. Builder separates a fixed Save header from scrollable item layouts. Wiki has bounded result lists and immediate setup choices. Export separates format and copy controls from a code viewport.

## 5 Request lifecycle

Typing edits a local field only. Enter or Search Wiki explicitly requests suggestions. Selecting a page schedules fetching after 300 ms. WikiClient resolves the canonical title and optional Strategies page before WikiParser produces choices. Choosing a setup opens a draft. Request revisions, profile scope and disposal checks reject stale responses. No request is made just by opening the plugin. A shared bounded queue combines identical work, caches responses, spaces requests and pauses on rate limits; see wiki-network-policy.md.

## 6 API and data contracts

Inventory Setups JSON and Bank Tag exports remain unchanged. Existing libraries keep their ConfigManager group and saved structure. The obsolete wikiRequests gate is ignored rather than renamed or used to disable searching. The Hub manifest warning states precisely which information goes to OSRS Wiki.

## 7 Consistency and recovery

Changing navigation does not reorder buttons. Expanded optional sections remain open during Builder rerenders. Unsaved-draft confirmation is retained. Failed requests display useful errors, and Reload setups retries the selected page. Missing Strategies pages fall back to the article. Duplicate stale responses cannot replace current choices.

## 8 Security and privacy

Wiki requests are always available as explicitly requested. Search queries, requested page titles and the connection IP address reach OSRS Wiki. Gear, library backups and account credentials are not uploaded. Keep HTTPS host validation, response limits, disabled HTTP redirects and the specific Hub installation warning. The current testclient is not restarted during implementation.

## 9 Operational readiness

Run the Java 11 tests and build a separate versioned testclient. Render all four views at narrow and wider widths without game input. Check control clipping, keyboard focus, stable navigation and the persistent Save area. Manual in-client acceptance remains required.

## 10 Alternatives considered

Four horizontal tabs are too cramped. Wrapped tabs change order when selected. Increasing only the font size leaves the hierarchy unchanged. Changing global Swing defaults could affect unrelated plugins. A fixed scoped navigation panel gives predictable placement and localized styling.

## 11 Open questions

Plugin Hub review remains pending. Ranked recommendation tables still need explicit gear choices and are not silently converted. Final user acceptance must cover actual RuneLite font rendering, scrolling and drag and drop at the user's configured UI scale.

## 12 Decision and next steps

Build 0.4.0-test, verify fake-host previews and regression tests, then let the user restart manually. Test Library capture and saved sections, Builder editing and Save, Wiki suggestions and canonical page choices, and both export formats. Preserve the previous testclient for rollback.

## Optimization review for 0.4.0

The current design prioritizes three short paths: Library to a saved setup with one click, Wiki content to a draft, and Builder Find items to item insertion. Library remains the startup view. The gold and turquoise Inventory backpack badge is drawn at 32 px and inspected at 16, 24 and 32 px so it remains distinctive in the sidebar without animation or remote assets.

Builder keeps Save and the collapsible Find items panel outside the scrolling loadout. Search is debounced by 180 ms, returns at most 100 locally cached candidates and exposes a four-row result viewport. Double-click or Add to Inventory fills the first empty slot; a full Inventory is reported rather than replacing slot one. Dragging from the catalogue copies an item into a chosen destination. A guarded asynchronous item-description lookup prevents a delayed result from overwriting a different draft or a changed destination.

Ctrl+S saves the current draft. Ctrl+F focuses Library search, Wiki search or Builder Find items according to the active view. The setup-name tooltip shows whether the draft has unsaved changes. Rerendering the same draft preserves the loadout scroll position; a different draft starts at the top.

Drag and drop is restricted to this plugin's drafts. It does not withdraw, equip or move items in the game. Inventory, Equipment, Rune Pouch, Quiver and Additional filtered items accept compatible local transfers. Item IDs, quantities and flags are copied or swapped together. Invalid Equipment slots, non-runes, Rune Pouch quantities above 16,000, non-arrow/non-bolt Quiver contents, duplicate additional items and stale or malformed drag payloads are rejected before modifying slots. Additional filtered items retain their existing unique-item set contract.

The drag payload includes a per-draft session identifier and only names supported containers and valid indices. Catalogue drags are restricted to candidates in the current results. Network and persistence boundaries remain unchanged: profile-scoped ConfigManager storage, bounded OSRS Wiki requests, existing export schemas and an explicit user-click support link. No account credentials are introduced.

Offline Java 11 tests exercise the actual Swing TransferHandler import path with an injected fake host. These tests prove draft-state transitions and failure behavior, not native mouse dragging inside a live RuneLite client. Manual acceptance must cover drag gestures, the visible sidebar badge, actual game sprites, keyboard shortcuts and both receiving-plugin exports.

The supplied System Design reference file remains unchanged. Its twelve design topics inform this note. The available Word preview helper was checked again and cannot render because LibreOffice soffice.exe is absent; no unverified Word artifact is presented as a finished document. The requested plugin UI is rendered and inspected using the offline Swing harness.

## Native editing revision — 0.5.0-test

Slot menus expose replacement, quantity, Fuzzy, stack comparison and removal using RuneLite chatbox inputs. Callbacks are scoped to the active draft and original slot value. Notes use a ten-row text area that grows inside the outer sidebar scroll; counts are overlaid within 44 px cells. Only Rune Pouch stacks have a 16,000-per-rune cap. Inventory stacks such as 200,000 are valid. Explicit zero means unlimited matching and serializes with Stack comparison None; it does not represent infinite in-game items. Bank titles show the setup name only while our own tag is active. The requested Combat Tutor asset is bundled, with no runtime image download. No game input was automated; live acceptance remains with the user.

## Persistence and interoperability revision — 0.6.0-test

The Wiki boundary now has a lazy, persistent Filepath cache. No startup IO or HTTP is introduced. Cached page sources are used even after their freshness interval until an explicit refresh. Local suggestions use saved titles; a manual remote search remains available. The UI exposes source dates, stale status and conversion loss. Cache parsing, validation, throttling and disk writes stay off the client and Swing threads.

Concepthandling uses a versioned, bounded recovery record in this plugin's own scoped ConfigManager group. Dirty changes are stored independently from saved Library entries. Recovery is offered on reopening, never automatically applied. Existing RuneLite synchronization preferences still govern configuration.

A separate read-only reader supports modern Inventory Setups and native/legacy Bank Tags using only their known configuration keys. It has no write interface. Results are copied after explicit selection and validated before committing the destination library. Owner/revision checks reject stale results across profile changes. JSON fallback remains supported for old/unsupported formats.

Comparison is pure logic with a maximum matching step for unordered Inventory and explicit Fuzzy/Stack comparison rules. The extra Inventory Setups variation table is attributed and bundled with its BSD license. The native Bank filter uses that mapping too. A guided unresolved-slot editor keeps uncertain source choices visible and never silently chooses a recommended rank or quantity.

Manual acceptance is tracked in local-acceptance.md; automated fixtures do not establish live game behavior. No paid service, account, background data collector or new remote image dependency is added.

### Sprite completion boundary

An item tooltip can exist before its local sprite has finished rendering. Slots subscribe through AsyncBufferedImage.addTo; painting counts stays independent of image readiness. Source-only ID-zero Wiki choices keep `?` and an explicit exact-ID tooltip. No HTTP image fallback, runtime image download or automatic variant guessing is introduced. Offline regression verifies delayed completion repaints; only user acceptance can verify actual game-cache sprites.

## 0.6.1 first-choice defaults

The user explicitly selected automatic first choices. Recommendation order remains source order; the first component of a combination fills its slot and the complete conditions remain in Notes. Equal-name variants use stable local search order. Existing positive Item IDs and quantities are never replaced by this resolver. Older ID-zero copies resolve after catalogue readiness, with a per-draft guard against late replies; saving remains explicit. The two common cheap-item template defaults are bundled and introduce no new HTTP operations. Unsupported expressions remain visible for manual repair.

## 0.6.2 archive actions

Active rows retain their two compact controls: favorite and archive box. Archived rows reserve three 28-pixel controls: favorite, restore and red trash. Titles shrink with an ellipsis while tooltips retain the full name. Permanent deletion is restricted to archived entries and uses a Cancel-default confirmation. Library updates are cloned, validated and persisted before replacing UI state. Failed storage preserves the original entry and editor. Confirmations capture profile ownership; scope changes cancel deletion. Successful deletion closes the matching open Builder draft, invalidates its callbacks and clears its recovery/filter; unrelated setups and drafts remain intact.

## 0.6.3 player workflow review

The fixed Builder header exposes Compare and the current Bank filter toggle, so routine gearing does not require scrolling past Notes. The layout selector stays in the existing disclosure. Filtered Library results expand matching sections temporarily, preserving manual collapse preferences when filters clear. Empty views distinguish first use from a restrictive filter and offer direct next steps. A captured gear reply must match the original profile owner, draft session and edit revision; late replies cannot overwrite subsequent edits. Offline Swing fixtures exercise these routes without game interaction or Wiki traffic.

## 0.7.0 local gearing and ownership

The native layout adapter observes only its own active tag, validates changed cell IDs and reports Custom positions to the guarded active draft. The user must Save. Local container/selected-varbit events set a dirty flag; one ClientTick consumes it, captures local state and only publishes a changed snapshot. Profile/login epochs reject stale results. The panel refreshes borders/status without rebuilding Notes.

OwnedGear carries an immutable local Bank observation plus carried/worn items, with a timestamp. Unknown Bank state remains unknown, placeholders are excluded, and other storage is outside the observation boundary. Suggestions prefer source alternatives, then bounded bundled families, then an explicit same-Equipment-slot fallback. No option is selected automatically. Rechecking and context/revision validation precede an atomic draft replacement; Undo rejects intervening edits.

Library mutations clone/validate/persist before replacing state. Bulk upgrades have exact-ID scope, preview, slot checks and one bounded Undo guarded by the complete current library. Selected share bundles use a marker separate from whole-library backups, so their import merges independent copies. New lastOpened metadata defaults to zero for old libraries; existing keys/schema remain compatible.

Inventory Setups and standalone Bank Tag Layouts are optional data sources/formats, not linked modules. In this historical version built-in RuneLite Bank Tags was the declared plugin dependency; 0.8.0 removes that hard dependency. No native renderer is duplicated and no new HTTP route is added. RuneLite API changes can still require maintenance.

## 0.7.1 general preferences boundary

Native ConfigSection groups own general defaults, comparison, sidebar order/visibility, hotkeys and Bank menus. Existing filterHotkey and bankMenu keys remain unchanged. Only explicit display/comparison keys refresh the open panel; library/recovery writes do not trigger preference refresh. Defaults are cloned into new blank/captured loadouts only, never imported or saved records. Empty drafts keep their configured layout mode/direction even when generated cells cannot distinguish them. Current setup overrides remain in collapsed sidebar controls. Duplicate Bank/Compare/Export actions are removed. Preference refresh updates comparison/order/visibility without replacing Notes or marking a saved setup dirty. Legacy recovery retains its captured Ammo preference until a later explicit preference change or setup opening.

## Player feedback update in 0.8.0

The hard built-in Bank Tags dependency is removed. Its active plugin injector supplies optional native tag services; disabled/missing native tags use IndependentBankView and BankViewPlan. The independent path filters script callbacks and positions original Bank widgets without rewriting their IDs, amounts or child indices. User drag changes the owned layout; ordinary Bank stock remains real. Potion Storage and account-wide ownership are outside the standalone observation. Confirm transitions and native Bank listener restoration manually.

Known items can filter while unresolved slots remain editable. The item catalogue ranks actual equipment/tradeable definitions and verified gameval identities instead of only the lowest ID. Known beta-ring repairs require an explicit Save and preserve Custom positions.

Automatic local alternatives are coalesced and update their own rows, retaining Notes caret and text. Choices are source/family ordered, bounded, and explicitly applied. Native preference sections now include Wiki and Editing. A compact setup icon row holds per-setup overrides; disclosure headers and Wiki choices are visually distinct from actions. Minimum sidebar width is bounded to avoid long import labels requesting a larger client. No code changes the game canvas size.

## Player workflow review in 0.8.3

The primary path remains Library/Wiki → Builder → review local gear differences and bounded alternatives → Save → optional Export. Player review found that replacing a unique item in a custom Bank layout appended its replacement instead of occupying the previous cell. Replacements now update those cells before normal custom-layout reconciliation; if another setup slot still needs the old item, its position is retained and the new ID is appended. Undo retains the previous portable layout.

The alternatives heading prefers a stored item label over a generic numeric ID when the missing item is absent from the Bank observation. General settings stay in RuneLite configuration. Sections, actions and Wiki preset cards remain visually distinct. Source conditions and equipment combinations still require player review; the plugin does not evaluate combat optimality or withdraw/equip items automatically.

The README gallery intentionally uses offline, labeled UI previews with example data. It demonstrates sidebar presentation, not live Bank correctness. The in-game resize report, Windows drag visuals, native chat focus and independent Bank transitions remain pending player confirmation.

## Export formatting in 0.8.4

Export has two in-panel representation controls. Pretty Print applies to Inventory Setup JSON and native Loadout Studio JSON. GZIP/Base64 compression applies only to native JSON because receiving external plugins do not decode it. Loadout Studio's compressed transport has a versioned prefix, bounded encoded and expanded sizes, strict UTF-8 decoding and the existing schema/nesting validation. Native JSON includes a version marker and preserves labels/choices; external Inventory Setups output removes that marker and editor metadata. Preview and Copy pass through the same export path; checkbox changes do not modify saved loadouts. Bank Tag text remains CSV in both supported dialects.

## Support control and settings previews in 0.8.5

Buy a coffee is a permanent header control. The visibility config item and its runtime check are removed at the user's request; existing stored hidden values are ignored without rewriting other preferences. The URL is still opened only on user action.

Three settings documentation images derive their names, ordering, sections and defaults from LoadoutStudioConfig. They use RuneLite styling but are explicitly labeled offline settings previews; the renderer does not create a client, read configuration files or interact with the game. They do not constitute live acceptance of the configuration window.

## Local diagnostics and Bank ownership in 0.8.5

StudioDiagnostics keeps fixed enum-based counters/durations and a 24-entry ring in memory. UI action boundaries, Wiki requests/cache and Bank operations record typed outcomes; validation and expected states are separated from unexpected faults. Reports deliberately omit messages and dynamic payloads; only bounded own-package stack frames are retained for unexpected exceptions. Reports are reviewed and copied manually. Opening the static GitHub issue-form URL sends no report; no remote telemetry or persistent diagnostics store is implemented. Support config can stop and clear recording.

Show worn items gets a distinct Loadout Studio submenu. Layout/filter actions carry the profile owner and draft session token, are checked on the EDT, open our navigation panel and modify only our draft. Automatic filtering yields to a competing active Bank Tag when respectOtherBankFilters is true; explicit player actions can switch the view. Pausing releases our independent view/predicate without closing the other tag. Other plugin menus and configuration remain untouched. This is cooperative ownership, not a global lock against another plugin's later writes; Inventory Setups' own active-panel requirement is documented for players to set manually.

## Independent visual identity in 0.9.0

StudioTheme owns the sidebar palette: background #1C1A17, surfaces #27231D, controls #171512, gold accent #E8BC63 and muted text #BCAE94. Logical SansSerif 12 is the UI body font; small uppercase section labels and a two-line LOADOUT/STUDIO wordmark distinguish the chrome from Inventory Setups' presentation. Route glyphs, an active underline, card rails and clear primary actions provide redundant visual cues. Favorites retain a warm secondary star.

The change is limited to this component tree and locally created popup menus/tooltips. Native RuneLite settings and in-game menu rendering are owned by the host and remain unchanged. UIManager values are never modified by StudioTheme. Item art and count fonts remain OSRS-style, with existing grid dimensions and drag/drop semantics. Settings/export/diagnostic flags and stored data receive no migration or rewrite.

The current header and toolbar share the bundled OSRS Combat Achievements icon. Gold accents restore the requested warmer identity while keeping distinct navigation, cards and disclosure treatments. Native settings retain RuneLite styling.

## Candidate architecture review 0.9.1-rc.1

Status: proposed for final manual acceptance, 24 October 2026. The [design report](release-readiness.md) records player scenarios, evidence, implications and release gates. The retained templates remain unchanged; these repository notes describe the implementation, not a rendered Word artifact.

| Component | Boundary and state | Failure behavior |
| --- | --- | --- |
| StudioPanel | EDT-owned draft, session token, scoped library and local component theme | Late callbacks are ignored; broken saved data is not overwritten |
| LoadoutStudioPlugin | Client-thread game snapshots, owner epochs, optional core Bank Tags gateway | Login/profile changes clear observations and owned filtering |
| WikiClient and WikiCacheFile | On-demand queue and bounded public-source cache on a worker | Cooldown, stale normal-request fallback and visible cache errors |
| WikiParser and ItemCatalogue | Pure source parsing and local item identity resolution | First source choice is used when resolvable; remaining choices stay visible |
| OwnedGear | Snapshot of observed Bank plus Inventory/Equipment; ranked local proposals | No automatic replacement; stock rechecked before applying |
| SetupCodec and SetupExportText | Bounded JSON/CSV parsing and versioned native compression | Unsupported formats, expanded-size overflow and unresolved exports fail visibly |
| StudioDiagnostics | Bounded session counters and allowed technical frames | No automatic transmission; disabling clears the session |

Request lifecycle: explicit UI action, token capture, validation, cache lookup, bounded/coalesced request if necessary, source parse, local item resolution, reviewed draft and explicit Save. Layout/filtering remains local and never performs an equipment or withdrawal action. Public Wiki cache is independent of character libraries; no cached gear is uploaded.

Consistency: config profile plus RuneScape character scope identifies saved libraries; draft/owner revisions invalidate pending work. A saved copy is deep-cloned and duplicate names are numbered. Runtime observations are not durable inventory authority. Sharing a Bank Tag cannot recreate absent Equipment assignments or amounts.

Tradeoff: source-ranked and bundled alternatives are explainable and avoid extra HTTP, but they cannot certify item requirements, unlocks or HCIM safety. The per-client rate budget does not coordinate separate client processes. Keep these limitations visible and test native service transitions before stable release. Future background refresh, telemetry endpoints or automatic game actions require a new design and policy review.
