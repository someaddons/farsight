package com.farsight;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(modid = FarsightMod.MODID)
public class FarsightMod
{
    public static final String MODID  = "farsight_view";
    public static final Logger LOGGER = LogManager.getLogger();

    public static Random rand = new Random();

    public FarsightMod()
    {

    }
}
