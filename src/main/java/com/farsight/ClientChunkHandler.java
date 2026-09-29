package com.farsight;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import java.util.concurrent.ConcurrentHashMap;

public class ClientChunkHandler
{
    /**
     * Extra distance of chunks kept
     */
    public static int EXTRA_CHUNK_DATA_LEEWAY = 10;

    /**
     * Pending chunks to be unloaded.
     */
    private static final ConcurrentHashMap<Long, ClientboundForgetLevelChunkPacket> unloadedOnServer = new ConcurrentHashMap<>();

    /**
     * Toggle to allow the vanilla call go through when actually unloading
     */
    static volatile boolean unloading = false; //marked as volatile: warns other

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

        //Reduces memory accesses
        final int maxDistance = Minecraft.getInstance().options.renderDistance().get() + EXTRA_CHUNK_DATA_LEEWAY;
        final int playerX = player.chunkPosition().x();
        final int playerZ = player.chunkPosition().z();

        //using declared function
        if (getChebyshevDistance(playerX, playerZ, packet.pos().x(), packet.pos().z()) > maxDistance)
        {
            return false;
        }

        unloadedOnServer.put(ChunkPos.pack(packet.pos().x(), packet.pos().z()), packet);

        final LongArrayList chunksToUnload = new LongArrayList();

        //Fetch all chunks first
        for (final long chunkLong : unloadedOnServer.keySet())
        {
            if (getChebyshevDistance(playerX, playerZ, ChunkPos.getX(chunkLong), ChunkPos.getZ(chunkLong)) > maxDistance)
            {
                chunksToUnload.add(chunkLong);
            }
        }

        //Now remove them
        for (int i = 0; i < chunksToUnload.size(); i++)
        {
            final ClientboundForgetLevelChunkPacket pending = unloadedOnServer.remove(chunksToUnload.getLong(i));
            if (pending != null && packetListener != null)
            {
                unloading = true;
                try
                {
                    packetListener.handleForgetLevelChunk(pending);
                }
                finally
                {
                    unloading = false;
                }
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
        unloadedOnServer.remove(ChunkPos.pack(x, z));
    }

    public static int getChebyshevDistance(int chunkXa, int chunkZa, int chunkXb, int chunkZb)
    {
        return Math.max(Math.abs(chunkXa - chunkXb), Math.abs(chunkZa - chunkZb));
    }
}