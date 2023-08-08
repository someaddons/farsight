package farsight;

import com.cupboard.config.CupboardConfig;
import farsight.config.CommonConfiguration;
import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FarsightMod implements ModInitializer
{
    public static final String                              MODID  = "farsight";
    public static final Logger                              LOGGER = LogManager.getLogger();
    public static       CupboardConfig<CommonConfiguration> config = new CupboardConfig<>(MODID, new CommonConfiguration());

    @Override
    public void onInitialize()
    {
    }
}
