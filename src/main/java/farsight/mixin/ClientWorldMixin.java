package farsight.mixin;

import farsight.FarsightClientChunkManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;

@Mixin(ClientWorld.class)
/**
 * Exchanges the client's chunk map with a custom implementation, which can handle chunks at any distance apart fine
 */
public class ClientWorldMixin
{
    @Shadow
    @Final
    @Mutable
    private ClientChunkManager chunkSource;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(
      final ClientPlayNetworkHandler clientPacketListener,
      final ClientWorld.Properties clientLevelData,
      final RegistryKey resourceKey,
      final RegistryEntry holder,
      final int i,
      final int j,
      final Supplier supplier,
      final WorldRenderer levelRenderer,
      final boolean bl,
      final long l,
      final CallbackInfo ci)
    {
        chunkSource = new FarsightClientChunkManager((ClientWorld) ((Object) this));
    }
}
