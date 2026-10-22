# Loadout Studio sidebar system design

This design applies the supplied System Design template topics to the requested RuneLite sidebar implementation. The retained Word reference is unchanged. The user requested a working plugin rather than a Word document; this implementation note records the decisions and acceptance checks.

## 1 Abstract

Use a fixed two-by-two navigation grid with native RuneLite fonts and a consistent dark palette. Give each view one main task. Keep Save visible during Builder scrolling. Wiki searching is always available without a permission checkbox; network traffic begins only when the user searches or requests a page.

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
