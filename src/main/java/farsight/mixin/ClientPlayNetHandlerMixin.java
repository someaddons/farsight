package farsight.mixin;

import farsight.FarsightClientChunkManager;
import farsight.FarsightMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.ClientConnection;
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
public abstract class ClientPlayNetHandlerMixin extends ClientCommonNetworkHandler
{
    @Shadow
    private ClientWorld level;

    protected ClientPlayNetHandlerMixin(
      final MinecraftClient minecraft,
      final ClientConnection connection,
      final ClientConnectionState commonListenerCookie)
    {
        super(minecraft, connection, commonListenerCookie);
    }

    @Redirect(method = "handleSetChunkCacheRadius", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetChunkCacheRadiusPacket;getRadius()I"))
    private int onViewDistChange(final ChunkLoadDistanceS2CPacket sUpdateViewDistancePacket)
    {
        return FarsightMod.config.getCommonConfig().maxRenderDistance;
    }

    @Redirect(method = "handleLogin", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;chunkRadius()I"))
    private int onJoinGame(final GameJoinS2CPacket sJoinGamePacket)
    {
        return FarsightMod.config.getCommonConfig().maxRenderDistance;
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("HEAD"), cancellable = true)
    private void onChunkUnload(
      final UnloadChunkS2CPacket clientboundForgetLevelChunkPacket,
      final CallbackInfo ci)
    {
        NetworkThreadUtils.forceMainThread(clientboundForgetLevelChunkPacket, (ClientPlayNetworkHandler) (Object) this, this.client);
        ClientChunkManager clientChunkManager = level.getChunkManager();
        if (clientChunkManager instanceof FarsightClientChunkManager && ((FarsightClientChunkManager) clientChunkManager).checkUnload(clientboundForgetLevelChunkPacket))
        {
            ((FarsightClientChunkManager) clientChunkManager).packetListener = (ClientPlayNetworkHandler) (Object) this;
            ci.cancel();
        }
    }
}
