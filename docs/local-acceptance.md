# Testclient acceptance — 0.9.1-rc.12

The candidate passed 249 offline tests. Before stable publication, also complete the account and real Bank scenarios in [the release-readiness report](release-readiness.md#manual-acceptance-before-submission). A previous pass on another version does not cover newly changed native integrations.

Status: **pending user acceptance**. Automated tests and offline previews are separate from live RuneLite behavior. Only the user performs game interactions. Record RuneLite version, plugin version, Windows display scaling, test date and any active Bank-related plugins. Start with a library backup and disposable test setups.

Use scripts/Launch-Test.bat to start this test build. For a Jagex Account, follow [Using Jagex Accounts](https://github.com/runelite/runelite/wiki/Using-Jagex-Accounts). No Docker installation is needed. Do not treat an old, already-running client as the updated build.

Mark each row PASS or FAIL. For a failure, record the row ID, action, expected result, actual result and a screenshot. Stop release preparation for crashes, lost edits or changes to another profile/tag.

| ID | Action | Expected result | Result |
| --- | --- | --- | --- |
| T01 Startup and cleanup | Start, open the sidebar, disable/re-enable the plugin and restart the client | One sidebar icon, Library starts normally, no repeated errors; an owned filter/input is cleaned up without closing another plugin's input or tag | Pending |
| T02 Capture and persistence | Capture real gear, rename, add Notes and a section, Save, reopen and restart | Inventory, Equipment, Pouch/Quiver/Bolt Pouch where present, Spellbook and configured attack option are retained | Pending |
| T03 Favorite regression | Open a saved setup, change its Library star, return to Builder and Save; repeat with star removal, Save copy and unsaved recovery | Latest favorite state survives all paths; unsaved Notes remain intact | Pending |
| T04 Invalid input | Export a disposable setup, replace its fb boolean with an empty array and try importing; also cancel a valid import review | Clear inline rejection, no broken entry or partial replacement, existing library/draft unchanged | Pending |
| T05 Slot editing | Right-click items, use Update Slot from Search and quantity entry; inspect the Stack comparison submenu | Search and quantity use the chatbox; Ignore amount, Exactly, At least and At most work; cancel leaves the item unchanged | Pending |
| T06 Drag and drop | Drag a search result into Inventory, move/swap slots, try suitable Equipment and arrows/bolts in Quiver, then an incompatible drop | Compatible moves work, invalid drops preserve both old slots, sprites appear after loading | Pending |
| T07 Amounts | Set Inventory runes above 16k, Pouch runes to 16k and then 16001, and a populated slot to zero | Inventory accepts large stacks; Pouch amount entry rejects amounts above 16000; Inventory-to-Pouch drops clamp to 16000 and clear the source slot; explicit zero shows unlimited matching but still requires an item; counts remain inside cells | Pending |
| T08 Bank layouts | Apply Preset and both Zigzag directions, move cells in the active Loadout Studio layout, Save and restart; switch to an unrelated tag and clear the filter | Correct visible loadout name; Custom positions persist; placeholders/missing items stay represented; unrelated tags are not changed or closed | Pending |
| T09 Live comparison | Withdraw/deposit/equip items; change Spellbook, Pouch/Quiver and attack option; test Fuzzy and amount rules | Differences update without losing Notes focus; rules have the displayed meaning; no game action is automated | Pending |
| T10 Owned alternatives | Check before opening Bank, then with a known limited Bank. Select replacements needing more total items than observed, including duplicate slots | Unknown/stale Bank state is explained; Keep original is default; enabled stock check rejects shortages near Builder and keeps the draft; sufficient stock applies after review | Pending |
| T11 Alternative override and Undo | Turn Check replacement stock off in native Comparison settings, retry a deliberately oversized plan, then restore the default and test Undo | Override permits planning beyond amounts but selected items must still have been observed; Undo works until later draft edits make it unsafe; Save remains explicit | Pending |
| T12 Portable conversions | Round-trip a real Inventory Setup through its actual import action; test native Bank Tag and legacy layout in both directions | Correct name and representable items/layout; losses of worn slots, quantities, Spellbook and Notes in Bank Tags are disclosed; no JSON is pasted into Add New Setup name | Pending |
| T13 Copies and library actions | Copy installed plugin data, create/share a section, filter/sort, archive/restore and confirm or cancel permanent deletion of a disposable setup | Originals remain unchanged; names truncate with full tooltips; sections/search work; trash only deletes after confirmation; Cancel is safe | Pending |
| T14 Wiki cache and errors | Search an activity, choose a preset, revisit after restart, explicitly Refresh, then use a cached source offline | Choices appear, first choices are reviewable starting points, source date/cache status is clear, failures are readable and existing choices survive failed refresh. Typing alone uses local suggestions | Pending |
| T15 Profiles and recovery | Change profile/character during capture, search, import/chatbox editing and with an unsaved draft | Late callbacks never change another scope; matching recovery restores the correct Notes/items/favorite; saved libraries stay separate | Pending |
| T16 Independence and presentation | Disable optional Inventory Setups and standalone Bank Tag Layouts with built-in Bank Tags both enabled and disabled. Inspect 220/260 px and normal display scaling | Own Library/Builder remain usable; native settings apply; buttons, fields, menus, feedback, long titles, Notes and counts remain readable with one outer Notes scrollbar | Pending |

Network checks for zero startup/typing/background HTTP, coalescing, rate limits, cache persistence and cooldown are automated offline. Ordinary UI observation cannot prove zero HTTP; do not mark traffic monitoring complete from the absence of a visible error. The live cache/error behavior in T14 still needs user acceptance.

## After acceptance, before Plugin Hub submission

- Resolve every failing row, rerun the relevant regressions and record the accepted version/commit.
- Choose final display name/author and include real screenshots only after user verification.
- Review bundled asset provenance and notices.
- Choose a controlled public source repository/commit with user authorization; this task does not change visibility.
- Finalize the prepared Hub manifest with a full reviewed commit and the precise Wiki network warning. Keep the example placeholder until that release commit is selected.
- Create the actual Plugin Hub pull request separately and pass its checks/review. Local test success is not approval by RuneLite.

## Targeted checks after the player feedback update

- Open Vorkath, Duke and Fractured Archive; activity suggestions omit table/miniquest entries and automatically prefer Strategies with main-page fallback. Cache reuse/save is automatic; there is no Use cache control. Verify source names are meaningful.
- Inspect Ultor ring tooltip ID 28307, including an older saved Wiki setup. Known obsolete IDs are corrected in a draft and kept only after Save; Custom positions should survive. Unknown expressions still allow known items to filter.
- Click a question-mark slot or Update Slot from Search and type immediately without clicking the game area first. Quantity entry should be empty.
- Check the item sprite while dragging on Windows; test Inventory runes/arrows to Pouch/Quiver. A 16001 rune stack puts 16000 in the Pouch with no remainder in Inventory. A different rune already in the target swaps into the freed source Inventory slot; test a full Inventory too. An occupied target with the same rune must not create another Inventory stack.
- Verify automatic compact alternatives show at most the configured number in source/family order, keep Notes caret/text, respect manual collapse, and never auto-apply a replacement.
- Exercise all five setup icons, including highlighting off and a custom color, and verify the saved attack option is visible. Headers, ordinary actions and Wiki choices must be distinct.
- Save, then Shift-right-click and review an ALL-slots update; test search and live-container sources and Undo. Duplicate saves/imports must use numbered names instead of being skipped.
- With Bank Tags disabled, test ordinary Bank filtering, Preset/Zigzag/Custom positions, manual layout drag, clear, close/reopen and plugin disable. Withdraw manually and confirm the correct real stack/slot is used. Toggle Bank Tags while a Loadout filter is active and test again. This integration is not proven by offline geometry tests.
- Open From installed plugins with long names and resize the client. Sidebar controls must fit without requesting a larger window. Recheck the reported in-game visual distortion; no canvas resize or graphics-setting change is made by this plugin.

User feedback confirmed many 0.7.3 behaviors, including startup, persistence, favorites, invalid-import feedback, basic editing, comparison, native layouts and portable data import. Changed 0.8.0 paths above still need fresh acceptance; prior confirmations are not claimed as live tests of this build.

## Aquanites ranked alternatives

Open a fresh Wiki draft from the cached Aquanites source, not an older saved copy without choices. Melee Slash/Stab body choices follow Oathplate, Torva, Bandos, Blood Moon, Fighter torso, Sunfire and Proselyte; only observed items appear, capped by settings. Ranged uses its separate Masori/Crystal/Karil/blessed/black-dhide list. Confirm unlisted same-slot gear is absent, original Inventory is retained, and source conditions remain in Notes. No extra requests should be made just to determine alternatives.

## Custom alternatives after the workflow review

In a custom Bank layout, replace a missing item with an observed alternative. If no other slot still requires the old item, the replacement should occupy the old custom positions; Undo restores the original layout. If another slot still requires the old item, its positions remain and the new item is added. Save/reopen to check persistence. A missing item should have its known name above the alternatives selector, even when absent from the Bank observation.

## Export formatting and native compressed codes

In Export, select Inventory Setup JSON and toggle Pretty Print: only whitespace changes, and Copy must equal the preview. Compress is disabled for this target. Select Loadout Studio JSON and enable Compress: Pretty Print is disabled, a loadoutstudio:gz1: code appears and can be pasted back into this plugin's Import. Check names, Unicode Notes, source choices, quantities and custom positions. Disable Compress to restore JSON. With either Bank Tag target both options are disabled and the code must retain the normal banktags or banktaglayoutsplugin prefix. Malformed/truncated/oversized compressed imports must leave the saved library unchanged.

## Permanent support control

Confirm Show coffee button no longer exists in plugin settings. Buy a coffee remains visible after changing Sidebar preferences and after restarting, including when it was previously hidden. No browser opens until the button is clicked.

## Local diagnostics and Bank context menu

- Right-click Show worn items and confirm Loadout Studio has its own submenu alongside Inventory Setups. Choose Preset and both Zigzag directions, review the opened sidebar and Save/reopen. Old menu actions from another draft/profile must do nothing.
- With Inventory Setups filtering a selected setup, open ours: automatic filtering must yield with a visible explanation. Choose our Filter Bank/Auto Layout to switch, then return to the other plugin and clear ours. Its tag, saved setups and menu entries must remain intact. Enable Inventory Setups' Require Active Panel for Filtering manually if needed; no foreign settings should change automatically. Test both shared native and standalone Bank paths and manually verify withdrawals.
- Open Library tools > Diagnostics / report problem. Review before Copy; opening the GitHub form must not attach/send the report. Reset must clear counters. Disabling Record local diagnostics must clear and stop collection, with features still usable. Restart should start a new session. Normal invalid input must be classified as validation, not an unexpected bug.
- Check all new native settings and the always-visible coffee control. Compare the documentation previews with the real client; previews do not prove native behavior.

## Distinct sidebar style

Check the warm charcoal/gold theme at normal and increased display scaling, 220/260 px sidebar widths, every route and the report dialog. Navigation labels and the permanent support button must fit with their icons. Verify keyboard focus, long-title ellipsis, popup/dropdown selection, checkboxes and selected highlights. Item sprites/counts and slot positions must retain their previous behavior. RuneLite native settings and other plugins should keep their original style. Test Save, Wiki selection, drag/drop and Copy once in the real client after the visual update.


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


### Settings applicability correction (0.9.1-rc.12)

Audited all 23 configuration items. New Wiki drafts now receive creation defaults, including highlighting, color, missing-only, filtering, Fuzzy, amount rule and layout. Previously only blank/captured setup creation used this policy. Explicit imports and saved setups retain their own options; defaults are not global overrides. New manual/catalogue/captured-slot insertions receive item defaults while replacements retain existing slot rules. Entering an explicit amount can enable exact comparison; zero remains unlimited.

Live sidebar preferences rebuild the affected controls without losing Notes/caret. Automatic alternative timers and stale callbacks are invalidated when preferences change. Auto-find and auto-expand labels clarify automatic versus manual operation without changing configuration keys. Respect-other-filter changes schedule a safe live reassessment. Freshness duration is read at the next user request, with no background traffic. Existing native Bank freeze acceptance remains open; the settings audit does not close that blocker.


## Settings applicability audit — rc.12

Code paths and offline boundaries below are checked separately from native client acceptance. Defaults intentionally do not rewrite imported or saved setups.

| Setting | Scope and evidence |
| --- | --- |
| Refresh source after days (`wikiFreshDays`) | Live supplier on next source request; 30-to-1-day change tested without background requests. |
| Auto-find alternatives (`autoAlternatives`) | Automatic local requests/timer only; manual refresh remains available; stale results rejected. |
| Alternatives per item (`maxAlternatives`) | Choice cap; updated on auto-refresh or explicit manual refresh when automation is off. |
| Auto-expand alternatives (`autoShowAlternatives`) | Automatic expansion only; user-triggered expansion remains available. |
| Default highlighting (`defaultHighlight`) | New setups including Wiki; disabled border tested in the real Wiki-open route. |
| Default highlight color (`defaultHighlightColor`) | Initial setup color including Wiki; does not recolor saved setups. |
| Default missing items only (`defaultMissingOnly`) | Initial Inventory missing/order rule including Wiki. |
| Empty quantity input (`emptyQuantityInput`) | Read at amount entry; both empty and previous-value states tested. |
| Show attack option (`showAttackOption`) | Live display toggle, preserving the stored attack option and Notes/caret. |
| Record local diagnostics (`localDiagnostics`) | Live off clears and stops recording; verified through the sidebar report. |
| Bank layout menu (`bankLayoutMenu`) | Auto Layout submenu; native menu retest required. |
| Respect other Bank filters (`respectOtherBankFilters`) | Live reassessment at tick end; explicit takeover remains allowed; native coexistence retest required. |
| Default layout (`defaultLayout`) | New blank, captured and Wiki setups; existing layouts remain unchanged. |
| Zigzag bottom row first (`defaultZigzagBottom`) | Direction for new Zigzag layouts; no effect on Preset. |
| Default Filter Bank (`defaultFilterBank`) | Initial setup flag, now including Wiki creation; not a global off switch. |
| Default Fuzzy (`defaultFuzzy`) | New generated items and empty-slot insertions; existing/imported rules retained. |
| Default amount rule (`defaultStackRule`) | New generated items and empty-slot insertions; explicit amount entry is a slot edit. |
| Capture attack option (`captureAttackOption`) | Capture gear only; display and source-provided options are independent. |
| Ignore Ammo quantities (`ignoreAmmoQuantities`) | Live comparison/export policy; Rune Pouch amounts remain checked. |
| Check replacement stock (`checkAlternativeStock`) | Read on Apply; enabled/disabled action paths tested with insufficient stock. |
| Library order (`libraryOrder`) | Live Library sorting; item/setup contents retained. |
| Toggle Bank filter (`filterHotkey`) | RuneLite keybind supplier while Bank is open; native keyboard retest required. |
| Bank setup shortcuts (`bankMenu`) | Saved setup shortcuts in the next opened Bank menu; not every Bank action. |
