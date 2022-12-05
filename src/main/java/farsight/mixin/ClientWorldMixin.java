package farsight.mixin;

import farsight.FarsightClientChunkManager;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraft.util.registry.RegistryKey;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
/**
 * Exchanges the client's chunk map with a custom implementation, which can handle chunks at any distance apart fine
 */
public class ClientWorldMixin
{
    @Shadow
    @Final
    @Mutable
    private ClientChunkManager chunkManager;

    @Inject(method = "<init>", at = @At("RETURN"))
    public void onInit(
      final ClientPlayNetworkHandler netHandler,
      final ClientWorld.Properties properties,
      final RegistryKey registryRef,
      final RegistryEntry registryEntry,
      final int loadDistance,
      final int simulationDistance,
      final Supplier profiler,
      final WorldRenderer worldRenderer,
      final boolean debugWorld,
      final long seed,
      final CallbackInfo ci)
    {
        chunkManager = new FarsightClientChunkManager((ClientWorld) ((Object) this));
    }
}
