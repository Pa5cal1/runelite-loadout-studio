# Plugin Hub submission manifest

Current preparation: **0.9.1-rc.13**, checked on **25 October 2026**. The candidate is not submitted or approved. See [the proposed listing and description](submission-description.md) and [release gates](../release-readiness.md).

## Submission sequence

1. Complete and record the manual acceptance pass on the exact candidate. Keep source code and version metadata in sync.
2. Approve public access to the source repository before submission; this preparation does not change repository visibility.
3. Choose the stable version after acceptance, normally **1.0.0**, then run the source checks and build again. The root `icon.png` is a bundled 32 by 32 pixel PNG, within the Hub's 48 by 72 maximum.
4. Fork `runelite/plugin-hub`, create a branch and add `plugins/loadout-studio` containing the source repository, full reviewed commit hash and proposed network warning. Use the commit from the source that will actually be reviewed.
5. Open one pull request with the prepared description and acceptance evidence. Respond to Hub checks and reviewers; update the same PR's pinned commit when fixes are required.
6. Wait for review and merge. Do not advertise the plugin as approved or available until it actually appears in the Hub.

For later updates, change the manifest's commit to the tested source revision; changing only the plugin version is insufficient. Official instructions: [Plugin Hub README](https://github.com/runelite/plugin-hub/blob/master/README.md). The [review scope](https://github.com/runelite/runelite/wiki/Plugin-Hub-Review) is security and rule compliance, not functional acceptance.

## Prepared manifest

`loadout-studio.example` is a preparation template, not a submitted release manifest. Its pin identifies the reviewed candidate source; replace it after any further source change or stable-version update before using the manifest as `plugins/loadout-studio` in a Plugin Hub submission. The current repository must be made public before review.

The `warning` line describes Loadout Studio's actual network behavior: queries and requested page titles go to OSRS Wiki, which receives the connection's IP address. Gear, saved setups and account credentials are not uploaded. The user explicitly rejected a generic warning suggesting account-data submission.

RuneLite's [PluginHubPanel](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/config/PluginHubPanel.java) adds the installation title, confirmation question, warning icon and Yes/No buttons. Do not duplicate that question in the warning line. Wiki requests are always available in the sidebar; there is no opt-in checkbox. No requests are sent merely by opening the plugin.

The development launcher bypasses Plugin Hub installation, so it does not display the Hub installation dialog. No duplicate install dialog is injected by the plugin.
