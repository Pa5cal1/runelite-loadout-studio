# Plugin Hub submission manifest

`loadout-studio.example` is a preparation template, not a submitted or valid release manifest. Publish and review the intended source commit, replace the commit placeholder with its complete hash, then use the manifest as `plugins/loadout-studio` in a Plugin Hub submission. The current repository must be made public before review.

The `warning` line describes Loadout Studio's actual network behavior: queries and requested page titles go to OSRS Wiki, which receives the connection's IP address. Gear, saved setups and account credentials are not uploaded. The user explicitly rejected a generic warning suggesting account-data submission.

RuneLite's [PluginHubPanel](https://github.com/runelite/runelite/blob/master/runelite-client/src/main/java/net/runelite/client/plugins/config/PluginHubPanel.java) adds the installation title, confirmation question, warning icon and Yes/No buttons. Do not duplicate that question in the warning line. Wiki requests are always available in the sidebar; there is no opt-in checkbox. No requests are sent merely by opening the plugin.

The development launcher bypasses Plugin Hub installation, so it does not display the Hub installation dialog. No duplicate install dialog is injected by the plugin.
