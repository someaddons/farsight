package farsight;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.jctools.maps.NonBlockingHashMapLong;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple client chunk manager, based on a concurrent hashmap. Unboxing here may be a performance bottleneck.
 */
public class FarsightClientChunkManager extends ClientChunkCache
{
    private static int                        EXTRA_CHUNK_DATA_LEEWAY = 10;
    public static  List<Consumer<LevelChunk>> unloadCallback          = new ArrayList<>();

    private final NonBlockingHashMapLong<LevelChunk> chunks           = new NonBlockingHashMapLong();
    private final LongOpenHashSet                    unloadedOnServer = new LongOpenHashSet();

    private final ClientLevel world;

    public FarsightClientChunkManager(final ClientLevel world)
    {
        super(world, 5);
        this.world = world;
    }

    @Override
    public LevelChunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create)
    {
        final LevelChunk chunk = chunks.get(ChunkPos.asLong(x, z));
        if (chunk != null)
        {
            return chunk;
        }

        if (create)
        {
            return emptyChunk;
        }

        return null;
    }

    @Override
    public String gatherStats()
    {
        return chunks.size() + ", " + this.getLoadedChunksCount();
    }

    @Override
    public int getLoadedChunksCount()
    {
        return chunks.size();
    }

    @Override
    public void updateViewRadius(int loadDistance)
    {
    }

    @Override
    public LevelChunk replaceWithPacketData(int x, int z, FriendlyByteBuf buf, CompoundTag nbt, Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer)
    {
        LevelChunk LevelChunk = chunks.get(ChunkPos.asLong(x, z));
        if (LevelChunk == null)
        {
            LevelChunk = new LevelChunk(this.world, new ChunkPos(x, z));
            LevelChunk.replaceWithPacketData(buf, nbt, consumer);
            this.chunks.put(ChunkPos.asLong(x, z), LevelChunk);
        }
        else
        {
            world.unload(LevelChunk);
            LevelChunk.replaceWithPacketData(buf, nbt, consumer);
        }
        unloadedOnServer.remove(ChunkPos.asLong(x, z));
        this.world.onChunkLoaded(new ChunkPos(x, z));

        return LevelChunk;
    }

    @Override
    public void drop(int chunkX, int chunkZ)
    {
        final Player player = Minecraft.getInstance().player;
        if (player != null && player.chunkPosition().getChessboardDistance(new ChunkPos(chunkX, chunkZ))
                                > Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY)
        {
            final LevelChunk chunk = chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
            unloadChunk(chunk);
        }
        else
        {
            unloadedOnServer.add(ChunkPos.asLong(chunkX, chunkZ));
            if (player != null)
            {
                for (LongIterator iterator = unloadedOnServer.iterator(); iterator.hasNext(); )
                {
                    final long chunkLong = iterator.nextLong();
                    if (getChebyshevDistance(player.chunkPosition().x, player.chunkPosition().z, ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong))
                          > Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY)
                    {
                        final LevelChunk chunk = chunks.remove(chunkLong);
                        unloadChunk(chunk);
                        iterator.remove();
                    }
                }
            }
        }
    }

    public int getChebyshevDistance(int chunkXa, int chunkZa, int chunkXb, int chunkZb)
    {
        return Math.max(Math.abs(chunkXa - chunkXb), Math.abs(chunkZa - chunkZb));
    }

    private void unloadChunk(final LevelChunk chunk)
    {
        if (chunk == null)
        {
            return;
        }

        world.unload(chunk);

        this.world.queueLightUpdate(() -> {
            LevelLightEngine levelLightEngine = this.world.getLightEngine();
            levelLightEngine.setLightEnabled(chunk.getPos(), false);

            int i;
            for (i = levelLightEngine.getMinLightSection(); i < levelLightEngine.getMaxLightSection(); ++i)
            {
                SectionPos sectionPos = SectionPos.of(chunk.getPos(), i);
                levelLightEngine.queueSectionData(LightLayer.BLOCK, sectionPos, (DataLayer) null);
                levelLightEngine.queueSectionData(LightLayer.SKY, sectionPos, (DataLayer) null);
            }

            for (i = this.world.getMinSection(); i < this.world.getMaxSection(); ++i)
            {
                levelLightEngine.updateSectionStatus(SectionPos.of(chunk.getPos(), i), true);
            }
        });

        for (final Consumer<LevelChunk> consumer : unloadCallback)
        {
            consumer.accept(chunk);
        }
    }
}
