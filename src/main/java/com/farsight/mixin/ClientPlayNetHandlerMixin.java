package com.farsight.mixin;

import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.play.server.SJoinGamePacket;
import net.minecraft.network.play.server.SUnloadChunkPacket;
import net.minecraft.network.play.server.SUpdateViewDistancePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetHandlerMixin
{
    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"), cancellable = true)
    private void onUnload(SUnloadChunkPacket packet, final CallbackInfo ci)
    {
        ci.cancel();
    }

    @Redirect(method = "handleSetChunkCacheRadius", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SUpdateViewDistancePacket;getRadius()I"))
    private int onViewDistChange(final SUpdateViewDistancePacket sUpdateViewDistancePacket)
    {
        return 32;
    }

    @Redirect(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/play/server/SJoinGamePacket;getChunkRadius()I"))
    private int onJoinGame(final SJoinGamePacket sJoinGamePacket)
    {
        return 32;
    }
}
