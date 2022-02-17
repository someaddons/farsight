package farsight;

import farsight.config.Configuration;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FarsightMod implements ModInitializer
{
    public static Configuration config;

    public static final String MODID  = "farsight";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize()
    {
        config = new Configuration();
        config.load();
        LOGGER.info(MODID + " mod initialized");
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
    }
}
