package com.farsight.mixin;

import com.farsight.ClientChunkHandler;
import com.farsight.FarsightMod;
import com.farsight.preview.PreviewRegionFileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class ClientTickHooks
{
    @Shadow
    private long clientTickCount;

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(final CallbackInfo ci)
    {
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().level != null && clientTickCount > 100 && FarsightMod.config.getCommonConfig().enableChunkPreview
        && !Minecraft.getInstance().isPaused())
        {
            PreviewRegionFileManager.onClientTick();
        }
    }

    @Inject(method = "Lnet/minecraft/client/Minecraft;updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void unOnload(final ClientLevel level, final boolean stopSound, final CallbackInfo ci)
    {
        if (level == null)
        {
            ClientChunkHandler.onUnloadWorld();
        }
    }
}
