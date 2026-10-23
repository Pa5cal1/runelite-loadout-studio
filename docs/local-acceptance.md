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
