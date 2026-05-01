package com.farsight.mixin;

import com.farsight.FarsightMod;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SodiumConfigBuilder.class)
public class SodiumGameOptionPagesMixin
{
    @ModifyConstant(method = "buildGeneralPage", constant = @Constant(intValue = 32), remap = false, require = 0)
    private static int initCompat(final int constant)
    {
        return FarsightMod.config.getCommonConfig().maxRenderDistance;
    }
}
