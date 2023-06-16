package farsight.mixin;

import farsight.FarsightMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.s2c.play.ChunkLoadDistanceS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetHandlerMixin
{
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private ClientWorld world;

    @Redirect(method = "onChunkLoadDistance", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/ChunkLoadDistanceS2CPacket;getDistance()I"))
    private int onViewDistChange(final ChunkLoadDistanceS2CPacket instance)
    {
        //client.options.getViewDistance().setValue(FarsightMod.config.getCommonConfig().maxchunkdist);
        return FarsightMod.getConfig().getCommonConfig().maxRenderDistance;
    }

    @Redirect(method = "onGameJoin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/GameJoinS2CPacket;viewDistance()I"))
    private int onJoinGame(final GameJoinS2CPacket instance)
    {
        return FarsightMod.getConfig().getCommonConfig().maxRenderDistance;
    }

    @Inject(method = "onUnloadChunk", at = @At("HEAD"), cancellable = true)
    private void onChunkUnload(final UnloadChunkS2CPacket packet, final CallbackInfo ci)
    {
        NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, this.client);
        int i = packet.getX();
        int j = packet.getZ();
        ClientChunkManager clientChunkManager = world.getChunkManager();
        clientChunkManager.unload(i, j);

        ci.cancel();
    }
}
