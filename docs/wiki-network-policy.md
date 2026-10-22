# Wiki request policy — 0.5.0-test

Requests start only after explicit Search/Enter, page selection, URL entry or reload. Startup, opening saved loadouts, typing and background time do not fetch Wiki content. Item sprites come from RuneLite ItemManager; no Wiki image service is called.

The MediaWiki API receives IP address, search query and requested page title. Gear, player identity, credentials and saved libraries are not uploaded. Pasted wikitext and saved setups work offline. The only remote endpoint is HTTPS oldschool.runescape.wiki/api.php; HTTP redirects remain disabled, with aliases resolved through API data.

A single queue limits requests to one in flight, a minimum two-second start interval and ten starts per rolling minute. At most twelve unique pending jobs are accepted. Duplicate requests share one operation. In-memory cache: search six hours, source pages seven days, at most 64 entries / four million characters. Nothing polls or refreshes after expiry; a later user request obtains fresh data. Caches do not survive plugin shutdown. Page resolution can require multiple bounded API calls. Manual reload may reuse cached data.

HTTP 429/503, Retry-After and API errors trigger a cooldown (at least sixty seconds, up to 24 hours when requested). Queued work is failed visibly; there is no automatic retry. Requests advertise LoadoutStudio/0.5.0 and the source repository, set maxlag=5 and cache hints, use 25-second timeouts, bound response sizes and cancel on shutdown.

These limits are conservative project choices, not published OSRS quotas. Wikimedia quotas must not be presented as OSRS Wiki quotas. The supplied Reddit thread is community discussion; it does not establish a general ban on user-triggered Wiki use or verify a particular plugin shutdown.

## Sources checked

- [MediaWiki API etiquette](https://www.mediawiki.org/wiki/API:Etiquette): identify clients, cache, limit concurrency and respect server limits.
- [Caching data](https://www.mediawiki.org/wiki/API:Caching_data).
- [Maxlag](https://www.mediawiki.org/wiki/Manual:Maxlag_parameter).
- [RuneLite policy](https://github.com/runelite/runelite/wiki/Rejected-or-Rolled-Back-Features): separate plugin policy from Wiki server etiquette.
- [User-supplied community discussion](https://www.reddit.com/r/2007scape/comments/1x5nu67/paid_plugins_should_not_be_allowed_on_runelite/).
- [Inventory Setups source](https://github.com/dillydill123/inventory-setups): chatbox slot editing and ten-row growing Notes.
- [RuneLite source](https://github.com/runelite/runelite): ChatboxItemSearch, ChatboxPanelManager and native Bank Tags title lifecycle.

Offline interceptor tests verify zero constructor traffic, request coalescing/cache hits, production request spacing and Retry-After blocking. User acceptance in RuneLite is still required.
