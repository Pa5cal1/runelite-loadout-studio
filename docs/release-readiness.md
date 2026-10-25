# Loadout Studio release readiness

Version: **0.9.1-rc.11**. Review date: **25 October 2026**. Status: **local release candidate, awaiting player acceptance and external review**.

Reviewed candidate source commit: `0e2ad367b71ad8118b2b9bfad2fbd3ae4472480f`. The prepared manifest pins this source; later documentation-only changes do not alter its code.

## Startup follow-up

A real local startup exposed two EventBus subscriber-name errors missed by the prior offline tests. The third candidate corrects them and adds native EventBus registration coverage. The corrected local client remained running with a responding visible window on 25 October 2026. All 209 automated tests passed. This does not replace the remaining gameplay acceptance.

The fourth candidate restores Combat Tutor artwork and validates a native-size 16 px sidebar image. All 210 offline tests pass. The already-running rc.3 client is left intact; the icon correction still needs the next user-controlled restart.

The fifth candidate fixes a user-reported full-width gear-difference tooltip. Offline tests reproduce 42 differences, verify compact grouped rendering and check refresh after manual Compare. The hover change still requires a user-controlled client restart.

## Executive summary

The candidate supports character-scoped libraries, manual loadout editing, local owned-item alternatives, interoperable exports and on-demand cached Wiki sources. Static inspection and offline tests support moving to the last manual acceptance pass. They do not establish that every RuneLite interaction works in a real client, and they do not constitute RuneLite or Jagex approval.

One concrete defect was corrected during the review: useful `Slayer task/…` articles were hidden alongside technical subpages in search. Those activity pages now remain discoverable; technical descendants remain excluded. The README icon now matches the requested Combat Tutor asset.

## At a glance

| Gate | Evidence | State |
| --- | --- | --- |
| Java compatibility | Java 11 compilation and class version 55 | Locally checked |
| Regression suite | 238 tests, zero failures, errors or skips | Locally passed |
| Account separation | Three-account save/reopen/favorite test, draft recovery and delayed callback guards | Offline passed; live switching pending |
| Wiki request restraint | Interceptor tests, persisted cache, explicit actions, rolling request budget | Offline passed |
| Sidebar appearance | Gold theme, 220/260 px layout checks and example-data previews | Offline checked; live scaling pending |
| Public listing | Name, author, description, candidate version and bundled icon | Prepared |
| Plugin Hub submission | Public source access, final acceptance and manifest pin | Pending |

## Introduction

This review considers three account types and three usage modes. These are scenario-based evaluations, not interviews with or endorsements by streamers. Lake, b0aty, Odablock, gnomemonkey and SoloMission are examples of the requested audience lens; no individual preference is attributed to them.

## Item identity follow-up

See [the item-ID compatibility audit](item-id-compatibility.md). The second candidate adds known duplicate exclusions, Bank-only canonical export IDs and runtime external-export validation. Explicit imports and captures are not rewritten by the exclusion list.

## Key findings

### Context and conditions

| Player scenario | Useful workflow | Evidence and practical limit |
| --- | --- | --- |
| Maxed main with endgame gear | Capture current gear, organise raid/boss sections, retain exact variants and switch saved setups quickly | Pouch/Quiver/attack-option capture, item identity and layouts have offline tests. Actual charged variants, live containers and withdrawals still need a client pass. |
| Mid-tier ironman | Load a Wiki setup, review owned alternatives in source order and save an adapted copy | Recommendations come from the selected source or a small bundled family. Combined stock is checked before replacement. Requirements, unlocks and encounter conditions are not solved automatically. |
| New hardcore ironman | Start blank or capture starter gear, keep a separate library and inspect missing items | No automatic replacement, withdrawal or equipment action occurs. A suggested item or first Wiki choice is not a statement that the encounter is safe for that character. |
| Fast PvM and switching | Favorites, name search, Bank menu shortcuts and hotkey | Frequent controls stay above the loadout. Live coexistence with other Bank filters and hotkey focus require acceptance. |
| Streaming and sharing | Export a selected setup, preserve source notes and manually share an inspected diagnostic report | Exports intentionally contain setup contents and Notes. Review them before sharing. Diagnostic reports use a separate technical allowlist and are not sent automatically. |
| Repeated progression or restricted series | Save copies with numbered names, sections, recovery and undo for guarded edits | Replacement stock excludes other storage. A closed-Bank observation may be stale. Archive and permanent deletion are distinct actions. |

