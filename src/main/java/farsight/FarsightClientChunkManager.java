package farsight;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import org.jctools.maps.NonBlockingHashMapLong;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.ChunkData;
import net.minecraft.network.packet.s2c.play.UnloadChunkS2CPacket;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

/**
 * Simple client chunk manager, based on a concurrent hashmap. Unboxing here may be a performance bottleneck.
 */
public class FarsightClientChunkManager extends ClientChunkManager
{
    private static int                                                       EXTRA_CHUNK_DATA_LEEWAY = 10;
    private final  NonBlockingHashMapLong<WorldChunk>                        chunks                  = new NonBlockingHashMapLong();
    private final  Long2ObjectOpenHashMap<UnloadChunkS2CPacket> unloadedOnServer        = new Long2ObjectOpenHashMap();
    private final  ClientWorld                                               world;
    public         ClientPlayNetworkHandler                                      packetListener          = null;
    public static  List<BiConsumer<ClientWorld, WorldChunk>>                 unloadCallback          = new ArrayList<>();
    public static  List<BiConsumer<ClientWorld, WorldChunk>>                 loadCallback            = new ArrayList<>();

    public FarsightClientChunkManager(final ClientWorld world)
    {
        super(world, 5);
        this.world = world;
    }

    @Override
    public WorldChunk getChunk(int x, int z, ChunkStatus leastStatus, boolean create)
    {
        final WorldChunk chunk = chunks.get(ChunkPos.toLong(x, z));
        if (chunk != null)
        {
            return chunk;
        }

        if (create)
        {
            return getChunk(0, 0, null, false);
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
    public void updateLoadDistance(int loadDistance)
    {
    }

    @Override
    public WorldChunk loadChunkFromPacket(int x, int z, PacketByteBuf buf, NbtCompound nbt, Consumer<ChunkData.BlockEntityVisitor> consumer)
    {
        WorldChunk levelChunk = chunks.get(ChunkPos.toLong(x, z));
        if (levelChunk == null)
        {
            levelChunk = new WorldChunk(this.world, new ChunkPos(x, z));
            levelChunk.loadFromPacket(buf, nbt, consumer);
            this.chunks.put(ChunkPos.toLong(x, z), levelChunk);
        }
        else
        {
            world.unloadBlockEntities(levelChunk);
            levelChunk.loadFromPacket(buf, nbt, consumer);
        }

        for (BiConsumer<ClientWorld, net.minecraft.world.chunk.WorldChunk> loadCallbackEntry : loadCallback)
        {
            loadCallbackEntry.accept(world, levelChunk);
        }

        unloadedOnServer.remove(ChunkPos.toLong(x, z));
        this.world.resetChunkColor(new ChunkPos(x, z));

        return levelChunk;
    }

    @Override
    public void unload(ChunkPos pos)
    {
        final WorldChunk chunk = chunks.remove(pos.toLong());
        if (chunk == null)
        {
            return;
        }

        for (final BiConsumer<ClientWorld, WorldChunk> unloader : unloadCallback)
        {
            unloader.accept(world, chunk);
        }

        world.unloadBlockEntities(chunk);
    }

    public int getChebyshevDistance(int chunkXa, int chunkZa, int chunkXb, int chunkZb)
    {
        return Math.max(Math.abs(chunkXa - chunkXb), Math.abs(chunkZa - chunkZb));
    }

    /**
     * Toggle to allow the vanilla call go through when actually unloading
     */
    boolean unloading = false;

    /**
     * Checks if the chunk should be unloaded directly, returns true is unloading is handled later by this
     *
     * @param packet
     * @return true if unloading is prevented/scheduled for later
     */
    public boolean checkUnload(final UnloadChunkS2CPacket packet)
    {
        if (unloading)
        {
            return false;
        }

        final PlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && player.getChunkPos().getChebyshevDistance(packet.pos())
                                > MinecraftClient.getInstance().options.getViewDistance().getValue() + EXTRA_CHUNK_DATA_LEEWAY)
        {
            return false;
        }
        else
        {
            unloadedOnServer.put(packet.pos().toLong(), packet);
            if (player != null)
            {
                for (ObjectIterator<Long2ObjectMap.Entry<UnloadChunkS2CPacket>> iterator = unloadedOnServer.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); )
                {
                    final Long2ObjectMap.Entry<UnloadChunkS2CPacket> entry = iterator.next();
                    final long chunkLong = entry.getLongKey();
                    if (getChebyshevDistance(player.getChunkPos().x, player.getChunkPos().z, ChunkPos.getPackedX(chunkLong), ChunkPos.getPackedZ(chunkLong))
                          > MinecraftClient.getInstance().options.getViewDistance().getValue() + EXTRA_CHUNK_DATA_LEEWAY)
                    {
                        unloading = true;
                        if (packetListener != null)
                        {
                            packetListener.onUnloadChunk(entry.getValue());
                        }
                        unloading = false;
                        iterator.remove();
                    }
                }
            }
            else
            {
                unloadedOnServer.clear();
            }

            return true;
        }
    }
}
