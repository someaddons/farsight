package farsight.mixin;

import farsight.FarsightMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SodiumGameOptionPages.class)
public class SodiumGameOptionPagesMixin
{
    @ModifyConstant(method = "lambda$general$0", constant = @Constant(intValue = 32), remap = false, require = 0)
    private static int initCompat(final int constant)
    {
        return FarsightMod.config.getCommonConfig().maxRenderDistance;
    }

    @ModifyConstant(method = "lambda$general$3", constant = @Constant(intValue = 32), remap = false, require = 0)
    private static int initCompat2(final int constant)
    {
        return FarsightMod.config.getCommonConfig().maxRenderDistance;
    }
}
