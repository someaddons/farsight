package com.farsight;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;

public class ClientChunkHandler
{
    /**
     * Extra distance of chunks kept
     */
    public static int                                                       EXTRA_CHUNK_DATA_LEEWAY = 10;

    /**
     * Pending chunks to be unloaded
     */
    private static final Long2ObjectOpenHashMap<ClientboundForgetLevelChunkPacket> unloadedOnServer = new Long2ObjectOpenHashMap();

    /**
     * Toggle to allow the vanilla call go through when actually unloading
     */
    static boolean unloading = false;

    /**
     * Checks if the chunk should be unloaded directly, returns true is unloading is handled later by this
     *
     * @param packet
     * @return true if unloading is prevented/scheduled for later
     */
    public static boolean delayUnload(final ClientboundForgetLevelChunkPacket packet, final ClientPacketListener packetListener)
    {
        if (unloading)
        {
            return false;
        }

        final Player player = Minecraft.getInstance().player;

        if (player == null)
        {
            unloadedOnServer.clear();
            return false;
        }

        if (player.chunkPosition().getChessboardDistance(new ChunkPos(packet.pos().x, packet.pos().z))
            > Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY)
        {
            return false;
        }

        unloadedOnServer.put(ChunkPos.asLong(packet.pos().x, packet.pos().z), packet);
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

        return true;
    }

    public static void onUnloadWorld()
    {
        unloadedOnServer.clear();
    }

    public static void onChunkUpdate(final int x, final int z)
    {
        unloadedOnServer.remove(ChunkPos.asLong(x,z));
    }

    public static int getChebyshevDistance(int chunkXa, int chunkZa, int chunkXb, int chunkZb)
    {
        return Math.max(Math.abs(chunkXa - chunkXb), Math.abs(chunkZa - chunkZb));
    }
}