### Patterns in the evidence

The strongest invariants are explicit user actions, independent saved data and small local transformations. Character/profile changes invalidate asynchronous UI work and clear owned Bank observations. Stored setup copies do not rewrite Inventory Setups or Bank Tag Layouts records. Automatic alternative discovery is local; applying changes requires a user choice and stock recheck.

The plugin does not require Inventory Setups or Bank Tag Layouts classes at runtime. Built-in Bank Tags is used when active; an independent Bank view handles its disabled state. Interoperability and coexistence are different questions: another plugin that continuously reapplies its own filter can still contend for the Bank view. The recommended Inventory Setups active-panel setting remains a manual user choice.

## Defect review

Review target: the current candidate's production source and release metadata, with the final patch checked against the previous 0.9.0 source. The review phase was read-only; the documented fixes were applied afterwards.

- **[P2] Preserve useful Slayer task search results — WikiClient.activityTitles.** Excluding every slash title hid activity-specific recommendation pages. The candidate allows one-level Slayer task articles while continuing to remove technical subpages. An offline regression covers both cases.
- **[P3] Match the documented icon to the actual asset — README.** The getting-started image and plugin must refer to the same requested artwork. The fourth candidate restores Combat Tutor consistently in both.

No additional qualifying findings in the final reviewed patch. This is not a claim that the entire plugin is defect-free. The service transitions, native UI lifecycle and game-facing interactions listed below remain material test gaps.

## Implications

### Wiki requests

| Trigger or condition | Result |
| --- | --- |
| Client/plugin startup, opening Library or a saved setup | No Wiki HTTP request |
| Typing | Local cached suggestions only |
| Search/Enter, selecting a page or explicit Refresh | May request Wiki source; cache and queue rules apply |
| Reopening a fresh source | Cached response |
| Page older than the configured seven-day default | Refresh only when opened by the user |
| 429/503, Retry-After or API rejection | Cooldown; no automatic retry loop |
| Item sprites, identity resolution and alternatives | Local game data; no Wiki image or per-item API traffic |

The request queue permits one in-flight operation, at least two seconds between starts, ten starts per rolling minute and twelve unique pending jobs. Concurrent identical requests coalesce. A page selection can require more than one API operation for redirects or Strategies fallback; it is not guaranteed to be one request. The limit is per active client, not shared across multiple RuneLite processes. Several simultaneously used accounts can therefore multiply total traffic. No background polling is introduced.

Search cache lifetime is six hours; page freshness defaults to seven days and is configurable from one to thirty. Public cached sources are reused across characters within the same cache location. Expired cache may be shown after a failed normal refresh. Cache success does not prove that the Wiki's current content is unchanged. These are conservative project limits, not an OSRS Wiki quota or permission grant. See [the complete request policy](wiki-network-policy.md).

### RuneLite and Jagex requirements

The inspected production code uses Java 11, injected OkHttp/Gson, RuneLite Filepath and LinkBrowser, bounded parsing, cancellation and client-thread/EDT boundaries. No input injection, automated equipping/withdrawal, outgoing chat modification, combat prediction, native code, reflection or runtime code download was found in the inspected paths. Own Bank menu entries use local RuneLite actions. Custom chatbox search and quantity widgets are UI inputs rather than outgoing chat-message automation.

The gold styling is component-local and does not replace global UI defaults. Setup actions do not change other plugins' preferences. Core Bank Tags interoperability uses its API and cleans up only the plugin-owned temporary layout key; reviewers should inspect that integration and the independent Bank view carefully. The standard production JAR excludes the launcher and test classes. The separate local all-in-one test client is not the artifact submitted to the Hub.

External I/O and Bank widget/layout changes deserve explicit reviewer attention. The coffee link is optional support and unlocks no feature; no paid external service is required. Acceptance is still at RuneLite's discretion. The Hub's own review focuses on security and rules; it does not guarantee usability, performance, correctness or compatibility.

## Recommendations

