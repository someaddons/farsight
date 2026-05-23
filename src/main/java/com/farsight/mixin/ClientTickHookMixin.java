package com.farsight.mixin;

import com.farsight.FarsightMod;
import com.farsight.preview.PreviewRegionFileManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickHookMixin
{
    @Shadow private long clientTickCount;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(final CallbackInfo ci)
    {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && clientTickCount > 100 && FarsightMod.config.getCommonConfig().enableChunkPreview)
        {
            PreviewRegionFileManager.onClientTick();
        }
    }
}
