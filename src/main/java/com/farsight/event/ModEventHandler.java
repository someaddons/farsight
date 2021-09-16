package com.farsight.event;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ModEventHandler
{
    @SubscribeEvent
    public static void onConfigChanged(ModConfigEvent event)
    {
    }
}