1. Complete the manual acceptance list below on the exact candidate build.
2. Keep the current low-request design. Consider cross-process request coordination only if actual usage or Wiki maintainers identify a need; do not add periodic refresh or per-item services.
3. Review encounter conditions and item requirements on every ironman/HCIM adaptation. Do not describe alternatives as automatic best-in-slot or safe-to-use recommendations.
4. After acceptance, use a stable **1.0.0** release and a newly pinned source commit for submission. Fixes before acceptance increment the candidate number, such as **0.9.1-rc.6**.

## Manual acceptance before submission

Use three separate characters/profiles. Do not test dangerous encounters to validate the plugin.

- Save a differently named setup on each account; switch both character and RuneLite profile; reopen each library and verify exact gear, favorites, sections and unsaved recovery remain separate.
- Begin a Wiki search/item selection, switch character, then finish or let it return. It must not change the new account's Builder.
- On the main, capture worn gear plus Inventory, Rune Pouch, Quiver and attack option. Check real charged/ornamented items, quantities, zero/unlimited and a stack above 16k outside the pouch.
- On the ironman, open the Bank, load a recommendation, choose a lower owned source-ranked alternative, apply and undo it. Verify combined stock when the same item is used in several slots.
- On the new hardcore, keep an unavailable item as missing, choose another manually, and verify the plugin never equips, withdraws or sends a game action by itself.
- Check Bank filtering with built-in Bank Tags active and disabled. Test Preset, both Zigzag directions, native/custom drag movement, clearing, reopening the Bank and foreign-filter handover. Verify real withdrawal targets after layout changes.
- With Inventory Setups/Bank Tag Layouts installed, verify each plugin can take over deliberately and clearing Loadout Studio leaves an active foreign filter alone. Check potion storage separately.
- Test chatbox search, right-click and Shift-right-click bulk actions; cancel and confirm; ensure focus and temporary UI return normally.
- Convert a known Inventory Setup to each Bank Tag format and back. Compare exact names, IDs and layout; review the documented losses in Equipment assignment, quantities, pouch and Spellbook data.
- Verify no Wiki traffic at startup or while typing; Search once, reuse the source, restart and reuse the persisted cache, then explicitly Refresh. Test offline fallback and the visible cached timestamp.
- Test narrow/resized windows and display scaling, Notes scrolling, every tooltip, amount labels, the sidebar icon and native settings.
- Export a backup, restore a separate copy, archive/unarchive, cancel deletion, then delete an archived disposable setup. Confirm active saved setups remain intact.
- Review/copy/reset diagnostics and disable recording. Confirm no automatic report submission and no player/setup content in the report. Check the support link without expecting it to unlock features.

## Conclusion

Proceed with the manual candidate acceptance pass. Do not claim Plugin Hub readiness is complete until the above game-facing checks pass, public source access is approved and the actual Hub checks/review accept the pinned revision.

## Appendix

### Sources checked on 24 October 2026

