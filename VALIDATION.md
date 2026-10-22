# Local validation — 0.2.0-test — 2026-10-22

- Docker Java 11: `sh gradlew test shadowJar jar --no-daemon --offline` succeeded.
- JUnit: 27 tests, zero failures/errors. Covers portable setup round-trip, native and legacy Bank Tag conversion, quantities and limits, malformed URLs, Wiki search JSON, multiline Wiki template pairing, library backups and section deletion, Bank Preset/Zigzag positions in both directions, exact/Fuzzy filter membership, delayed activation, owned-tag cleanup, size limits, saved preferences, and native Swing controls/profile reset with a fake host.
- Live public OSRS Wiki OpenSearch request for `Vorkath`: HTTP 200; returned Vorkath and Vorkath/Strategies among the article titles. This checks endpoint accessibility outside RuneLite; it does not prove the in-client network path.
- Plugin-only JAR class version 55 (Java 11); 24 classes; no Plugin service loader registration. Static source scan found no subprocess execution, reflection, native memory access, dynamic code loading or alternate HTTP stack.
- Narrow sidebar was rendered without a game client and inspected for Equipment/Inventory columns, search controls and scroll behavior. These screenshots use the test environment's Swing theme, not a running RuneLite client.
- `git diff --check` passed. Build outputs and test-home are ignored. Source repository: https://github.com/Pa5cal1/runelite-loadout-studio (private). No Plugin Hub submission performed.

Pending manual acceptance: actual startup/login, item catalogue and sprites, capturing Rune pouch/Quiver/Spellbook values, character/profile persistence, receiving-plugin imports, Wiki requests from inside RuneLite, and live Bank filtering/layout rendering. Test automatic filtering on Bank opening, both layouts/directions, Clear filter, preserving a normal Bank Tag, and cleanup at logout/profile switch/disable. The development launcher isolates its data in test-home. Follow the acceptance sequence in README.md.

Bank filtering now uses the built-in Bank Tags dependency, an in-memory custom tag, and the native layout engine. Layout choices are saved per setup; generated positions are exported. Existing user tags are not edited. This remains a prototype: gear comparison does not yet implement full fuzzy rules, and ranked Wiki recommendation tables still require a choice editor.
