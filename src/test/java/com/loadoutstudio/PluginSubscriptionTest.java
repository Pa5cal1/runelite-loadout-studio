/* SPDX-License-Identifier: BSD-2-Clause */
package com.loadoutstudio;
import net.runelite.client.eventbus.EventBus;
import org.junit.Test;

public class PluginSubscriptionTest {
    @Test public void nativeEventBusAcceptsEveryPluginSubscription(){
        EventBus bus=new EventBus();LoadoutStudioPlugin plugin=new LoadoutStudioPlugin();
        bus.register(plugin);bus.unregister(plugin);
    }
}
