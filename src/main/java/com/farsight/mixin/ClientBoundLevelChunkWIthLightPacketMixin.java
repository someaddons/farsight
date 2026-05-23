package com.farsight.mixin;

import com.farsight.FarsightMod;
import com.farsight.preview.PreviewRegionFileManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(ClientboundLevelChunkWithLightPacket.class)
public class ClientBoundLevelChunkWIthLightPacketMixin
{
    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void save(final FriendlyByteBuf input, final CallbackInfo ci)
    {
        if (Thread.currentThread().getName().toLowerCase(Locale.ROOT).contains("netty") && FarsightMod.config.getCommonConfig().enableChunkPreview)
        {
            PreviewRegionFileManager.saveChunkPacket((ClientboundLevelChunkWithLightPacket) (Object) this);
        }
    }
}
