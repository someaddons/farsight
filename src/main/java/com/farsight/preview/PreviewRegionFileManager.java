package com.farsight.preview;

import com.farsight.FarsightMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.io.File;
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
    private static PreviewRegionFile[] cache = new PreviewRegionFile[36];
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
        for (int index = cacheIndex, i = 0; i < cache.length; i++)
        {
            final PreviewRegionFile cacheRegionFile = cache[index];
            if (cacheRegionFile != null && cacheRegionFile.contains(chunkX, chunkZ) && cacheRegionFile.channel.isOpen() && cacheRegionFile.dimension.equals(dimension))
            {
                return cacheRegionFile;
            }

            if (--index < 0)
            {
                index = cache.length - 1;
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
            final PreviewRegionFile newRegion = new PreviewRegionFile(file.toPath(), dimension, chunkX, chunkZ);
            cacheIndex = (cacheIndex + 1) % cache.length;
            final PreviewRegionFile oldRegion = cache[cacheIndex];
            cache[cacheIndex] = newRegion;
            if (oldRegion != null)
            {
                oldRegion.close();
            }

            return newRegion;
        }
        catch (Throwable e)
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
        final Level level = Minecraft.getInstance().level;
        ioExecutor.submit(() -> {
            PreviewRegionFile regionFile = getFileForCoords(level.dimension(), packet.x(), packet.z());
            if (regionFile == null)
            {
                return;
            }

            try
            {
                regionFile.saveChunk(level, packet.x(), packet.z(), packet);
            }
            catch (Throwable e)
            {
                FarsightMod.LOGGER.warn("Failed to save chunk packet: x:" + packet.x() + " z:" + packet.z(), e);
            }
        });
    }

    /**
     * Loads a chunk from disk
     *
     * @param chunkX
     * @param chunkZ
     * @param clientLevel
     * @return
     */
    public static ClientboundLevelChunkWithLightPacket loadChunk(final int chunkX, final int chunkZ, final Level clientLevel)
    {
        try
        {
            PreviewRegionFile regionFile = getFileForCoords(clientLevel.dimension(), chunkX, chunkZ);
            if (regionFile == null)
            {
                return null;
            }
            return regionFile.loadChunk(clientLevel, chunkX, chunkZ);
        }
        catch (Exception e)
        {
            FarsightMod.logDebug("Error during loading chunk call: ", e);
        }

        return null;
    }

    private static final ExecutorService ioExecutor =
        Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "farsightIO");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((t, e) -> FarsightMod.logDebug("Exception in IO thread: ", e));
            return thread;
        });

    /**
     * Set of positions currently in loading
     */
    private static Set<ChunkPos> pendingPositions = new HashSet<>();

    /**
     * Set of positions where loading failed or no data exists
     */
    private static Set<ChunkPos> failedOrEmptyPositions = new LinkedHashSet<>();

    public static void onClientTick()
    {
        final Player player = Minecraft.getInstance().player;
        final Level clientLevel = player.level();

        final List<ChunkPos> toLoad = getLoadingPositions(player, clientLevel);
        if (toLoad.isEmpty())
        {
            return;
        }

        for (final ChunkPos loadPos : toLoad)
        {
            pendingPositions.add(loadPos);
            ioExecutor.submit(() -> {
                final ClientboundLevelChunkWithLightPacket packet = loadChunk(loadPos.x, loadPos.z, clientLevel);
                Minecraft.getInstance().submit(() -> {
                    if (clientLevel != Minecraft.getInstance().level)
                    {
                        return;
                    }

                    pendingPositions.remove(loadPos);
                    if (packet == null)
                    {
                        failedOrEmptyPositions.add(loadPos);
                        return;
                    }

                    if (clientLevel.getChunk(loadPos.x, loadPos.z, ChunkStatus.FULL, false) == null)
                    {
                        try
                        {
                            Minecraft.getInstance().getConnection().handleLevelChunkWithLight(packet);
                        }
                        catch (Throwable e)
                        {
                            FarsightMod.logDebug("Failed to handle chunk packet, dim:" + clientLevel.dimension() + " pos:" + loadPos, e);
                            ioExecutor.submit(() ->
                            {
                                PreviewRegionFile regionFile = getFileForCoords(clientLevel.dimension(), loadPos.x, loadPos.z);
                                if (regionFile != null)
                                {
                                    regionFile.clearFile();
                                }
                            });
                        }
                    }
                });
            });
        }
    }

    /**
     * Saved directional chunk position the player is looking at
     */
    static ChunkPos comparePos = null;

    /**
     * Calculates the positions to load
     *
     * @param player
     * @param clientLevel
     * @return
     */
    private static List<ChunkPos> getLoadingPositions(final Player player, final Level clientLevel)
    {
        final int renderDistance = Minecraft.getInstance().options.renderDistance().get();
        final ChunkPos origin = player.chunkPosition();

        if (comparePos == null || clientLevel.getGameTime() % 40 == 0)
        {
            cleanupFailedOrEmptyPositions(renderDistance, origin);

            double xDir = -Mth.sin(player.getYRot() * Mth.DEG_TO_RAD);
            double zDir = Mth.cos(player.getYRot() * Mth.DEG_TO_RAD);
            ChunkPos newComparePos = new ChunkPos(origin.x + (int) (xDir * LOOK_AHEAD), origin.z + (int) (zDir * LOOK_AHEAD));
            if (clientLevel.getChunk(newComparePos.x, newComparePos.z, ChunkStatus.FULL, false) == null)
            {
                newComparePos = new ChunkPos(origin.x + (int) (xDir * 2), origin.z + (int) (zDir * 2));
            }

            if (!newComparePos.equals(comparePos))
            {
                // Reset cursor
                resetScan();
                comparePos = newComparePos;
            }
        }

        final int limit = FarsightMod.config.getCommonConfig().previewChunkLoadSpeed;
        final List<ChunkPos> toLoad = new ArrayList<>(limit);

        // Limit to player render area
        final int minX = origin.x - renderDistance;
        final int maxX = origin.x + renderDistance;
        final int minZ = origin.z - renderDistance;
        final int maxZ = origin.z + renderDistance;

        final int maxRadius = renderDistance + Math.max(Math.abs(comparePos.x - origin.x), Math.abs(comparePos.z - origin.z));

        final int diameter = renderDistance * 2 + 1;
        final int checksPerTick = (diameter * diameter + 39) / 40;
        for (int checked = 0; checked < checksPerTick && toLoad.size() < limit; )
        {
            final int chunkX = comparePos.x + chunkOffsetX;
            final int chunkZ = comparePos.z + chunkOffsetZ;

            if (advanceScanUntil(maxRadius))
            {
                break;
            }

            if (chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ)
            {
                checked++;
                final ChunkPos pos = new ChunkPos(chunkX, chunkZ);
                if (!pendingPositions.contains(pos) && !failedOrEmptyPositions.contains(pos) && clientLevel.getChunk(pos.x, pos.z, ChunkStatus.FULL, false) == null)
                {
                    toLoad.add(pos);
                    if (toLoad.size() == limit)
                    {
                        break;
                    }
                }
            }
        }

        return toLoad;
    }

    /**
     * Utility to scan positions iteratively
     */
    private static int chunkOffsetX = 0;
    private static int chunkOffsetZ = 0;

    private static void resetScan()
    {
        chunkOffsetX = 0;
        chunkOffsetZ = 0;
    }

    private static boolean advanceScanUntil(final int endRadius)
    {
        final int radius = Math.max(Math.abs(chunkOffsetX), Math.abs(chunkOffsetZ));
        if (radius > endRadius)
        {
            return true;
        }

        if (chunkOffsetX == radius && chunkOffsetZ == -radius)
        {
            // Enter next ring
            chunkOffsetX++;
        }
        else if (chunkOffsetX == radius && chunkOffsetZ < radius)
        {
            chunkOffsetZ++;
        }
        else if (chunkOffsetZ == radius && chunkOffsetX > -radius)
        {
            chunkOffsetX--;
        }
        else if (chunkOffsetX == -radius && chunkOffsetZ > -radius)
        {
            chunkOffsetZ--;
        }
        else
        {
            chunkOffsetX++;
        }

        return false;
    }

    public static void cleanupFailedOrEmptyPositions(final int renderDistance, final ChunkPos origin)
    {
        failedOrEmptyPositions.removeIf(pos -> pos.getChessboardDistance(origin) > renderDistance);
    }

    /**
     * Removes a failed or empty position on chunk unload
     *
     * @param pos
     */
    public static void onChunkUnload(final ChunkPos pos)
    {
        failedOrEmptyPositions.remove(pos);
    }

    public static void onUnloadWorld()
    {
        pendingPositions.clear();
        failedOrEmptyPositions.clear();
        resetScan();
        comparePos = null;
        ioExecutor.submit(() -> {
            for (final PreviewRegionFile file : cache)
            {
                if (file != null)
                {
                    file.close();
                }
            }
            cache = new PreviewRegionFile[cache.length];
            cacheIndex = 0;
        });
    }
}
