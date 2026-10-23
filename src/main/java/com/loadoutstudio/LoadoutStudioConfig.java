/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup(LoadoutStudioConfig.GROUP)
public interface LoadoutStudioConfig extends Config
{
	String GROUP = "loadout-studio";
    @net.runelite.client.config.ConfigItem(keyName="filterHotkey",name="Toggle Bank filter",description="Toggle the active Loadout Studio filter while the Bank is open")
    default net.runelite.client.config.Keybind filterHotkey(){return net.runelite.client.config.Keybind.NOT_SET;}
    @net.runelite.client.config.ConfigItem(keyName="bankMenu",name="Bank setup shortcuts",description="Show up to five favorite or recent setups on the Bank worn-items button")
    default boolean bankMenu(){return true;}
}
