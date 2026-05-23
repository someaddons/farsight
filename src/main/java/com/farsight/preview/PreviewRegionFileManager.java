package com.farsight.preview;

import com.farsight.FarsightMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PreviewRegionFileManager
{
    private static final double LOOK_AHEAD = 5;
    public static        int    serverRenderDists;

    /**
     * Small cache so we do not re-read the same file from disk all the time
     */
    private static PreviewRegionFile[] cache      = new PreviewRegionFile[16];
    private static int                 cacheIndex = 0;

    /**
     * Get region file by dimension and coordinates
     *
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    private static PreviewRegionFile getFileForCoords(ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        for (int i = 0; i < cache.length; i++)
        {
            final PreviewRegionFile cacheRegionFile = cache[i];
            if (cacheRegionFile != null && cacheRegionFile.contains(chunkX, chunkZ) && cacheRegionFile.channel.isOpen())
            {
                return cacheRegionFile;
            }
        }

        final Path regionPath = PreviewRegionFile.pathFor(Minecraft.getInstance().gameDirectory.toPath(), dimension, chunkX, chunkZ);
        if (regionPath == null)
        {
            return null;
        }

        final File file = regionPath.toFile();
        try
        {
            final PreviewRegionFile newRegion = new PreviewRegionFile(file.toPath(), chunkX, chunkZ);
            cacheIndex = (cacheIndex + 1) % cache.length;
            final PreviewRegionFile oldRegion = cache[cacheIndex];
            cache[cacheIndex] = newRegion;
            if (oldRegion != null)
            {
                oldRegion.close();
            }

            return newRegion;
        }
        catch (Exception e)
        {
            FarsightMod.logDebug("Error getting region file: " + file.toPath(), e);
            return null;
        }
    }

    /**
     * Save a received packet to disk
     *
     * @param packet
     */
    public static void saveChunkPacket(ClientboundLevelChunkWithLightPacket packet)
    {
        ioExecutor.submit(() -> {
            PreviewRegionFile regionFile = getFileForCoords(Minecraft.getInstance().level.dimension(), packet.getX(), packet.getZ());
            if (regionFile == null)
            {
                return;
            }

            try
            {
                regionFile.saveChunk(Minecraft.getInstance().level, packet.getX(), packet.getZ(), packet);
            }
            catch (Exception e)
            {
                FarsightMod.LOGGER.warn("Failed to save chunk packet: x:" + packet.getX() + " z:" + packet.getZ(), e);
            }
        });
    }

    /**
     * Loads a chunk from disk
     *
     * @param chunkX
     * @param chunkZ
     * @return
     */
    public static ClientboundLevelChunkWithLightPacket loadChunk(final int chunkX, final int chunkZ)
    {
        PreviewRegionFile regionFile = getFileForCoords(Minecraft.getInstance().level.dimension(), chunkX, chunkZ);
        if (regionFile == null)
        {
            return null;
        }

        try
        {
            return regionFile.loadChunk(Minecraft.getInstance().level, chunkX, chunkZ);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    final private static ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private static List<ChunkPos> toLoad           = new ArrayList<>();
    private static Set<ChunkPos>  pendingPositions = new HashSet<>();

    public static void onClientTick()
    {
        final int size = Minecraft.getInstance().options.renderDistance().get();
        final Player player = Minecraft.getInstance().player;
        final ChunkPos origin = player.chunkPosition();
        final Level clientLevel = player.level();

        if (pendingPositions.size() > 500)
        {
            pendingPositions.clear();
        }

        toLoad = new ArrayList<>();
        for (int x = -size; x <= size; x++)
        {
            for (int z = -size; z <= size; z++)
            {
                final int chunkX = origin.x + x;
                final int chunkZ = origin.z + z;
                if (!pendingPositions.contains(new ChunkPos(chunkX, chunkZ)) && clientLevel.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) == null)
                {
                    toLoad.add(new ChunkPos(chunkX, chunkZ));
                }
            }
        }

        if (toLoad.isEmpty())
        {
            return;
        }

        double xDir = -Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
        double zDir = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
        ChunkPos comparePos = new ChunkPos(origin.x + (int) (xDir * LOOK_AHEAD), origin.z + (int) (zDir * LOOK_AHEAD));
        if (clientLevel.getChunk(comparePos.x, comparePos.z, ChunkStatus.FULL, false) == null)
        {
            comparePos = new ChunkPos(origin.x + (int) (xDir * 2), origin.z + (int) (zDir * 2));
        }
        toLoad.sort(Comparator.comparingLong(comparePos::getChessboardDistance));

        for (int i = 0; i < FarsightMod.config.getCommonConfig().previewChunkLoadSpeed && i < toLoad.size(); i++)
        {
            final ChunkPos loadPos = toLoad.get(i);
            pendingPositions.add(loadPos);
            ioExecutor.submit(() -> {
                final ClientboundLevelChunkWithLightPacket packet = loadChunk(loadPos.x, loadPos.z);
                Minecraft.getInstance().submit(() -> {
                    if (packet != null && clientLevel.getChunk(loadPos.x, loadPos.z, ChunkStatus.FULL, false) == null)
                    {
                        Minecraft.getInstance().getConnection().handleLevelChunkWithLight(packet);
                        pendingPositions.remove(loadPos);
                    }
                });
            });
        }
    }
}
