# Local acceptance — 0.6.0-test

Status: automated fixtures are separate from live RuneLite acceptance. The client has not been restarted or operated by the coding agent. Only the user should perform game interactions. Use scripts/Launch-Test.bat when ready and follow the README Jagex Account instructions.

| Area | User test | Expected result |
| --- | --- | --- |
| Startup | Open/restart the test client without searching | No Wiki HTTP requests or background refresh; Library remains the main view |
| Persistent cache | Search a boss, open its page, close/reopen client and select the same page | Cached setup choices and original fetch time; no page fetch |
| Local suggestions | Type a previously cached boss name | Cached suggestions appear without HTTP; Search Wiki finds new titles on demand |
| Fresh source | Choose Refresh Wiki | Explicit confirmation, bounded request, new fetch time; cached choices survive a failure |
| Offline | Disconnect network and open an already cached source | Stored page remains usable, even if marked stale |
| Inventory Setups | Import icon → From installed plugins; select a setup and Save copies | Copy has name, slots, quantities, Notes, flags, available layout, favorite and section; original remains unchanged |
| Bank Tags | Copy a native tag and a legacy layout | Members and positions retained; missing worn slots/amounts/Spellbook clearly disclosed |
| Library safety | Copy the same setup twice; select too many or malformed entries | Identical copies skipped; validation failure causes no partial library replacement |
| Recovery | Edit name, item and Notes without Save; restart and Restore draft | Exact draft reappears only in the matching RuneLite profile/character; Save clears recovery |
| Profile changes | Switch RuneLite profile/character during import or chatbox editing | Old replies cannot update the new scope; recoveries remain separate |
| Comparison | Toggle Fuzzy, each stack mode, unordered highlight and Ammo ignore | Differences follow the selected rules; no slot is matched twice |
| Wiki choices | Open a page with unresolved items or only ranked gear | Guided chatbox/alternative choice; conditions retained; no invented quantities |
| Quantity | Use Inventory runes 200k, Pouch 16001 and explicit zero | Inventory accepted; Pouch over 16k rejected; zero ignores amount but requires the item |
| Drag and drop | Move/swap items, drag catalogue entry, try incompatible Equipment/Quiver | Compatible moves only; original draft preserved on invalid drops |
| Export round-trip | Copy receiving-plugin code through its proper import action | Names and representable data agree; Bank Tag losses are disclosed |
| Sidebar | Inspect 220/260 px widths, Notes and Rune Pouch labels | Buttons readable; Notes only outer scrolling; counts inside cells |

Record the RuneLite version, scenario and observed result when reporting a failure. Do not include credentials, sessions or the .runelite directory in reports or GitHub uploads.

## First-choice defaults (0.6.1)

Open a Wiki recommendation setup: its first listed items should appear immediately. In Vorkath, Cheap food should become Manta ray and Cheap prayer should become Prayer potion(4), with original Inventory positions retained. Open an older unresolved saved copy and check that known items are resolved locally; Save should persist these defaults. Replace a slot through chatbox search and reopen it: the chosen positive Item ID must remain unchanged. Missing quantities must still request an amount. No additional Wiki request should occur when opening a saved copy.

## Archive actions (0.6.2)

Archive a disposable setup with the box icon, then enable Archived. Confirm that restore and red trash are distinct and fit on the same row. Restore should return the setup to the active list. Archive again, click trash and choose Cancel: it must remain. Click trash again, confirm Delete permanently and verify it disappears after reopening the plugin. Check that other setups and sections remain. For an open archived setup, the warning must explain discarding its Builder draft, and confirmation must close that draft. Do not use a setup you need for this acceptance test.

## Player workflow review — 0.6.3-test

1. Start with an empty Library: use Find Wiki content, Capture gear and New setup. Check there is no Wiki traffic until an explicit search/page request.
2. Collapse a section, search for one of its setups, then clear the search. The matching setup is visible during search; the section returns to its previous collapsed state. Repeat with Favorites.
3. Apply and clear the Bank filter from the fixed Builder header. Confirm the checkbox and saved preference agree. Change Preset/Zigzag in the existing layout disclosure.
4. Use Compare above the loadout. Confirm actual differences and sprites in RuneLite; these require manual game-client acceptance.
5. Make a query that matches nothing and use Clear filters to return to the active Library.
6. Verify Capture gear opens a new draft as usual. Changes made while a delayed capture is pending must be retained.

## Gearing and alternatives — 0.7.0-test

1. Disable/uninstall the two optional hub plugins in a test profile. Create, capture, save, reopen and export in Loadout Studio. Keep built-in Bank Tags enabled.
2. Open the Bank, load a setup with unowned gear and check My gear alternatives. Verify the observed owned choices, no auto-selection, explicit apply, Save and Undo. Repeat before opening the Bank, after closing it, after withdrawing gear and across profile changes. Zero-quantity placeholders must not count.
3. Confirm source choices, same-slot-only warnings and no arbitrary rune substitutions. Review requirements, combat style, ammo and spells after choosing a replacement.
4. Move items in the active native layout, Save, switch setups and restart. Positions must survive; another tag must stay intact. Test Preset/Zigzag regeneration explicitly.
5. Withdraw/deposit/equip items and change Spellbook, Pouch/Quiver contents and attack style. Confirm automatic differences, tooltip reasons, no Notes focus loss and unordered extra-item behavior.
6. Exercise Bolt Pouch capture and four slots, including enchanted/dragon bolts, and attack-option capture/ignore/export.
7. Test Recent/Favorites-first/Manual ordering, right-click move and item:/notes: search.
8. Upgrade an exact item in selected saved setups, inspect unaffected setups and Undo. Later edits must prevent unsafe Undo.
9. Share a selected section, import it into a library with existing setups, and confirm independent copies. Verify the old whole-library backup restore flow remains separate.
10. Configure a Bank-only filter hotkey and use the worn-items menu shortcuts. Confirm no game action is automated and the hotkey does nothing outside the Bank.

## Plugin preferences — 0.7.1-test

Open the native Loadout Studio configuration cog. Confirm the five collapsible groups, existing hotkey/menu values and live Sidebar/Comparison preferences. Change new-loadout defaults, capture/create a new setup and verify them; existing saved and imported setups must retain their own layout/rules. Sort through config, then right-click move a library row and verify Manual persists. Change preferences while editing Notes; text/focus must remain. Use the single fixed Bank toggle and Compare action; Save copy is in Setup details.

## Styling consistency — 0.7.2-test

- At normal sidebar width, compare Library, Builder, Wiki and Export fonts, field backgrounds, spacing and buttons. Resize the sidebar and check that labels remain readable.
- Right-click an Inventory or Equipment slot and open Stack comparison. Check menu text, disabled actions, hover feedback and item selection through the chatbox. Right-click a saved setup and check its ordering menu.
- Open import, share and item-upgrade review dialogs; check readable dark form content and native confirmation buttons. Cancel each review and confirm saved setups stay unchanged.
