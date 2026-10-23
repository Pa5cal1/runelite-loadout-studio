# Wiki request policy — 0.6.2-test

Requests start only after explicit Search/Enter, page selection, URL entry or reload. Startup, opening saved loadouts, typing and background time do not fetch Wiki content. Typing may read local cached suggestions on a worker; it never performs HTTP. Item sprites come from RuneLite ItemManager; no Wiki image service is called.

The MediaWiki API receives IP address, search query and requested page title. Gear, player identity, credentials and saved libraries are not uploaded. Pasted wikitext and saved setups work offline. The only remote endpoint is HTTPS oldschool.runescape.wiki/api.php; HTTP redirects remain disabled, with aliases resolved through API data.

A single queue limits requests to one in flight, a minimum two-second start interval and ten starts per rolling minute. At most twelve unique pending jobs are accepted. Duplicate requests share one operation. Persistent cache: up to 64 entries / four million response characters, written atomically through RuneLite Filepath under this plugin's data directory. The JSONL file is bounded to eight MB with per-entry limits; oversized entries may remain session-only. Nothing scans disk at startup: the first user search/page/local-suggestion request loads it on the Wiki worker. Responses are schema-checked before caching; malformed entries are ignored.

Search responses are fresh for six hours. Page sources become stale after seven days, but are still served from disk until the user explicitly chooses Refresh Wiki. A date and cached/stale indicator are shown. Use cache reuses cached source. Refresh bypasses this plugin's cache and sends Cache-Control: no-cache; it still obeys the queue, cooldown and request limits. Failures retain existing setup choices and prior cache data. Expired search results may be used if a user-triggered request fails. There are no expiry timers, prefetches or periodic refreshes.

Page resolution may require several bounded API calls. The same public Wiki cache can be reused across characters; it contains public source responses and user-entered Wiki search queries, never gear, account data or credentials. Write failures are visible and leave normal in-session usage available.

HTTP 429/503, Retry-After and API errors trigger a cooldown (at least sixty seconds, up to 24 hours when requested). Queued work is failed visibly; there is no automatic retry. Requests advertise LoadoutStudio/0.6.2 and the source repository, set maxlag=5 and cache hints, use 25-second timeouts, bound response sizes and cancel on shutdown.

These limits are conservative project choices, not published OSRS quotas. Wikimedia quotas must not be presented as OSRS Wiki quotas. The supplied Reddit thread is community discussion; it does not establish a general ban on user-triggered Wiki use or verify a particular plugin shutdown.

## Sources checked

- [MediaWiki API etiquette](https://www.mediawiki.org/wiki/API:Etiquette): identify clients, cache, limit concurrency and respect server limits.
- [Caching data](https://www.mediawiki.org/wiki/API:Caching_data).
- [Maxlag](https://www.mediawiki.org/wiki/Manual:Maxlag_parameter).
- [RuneLite policy](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features): separate plugin policy from Wiki server etiquette.
- [User-supplied community discussion](https://www.reddit.com/r/2007scape/comments/1x5nu67/paid_plugins_should_not_be_allowed_on_runelite/).
- [Inventory Setups source](https://github.com/dillydill123/inventory-setups): chatbox slot editing and ten-row growing Notes.
- [RuneLite source](https://github.com/runelite/runelite): ChatboxItemSearch, ChatboxPanelManager and native Bank Tags title lifecycle.

Offline interceptor tests verify zero constructor traffic or disk reads, reuse across WikiClient instances without HTTP, local-only suggestions, explicit refresh bypass, stale-page use, corrupt cache handling, atomic Filepath replacement, request coalescing, production spacing and Retry-After blocking. User acceptance in RuneLite is still required.

First-choice item resolution is entirely local. Bundled first-listed defaults for [Cheap food](https://oldschool.runescape.wiki/w/Template:Cheap_food) and [Cheap prayer](https://oldschool.runescape.wiki/w/Template:Cheap_prayer) were checked once during development; no template expansion, market-price or image request is added to the plugin.
