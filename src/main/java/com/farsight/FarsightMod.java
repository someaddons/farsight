package com.farsight;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

@Mod(modid = FarsightMod.MODID, acceptableRemoteVersions = "*", acceptedMinecraftVersions = "*", version = "1.6")
public class FarsightMod
{
    public static final String MODID  = "farsight";
    public static final Logger LOGGER = LogManager.getLogger();

    public static Random rand = new Random();

    public FarsightMod()
    {
        LOGGER.info("Farsight initialized");
    }
}
