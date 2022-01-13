package com.farsight.mixin;

import com.farsight.FarsightMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetHandlerMixin
{
    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"), cancellable = true)
    private void onUnload(ClientboundForgetLevelChunkPacket packet, final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Redirect(method = "handleSetChunkCacheRadius", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetChunkCacheRadiusPacket;getRadius()I"))
    private int onViewDistChange(final ClientboundSetChunkCacheRadiusPacket sUpdateViewDistancePacket)
    {
        return FarsightMod.config.getCommonConfig().maxchunkdist.get();
    }

    @Redirect(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;chunkRadius()I"))
    private int onJoinGame(final ClientboundLoginPacket sJoinGamePacket)
    {
        return FarsightMod.config.getCommonConfig().maxchunkdist.get();
    }
}
