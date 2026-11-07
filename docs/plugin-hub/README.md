# Plugin Hub submission

**Version:** 0.9.1-rc.18  
**Status checked:** 7 November 2026  
**Pull request:** [runelite/plugin-hub#19421](https://github.com/runelite/plugin-hub/pull/19421)  
**Status:** Open; review pending; not merged or published.

## Submitted source

The live manifest pins [`6549e29bcee70b6003d6e8a5bdbd98cb46a45ea6`](https://github.com/Pa5cal1/runelite-loadout-studio/tree/6549e29bcee70b6003d6e8a5bdbd98cb46a45ea6). [loadout-studio.example](loadout-studio.example) contains the same source pin and network warning. Documentation-only commits on the source repository may follow that pin without changing the submitted Java code.

The initial scanner feedback and formatting request have been addressed. The pinned source passes all 289 Java 11 tests, RuneLite Checkstyle and the Plugin Hub build. The later sidebar refactor remains separate development work.

See [the submission description](submission-description.md), [validation and limitations](../release-readiness.md) and [the targeted API check](../release-readiness.md#api-review).

## Review and updates

Keep fixes for this submission in the existing PR. For a source change, validate the new revision and update the live manifest's `commit=` value and the example manifest together. Documentation-only updates do not require a new source pin or plugin version.

Passing automated checks is not approval. Keep the candidate version until acceptance and any reviewer fixes are complete. Do not advertise Hub availability before publication.

Official process: [Plugin Hub instructions](https://github.com/runelite/plugin-hub#submitting-a-plugin).

## Network warning

The warning describes the plugin's actual Wiki traffic: the Wiki receives the connection's IP address, search queries and requested page titles. Gear and saved setups are not uploaded.

RuneLite supplies the installation dialog. The development launcher bypasses that Hub dialog; the plugin does not inject a duplicate. Wiki requests are available in the sidebar and occur after explicit user search/page actions, not merely on startup or when the sidebar opens.
