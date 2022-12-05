package farsight;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.BlockView;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.light.LightingProvider;
import org.jctools.maps.NonBlockingHashMapLong;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Simple client chunk manager, based on a concurrent hashmap. Unboxing here may be a performance bottleneck.
 */
public class FarsightClientChunkManager extends net.minecraft.client.world.ClientChunkManager
{
    private static int                        EXTRA_CHUNK_DATA_LEEWAY = 10;
    public static  List<Consumer<WorldChunk>> unloadCallback          = new ArrayList<>();

    private final NonBlockingHashMapLong<WorldChunk> chunks           = new NonBlockingHashMapLong();
    private final LongOpenHashSet                    unloadedOnServer = new LongOpenHashSet();

    private final EmptyChunk       emptyChunk;
    private final LightingProvider lightingProvider;
    private final ClientWorld      world;

    public FarsightClientChunkManager(final ClientWorld world)
    {
        super(world, 64);
        this.world = world;
        this.emptyChunk = new EmptyChunk(world, new ChunkPos(0, 0), world.getRegistryManager().get(Registry.BIOME_KEY).entryOf(BiomeKeys.PLAINS));
        this.lightingProvider = new LightingProvider(this, true, world.getDimension().hasSkyLight());
    }

    @Override
    public WorldChunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create)
    {
        final WorldChunk superChunk = super.getChunk(x, z, leastStatus, create);
        final WorldChunk chunk = chunks.get(ChunkPos.toLong(x, z));
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
    public String getDebugString()
    {
        return chunks.size() + ", " + this.getLoadedChunkCount();
    }

    @Override
    public int getLoadedChunkCount()
    {
        return chunks.size();
    }

    @Override
    public LightingProvider getLightingProvider()
    {
        return lightingProvider;
    }

    @Override
    public BlockView getWorld()
    {
        return world;
    }

    @Override
    public void updateLoadDistance(int loadDistance)
    {
    }

    @Override
    public WorldChunk loadChunkFromPacket(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer)
    {
        WorldChunk worldChunk = chunks.get(ChunkPos.toLong(x, z));
        if (worldChunk == null)
        {
            worldChunk = new WorldChunk(this.world, new ChunkPos(x, z));
            worldChunk.loadFromPacket(buf, nbt, consumer);
            this.chunks.put(ChunkPos.toLong(x, z), worldChunk);
        }
        else
        {
            world.unloadBlockEntities(worldChunk);
            worldChunk.loadFromPacket(buf, nbt, consumer);
        }
        unloadedOnServer.remove(ChunkPos.toLong(x, z));
        this.world.resetChunkColor(new ChunkPos(x, z));

        return worldChunk;
    }

    @Override
    public void unload(int chunkX, int chunkZ)
    {
        final PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.getChunkPos().getChebyshevDistance(new ChunkPos(chunkX, chunkZ))
                                > MinecraftClient.getInstance().options.getViewDistance().getValue() + EXTRA_CHUNK_DATA_LEEWAY)
        {
            final WorldChunk chunk = chunks.remove(ChunkPos.toLong(chunkX, chunkZ));
            unloadChunk(chunk);
        }
        else
        {
            unloadedOnServer.add(ChunkPos.toLong(chunkX, chunkZ));
            if (player != null)
            {
                for (LongIterator iterator = unloadedOnServer.iterator(); iterator.hasNext(); )
                {
                    final long chunkLong = iterator.nextLong();
                    if (getChebyshevDistance(player.getChunkPos().x, player.getChunkPos().z, ChunkPos.getPackedX(chunkLong), ChunkPos.getPackedZ(chunkLong))
                          > MinecraftClient.getInstance().options.getViewDistance().getValue() + EXTRA_CHUNK_DATA_LEEWAY)
                    {
                        final WorldChunk chunk = chunks.remove(chunkLong);
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

    private void unloadChunk(final WorldChunk chunk)
    {
        if (chunk == null)
        {
            return;
        }

        world.unloadBlockEntities(chunk);

        this.world.enqueueChunkUpdate(() ->
        {
            LightingProvider lightingProvider = this.world.getLightingProvider();

            for (int i = this.world.getBottomSectionCoord(); i < this.world.getTopSectionCoord(); ++i)
            {
                lightingProvider.setSectionStatus(ChunkSectionPos.from(chunk.getPos().x, i, chunk.getPos().z), true);
            }

            lightingProvider.setColumnEnabled(new ChunkPos(chunk.getPos().x, chunk.getPos().z), false);
            this.world.markChunkRenderability(chunk.getPos().x, chunk.getPos().z);
        });

        for (final Consumer<WorldChunk> consumer : unloadCallback)
        {
            consumer.accept(chunk);
        }
    }
}
