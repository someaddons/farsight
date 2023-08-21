package com.farsight.mixin;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkProviderClient.class)
public abstract class ChunkProviderClientMixin
{
    @Shadow
    @Final
    private Long2ObjectMap<Chunk> loadedChunks;

    @Shadow
    public abstract Chunk provideChunk(final int x, final int z);

    @Unique
    private final Long2ObjectMap<ChunkPos> toUnload = new Long2ObjectOpenHashMap<>(200);

    @Inject(method = "unloadChunk", at = @At("HEAD"), cancellable = true)
    private void onUnload(final int x, final int z, final CallbackInfo ci)
    {
        ci.cancel();
        toUnload.put(ChunkPos.asLong(x, z), new ChunkPos(x, z));
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onTick(final CallbackInfoReturnable<Boolean> cir)
    {
        ObjectIterator<ChunkPos> objectiterator = this.toUnload.values().iterator();
        while (objectiterator.hasNext())
        {
            ChunkPos pos = objectiterator.next();
            if (pos.getDistanceSq(Minecraft.getMinecraft().player) > (64 * 16 * 64 * 16))
            {
                final Chunk chunk = this.provideChunk(pos.x, pos.z);

                if (!chunk.isEmpty())
                {
                    chunk.onUnload();
                }

                this.loadedChunks.remove(ChunkPos.asLong(pos.x, pos.z));
                objectiterator.remove();
            }
        }
    }
}
