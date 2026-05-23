package com.farsight.mixin;

import com.farsight.FarsightClientChunkManager;
import com.farsight.FarsightMod;
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
        final ClientPacketListener connection,
        final ClientLevel.ClientLevelData levelData,
        final ResourceKey dimension,
        final Holder dimensionType,
        final int serverChunkRadius,
        final int serverSimulationDistance,
        final LevelRenderer levelRenderer,
        final boolean isDebug,
        final long biomeZoomSeed,
        final int seaLevel,
        final CallbackInfo ci)
    {
        if (chunkSource.getClass().equals(ClientChunkCache.class))
        {
            if (FarsightMod.config.getCommonConfig().enableChunkManager)
            {
                chunkSource = new FarsightClientChunkManager((ClientLevel) ((Object) this));
            }
        }
        else
        {
            FarsightMod.LOGGER.info("Farsight not enabled for: " + dimension + " as other mods changed the chunk source to:" + chunkSource.getClass()+ " only preview chunk feature may work");
        }
    }
}