- [Plugin Hub contribution and submission instructions](https://github.com/runelite/plugin-hub/blob/master/README.md).
- [RuneLite prohibited/restricted features](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features).
- [What Plugin Hub review does and does not verify](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review).
- [Jagex Third Party Client Guidelines](https://secure.runescape.com/m=news/third-party-client-guidelines?oldschool=1).
- [MediaWiki API etiquette](https://www.mediawiki.org/wiki/API:Etiquette), used as general API guidance rather than an OSRS-specific quota.
- Production source and the local regression suite; all tests use offline fixtures/interceptors rather than live account automation.


### Legitimate item selection correction (0.9.1-rc.6)

Corrects false rejection of real imbued items such as Salve amulet(ei). Native manual selections are authoritative after canonicalization; automatic preferences no longer classify legitimate imbues as invalid. Regression coverage includes three Salve variants through sidebar selection, save and export, plus saved-source preservation and imbued mask, helm and ring families. In-game confirmation remains pending.


### Trouver variant checks

The rc.6 offline suite includes all 145 declared Trouver-related IDs from the pinned API fixture, including locked, broken/mangled definitions and the parchment itself. Explicit selection and both export round trips preserve their IDs. Dedicated cape cases cover Infernal max cape (l), Fire max cape (l), Fire cape (l) and Infernal cape (l): exact-name lookup resolves them, exact Bank filtering retains the selected locked item and its layout reference, and does not silently substitute another cape. This tests identity policy and offline boundaries; it does not prove every definition is wearable or owned. Confirm locked cape selection, sprite, save/reopen and filtering in the actual client.


### Grouped owned alternatives (0.9.1-rc.7)

Repeated Inventory items with the same ranked alternatives share one choice, for example Inventory ×20: Manta ray. Apply replaces all matching slots together; Keep original leaves the group untouched. Existing quantities and per-slot matching options remain intact. Different source recommendations and Equipment positions stay separate. Combined stock is rechecked before applying; insufficient stock leaves the draft intact. Undo restores the complete group. Confirm this behavior in the test client before release.


### Independent Bank placeholders (0.9.1-rc.8)

With built-in Bank Tags disabled, the independent Bank view now draws dimmed client-cache item sprites in requested layout positions that have no matching Bank widget. Repeated supply positions remain visible; intentional empty cells stay empty. Existing Bank placeholders or matched variants occupy their requested positions without a second sprite. Drawings are clipped to the Bank item container and follow its scroll offset. Only visible rows request cached sprites. Clearing the filter, switching to native Bank Tags or disabling the plugin removes this overlay. No synthetic withdraw widgets, item IDs, quantities or child indices are created or rewritten. No Wiki requests are involved. Native Bank Tags retains its own placeholder rendering.

Player acceptance: compare Bank Tags on/off with missing gear and repeated food, scroll through the layout, withdraw/deposit an owned item, then clear the filter and close/reopen the Bank. Confirm the sprites match the same requested layout and that nothing draws outside the Bank. Native behavior still requires user confirmation.


### Bank build and ownership correction (0.9.1-rc.9)

Bank Tags enabled and disabled now use the same Loadout Studio rendering path. Requested filtering runs after core Bank script callbacks. The pre-event of bank initialization/build schedules one coalesced refresh outside the script VM; layout arrangement and the readable title no longer depend on a post-event for a nested Bank script. The plugin no longer registers an ephemeral native tag or delegates its layout to native Bank Tags. Foreign active tags are still respected automatically; explicit Filter Bank switches ownership. Incomplete stored layouts gain missing item references in the runtime copy without overwriting the saved setup.

Offline regressions cover deferred/coalesced refresh without a post-event and completing an incomplete layout without mutating the draft, alongside existing placeholder, stock, codec and ownership tests. The reported live symptom is not claimed reproduced or resolved in-game: confirm the Bank Tags on/off matrix in the test client.


### RuneLite gray palette and mixed Wiki presets (0.9.1-rc.10)

The sidebar now uses RuneLite ColorScheme grays for backgrounds, surfaces, controls, borders, text and selections. Gold remains an accent for headings and selected or primary actions. Shared host UI defaults are unchanged. Documentation images were regenerated offline with example data.

A cached Maggot King Strategies source reproduced two concrete parsing defects: inventory-based styles omitted adjacent recommended Equipment, and recommendation-only styles disappeared when another style had a concrete Inventory. Adjacent source recommendations now populate Equipment when the style has no concrete Equipment template; explicitly empty concrete Equipment slots remain empty. Recommendation-only styles remain separately selectable with empty Inventory disclosed for manual completion. This preserves each style and its source-ranked alternatives. No network policy changed.


### Bank pause, menu safety and cosmetic armour (0.9.1-rc.11)

Clicking an ordinary Bank tab or choosing Show bank temporarily pauses the loadout without disabling its saved filter preference. Return to loadout resumes it; loading the Bank interface again releases the temporary pause. Clear filter remains the persistent off action. The worn-items submenu is built on MenuOpened rather than while script-generated menu entries are being added. Bank rebuilds and post-build work are deferred to tick end. A rebuild exception/assertion pauses the owned view and reports a technical failure instead of repeatedly attempting it.

A player reported an injected-client assertion/freeze in rc.10. Its exact native cause is not established by the obfuscated stack. The timing/menu corrections and containment above remain pending an in-game retest; offline success is not proof that the reported freeze is eliminated. Plugin Hub submission remains blocked on that acceptance.

Blood Torva and radiant Oathplate are explicitly recognised cosmetic counterparts for Bank membership and owned alternatives. Missing ornament-specific Equipment stats fall back to their known base piece; exact saved/exported IDs remain unchanged. Builder > Setup details now creates a section inline and selects it, reusing an existing case-insensitive name. Saving the setup remains explicit and unsaved Notes are retained.
