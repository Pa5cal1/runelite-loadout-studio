/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;

@ConfigGroup(LoadoutStudioConfig.GROUP)
public interface LoadoutStudioConfig extends Config
{
	String GROUP = "loadout-studio";
}
