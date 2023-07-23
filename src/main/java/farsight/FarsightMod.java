package farsight;

import farsight.config.Configuration;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FarsightMod implements ModInitializer
{
    private static Configuration config;

    public static final String MODID  = "farsight";
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitialize()
    {
        LOGGER.info(MODID + " mod initialized");
    }

    public static Configuration getConfig()
    {
        if (config == null)
        {
            config = new Configuration();
            config.load();
        }

        return config;
    }
}
