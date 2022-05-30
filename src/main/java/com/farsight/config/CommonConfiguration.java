package com.farsight.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class CommonConfiguration
{
    public final ForgeConfigSpec          ForgeConfigSpecBuilder;
    public final ForgeConfigSpec.IntValue maxchunkdist;

    protected CommonConfiguration(final ForgeConfigSpec.Builder builder)
    {
        builder.push("Config category");

        builder.comment("Max distance at which chunk data is kept on the client. default = 32");
        maxchunkdist = builder.defineInRange("maxchunkdist", 32, 8, 512);

        // Escapes the current category level
        builder.pop();
        ForgeConfigSpecBuilder = builder.build();
    }
}
