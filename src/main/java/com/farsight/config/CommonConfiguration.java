package com.farsight.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfiguration
{
    public final ForgeConfigSpec          ForgeConfigSpecBuilder;
    public final ForgeConfigSpec.IntValue maxchunkdist;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Config category");

        builder.comment("The distance at which chunks are kept in memory, regardless of whether the server unloads them. default = 32, maximum = 512");
        maxchunkdist = builder.defineInRange("maxchunkdist", 32, 8, 512);

        // Escapes the current category level
        builder.pop();
        ForgeConfigSpecBuilder = builder.build();
    }
}
