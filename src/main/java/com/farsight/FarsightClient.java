package com.farsight;

import com.farsight.compat.SodiumCompat;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;

@Environment(EnvType.CLIENT)
public class FarsightClient implements ClientModInitializer
{

    @Override
    public void onInitializeClient()
    {
        if (FabricLoader.getInstance().isModLoaded("sodium"))
        {
            SodiumCompat.init();
        }
    }
}
