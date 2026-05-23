package com.farsight.mixin;

import com.farsight.FarsightMod;
import com.farsight.preview.PreviewRegionFileManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickHookMixin
{
    @Unique
    private int tickCounter = 0;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(final CallbackInfo ci)
    {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && tickCounter++ > 100 && FarsightMod.config.getCommonConfig().enableChunkPreview)
        {
            tickCounter = 200;
            PreviewRegionFileManager.onClientTick();
        }
    }
}
