package farsight.mixin;

import farsight.FarsightClientChunkManager;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
/**
 * Exchanges the client's chunk map with a custom implementation, which can handle chunks at any distance apart fine
 */
public class ClientWorldMixin
{
    @Shadow
    @Final
    @Mutable
    private ClientChunkCache chunkSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(
      final ClientPacketListener clientPacketListener,
      final ClientLevel.ClientLevelData clientLevelData,
      final ResourceKey resourceKey,
      final Holder holder,
      final int i,
      final int j,
      final Supplier supplier,
      final LevelRenderer levelRenderer,
      final boolean bl,
      final long l,
      final CallbackInfo ci)
    {
        chunkSource = new FarsightClientChunkManager((ClientLevel) ((Object) this));
    }
}
