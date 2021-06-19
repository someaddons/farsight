package com.farsight.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfiguration
{
    public final ForgeConfigSpec ForgeConfigSpecBuilder;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Config category");

        // Escapes the current category level
        builder.pop();
        ForgeConfigSpecBuilder = builder.build();
    }
}
