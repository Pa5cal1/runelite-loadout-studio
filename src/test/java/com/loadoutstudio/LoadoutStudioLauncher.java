/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;

import java.nio.file.Paths;
import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Development launcher only, never packaged as the Plugin Hub plugin. */
public final class LoadoutStudioLauncher
{
	public static void main(String[] args) throws Exception
	{
		// Separate cache, login session and configuration from the normal RuneLite installation.
		System.setProperty("user.home", Paths.get("test-home").toAbsolutePath().toString());
		ExternalPluginManager.loadBuiltin(LoadoutStudioPlugin.class);
		RuneLite.main(args);
	}
}
