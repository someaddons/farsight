package farsight;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jctools.maps.NonBlockingHashMapLong;

import java.util.function.Consumer;

/**
 * Simple client chunk manager, based on a concurrent hashmap. Unboxing here may be a performance bottleneck.
 */
public class FarsightClientChunkManager extends ClientChunkCache
{
    private static int                                                       EXTRA_CHUNK_DATA_LEEWAY = 10;
    private final  NonBlockingHashMapLong<LevelChunk>                        chunks                  = new NonBlockingHashMapLong();
    private final  Long2ObjectOpenHashMap<ClientboundForgetLevelChunkPacket> unloadedOnServer        = new Long2ObjectOpenHashMap();
    private final  ClientLevel                                               world;
    public         ClientPacketListener                                      packetListener          = null;

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
        final LevelChunk chunk = chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk == null)
        {
            return;
        }

        world.unload(chunk);
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
    public boolean checkUnload(final ClientboundForgetLevelChunkPacket packet)
    {
        if (unloading)
        {
            return false;
        }

        final Player player = Minecraft.getInstance().player;
        if (player != null && player.chunkPosition().getChessboardDistance(new ChunkPos(packet.getX(), packet.getZ()))
                                > Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY)
        {
            return false;
        }
        else
        {
            unloadedOnServer.put(ChunkPos.asLong(packet.getX(), packet.getZ()), packet);
            if (player != null)
            {
                for (ObjectIterator<Long2ObjectMap.Entry<ClientboundForgetLevelChunkPacket>> iterator = unloadedOnServer.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); )
                {
                    final Long2ObjectMap.Entry<ClientboundForgetLevelChunkPacket> entry = iterator.next();
                    final long chunkLong = entry.getLongKey();
                    if (getChebyshevDistance(player.chunkPosition().x, player.chunkPosition().z, ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong))
                          > Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY)
                    {
                        unloading = true;
                        if (packetListener != null)
                        {
                            packetListener.handleForgetLevelChunk(entry.getValue());
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
