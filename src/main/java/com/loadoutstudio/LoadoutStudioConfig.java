/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(LoadoutStudioConfig.GROUP)
public interface LoadoutStudioConfig extends Config
{
	String GROUP = "loadout-studio";
	@ConfigItem(keyName = "wikiRequests", name = "Enable OSRS Wiki requests", description = "Search and fetch public pages from oldschool.runescape.wiki. Search text, requested page titles and your IP address are sent; loadouts are not uploaded.", warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers", position = 0)
	default boolean wikiRequests() { return false; }
}
