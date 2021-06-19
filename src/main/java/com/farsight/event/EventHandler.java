package com.farsight.event;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Forge event bus handler, ingame events are fired here
 */
public class EventHandler
{
    @SubscribeEvent
    public static void onWorldTick(final TickEvent.WorldTickEvent event)
    {

    }
}
