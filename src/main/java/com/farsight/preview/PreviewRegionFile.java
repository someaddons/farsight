package com.farsight.preview;

import com.farsight.FarsightMod;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

public class PreviewRegionFile implements Closeable
{
    // header:
    // magic int
    // version int
    // 1024 entries of:long chunkpos, long byteOffset, int length
    private static final int REGION_SIZE      = 32;
    private static final int CHUNK_COUNT      = REGION_SIZE * REGION_SIZE;
    private static final int MAGIC            = 0x4D435065;
    private static final int VERSION          = 1;
    private static final int CHUNK_ENTRY_SIZE = 20;
    private static final int HEADER_SIZE      = 8 + CHUNK_COUNT * CHUNK_ENTRY_SIZE;

    /**
     * Region file coords
     */
    private final int regionX;
    private final int regionZ;

    /**
     * Data holder for each contained respective chunk
     *
     * @param chunkPos
     * @param byteOffset
     * @param length
     */
    private record ChunkEntry(
        long chunkPos,
        long byteOffset,
        int length)
    {
        public int getHeaderIndex()
        {
            return headerIndex(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
        }
    }

    /**
     * The file this region corresponds to
     */
    final         FileChannel channel;
    private final Path        path;

    /**
     * Known index to chunk position metadata, read from header
     */
    private final Int2ObjectOpenHashMap<ChunkEntry> indexToChunkEntryMap = new Int2ObjectOpenHashMap<>();

    public PreviewRegionFile(Path path, final int chunkX, final int chunkZ) throws IOException
    {
        this.regionX = chunkX >> 5;
        this.regionZ = chunkZ >> 5;
        this.path = path;

        Files.createDirectories(path.getParent());

        this.channel = FileChannel.open(
            path,
            StandardOpenOption.CREATE,
            StandardOpenOption.READ,
            StandardOpenOption.WRITE
        );

        try
        {
            if (channel.size() < HEADER_SIZE)
            {
                initializeFile();
            }
            else
            {
                validateHeader();
                loadEntryCache();
                checkCompact();
            }
        }
        catch (Exception e)
        {
            FarsightMod.logDebug("Failed to initialize region file:" + path, e);
            clearFile();
        }
    }

    /**
     * Checks if the file is using too many unnecessary bytes and compacts it
     *
     * @throws IOException
     */
    private void checkCompact() throws IOException
    {
        long usedBytes = 0;
        for (final ChunkEntry chunkEntry : indexToChunkEntryMap.values())
        {
            usedBytes += chunkEntry.length;
        }
        long fileChunkBytes = channel.size() - HEADER_SIZE;
        long wasted = fileChunkBytes - usedBytes;
        if (wasted > 1000000 || wasted > fileChunkBytes / 2)
        {
            compact();
        }
    }

    /**
     * Compacts the regions files disk size
     *
     * @throws IOException
     */
    private void compact() throws IOException
    {
        FarsightMod.logDebug("Compacting region file to strip excess data: " + path);
        record SavedChunk(
            int index,
            long chunkPos,
            byte[] compressed) {}
        List<SavedChunk> chunks = new ArrayList<>();

        for (var entry : indexToChunkEntryMap.int2ObjectEntrySet())
        {
            ChunkEntry chunkEntry = entry.getValue();

            if (chunkEntry.length <= 0 || chunkEntry.byteOffset < HEADER_SIZE)
            {
                continue;
            }

            ByteBuffer buffer = ByteBuffer.allocate(chunkEntry.length);
            readFully(chunkEntry.byteOffset, buffer);

            byte[] compressed = new byte[chunkEntry.length];
            buffer.get(compressed);

            chunks.add(new SavedChunk(entry.getIntKey(), chunkEntry.chunkPos, compressed));
        }

        clearFile();

        for (SavedChunk saved : chunks)
        {
            final long offset = channel.size();
            final int length = saved.compressed.length;
            writeFully(offset, ByteBuffer.wrap(saved.compressed));
            ChunkEntry newEntry = new ChunkEntry(saved.chunkPos, offset, length);
            indexToChunkEntryMap.put(saved.index, newEntry);
            writeEntry(newEntry);
        }

        channel.force(true);
    }

    /**
     * Initializes the file with new header data
     *
     * @throws IOException
     */
    private void initializeFile() throws IOException
    {
        ByteBuffer header = ByteBuffer.allocate(HEADER_SIZE);
        header.order(ByteOrder.BIG_ENDIAN);

        header.putInt(MAGIC);
        header.putInt(VERSION);

        // entries default to zero byteOffset/length
        while (header.position() < HEADER_SIZE)
        {
            header.put((byte) 0);
        }

        header.flip();
        writeFully(0, header);
        channel.force(true);
    }

    /**
     * Tries to validate the header
     *
     * @throws IOException
     */
    private void validateHeader() throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        channel.position(0);
        channel.read(buffer);
        buffer.flip();

        int magic = buffer.getInt();
        int version = buffer.getInt();

        if (magic != MAGIC || version != VERSION)
        {
            FarsightMod.logDebug("Region file: " + path + " not matching version/id for existing file, initializing empty");
            clearFile();
        }
    }

    /**
     * Reset the file completly
     */
    private void clearFile()
    {
        try
        {
            indexToChunkEntryMap.clear();
            channel.truncate(0);
            channel.position(0);
            initializeFile();
        }
        catch (Exception e)
        {
            FarsightMod.logDebug("Failed to reset file:" + path, e);
        }
    }

    /**
     * Loads all chunk entries from the header
     *
     * @throws IOException
     */
    private void loadEntryCache() throws IOException
    {
        indexToChunkEntryMap.clear();

        ByteBuffer buffer = ByteBuffer.allocate(CHUNK_COUNT * CHUNK_ENTRY_SIZE);
        readFully(8L, buffer);

        for (int i = 0; i < CHUNK_COUNT; i++)
        {
            long chunkPos = buffer.getLong();
            long offset = buffer.getLong();
            int length = buffer.getInt();

            if (length > 0 && offset >= HEADER_SIZE)
            {
                indexToChunkEntryMap.put(
                    i,
                    new ChunkEntry(chunkPos, offset, length)
                );
            }
        }
    }

    /**
     * Check if the given coordinates can be contained in this region file
     *
     * @param chunkX
     * @param chunkZ
     * @return
     */
    public boolean contains(final int chunkX, final int chunkZ)
    {
        return chunkX >> 5 == regionX && chunkZ >> 5 == regionZ;
    }

    /**
     * Helper which creates the right file/folder path for the region file
     *
     * @param root
     * @param dimension
     * @param chunkX
     * @param chunkZ
     * @return
     */
    public static Path pathFor(Path root, ResourceKey<Level> dimension, int chunkX, int chunkZ)
    {
        final ServerData serverData = Minecraft.getInstance().getCurrentServer();

        if (serverData == null)
        {
            FarsightMod.LOGGER.warn("No server found, cannot create path");
            return null;
        }

        final String dimensionString = dimension.identifier().getNamespace() + File.separator + dimension.identifier().getPath() + File.separator;
        final String serverIdentifier = cleanString(serverData.name) + Objects.hash(serverData.ip, serverData.version, serverData.protocol);
        int regionX = chunkX >> 5;
        int regionZ = chunkZ >> 5;

        return root
            .resolve("farsightpreview")
            .resolve(serverIdentifier)
            .resolve(dimensionString)
            .resolve("r." + regionX + "." + regionZ + ".pcf");
    }

    /**
     * Makes sure the string works in the file system
     *
     * @param s
     * @return
     */
    private static String cleanString(String s)
    {
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Saves a chunk to the region file
     *
     * @param level
     * @param chunkX
     * @param chunkZ
     * @param packet
     * @throws IOException
     */
    public synchronized void saveChunk(final Level level, int chunkX, int chunkZ, ClientboundLevelChunkWithLightPacket packet) throws IOException
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
        packet.write(buf);
        byte[] packetBytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), packetBytes);

        byte[] compressedBytes = compress(packetBytes);
        long offset = channel.size();
        int length = compressedBytes.length;

        final ChunkEntry newEntry = new ChunkEntry(ChunkPos.pack(chunkX, chunkZ), offset, length);

        if (indexToChunkEntryMap.containsKey(newEntry.getHeaderIndex()) && indexToChunkEntryMap.get(newEntry.getHeaderIndex()).length == length)
        {
            // Skip writing, packet probably equals saved chunk, conflicts are nonissues as chunks are visual and temporary only.
            return;
        }

        FarsightMod.logDebug("Saving chunk: x:" + chunkX + " z:" + chunkZ);
        writeFully(offset, ByteBuffer.wrap(compressedBytes));
        writeEntry(newEntry);
        indexToChunkEntryMap.put(newEntry.getHeaderIndex(), newEntry);
        checkCompact();
    }

    /**
     * Loads a chunk preview packet from the region file
     *
     * @param level
     * @param chunkX
     * @param chunkZ
     * @return
     * @throws IOException
     */
    public synchronized ClientboundLevelChunkWithLightPacket loadChunk(final Level level, int chunkX, int chunkZ) throws IOException
    {
        int index = headerIndex(chunkX, chunkZ);

        ChunkEntry chunkEntry = readEntry(index, chunkX, chunkZ);
        if (chunkEntry.length <= 0 || chunkEntry.byteOffset <= 0)
        {
            return null;
        }

        if (chunkEntry.byteOffset + chunkEntry.length > channel.size())
        {
            return null;
        }

        FarsightMod.logDebug("Loading chunk: x:" + chunkX + " z:" + chunkZ + " entry:" + chunkEntry);
        try
        {
            ByteBuffer buffer = ByteBuffer.allocate(chunkEntry.length);
            readFully(chunkEntry.byteOffset, buffer);

            byte[] compressed = new byte[chunkEntry.length];
            buffer.get(compressed);
            byte[] uncompressed = decompress(compressed);


            RegistryFriendlyByteBuf registryFriendlyByteBuf = new RegistryFriendlyByteBuf(Unpooled.buffer(), level.registryAccess(), ConnectionType.NEOFORGE);
            registryFriendlyByteBuf.capacity(uncompressed.length);
            registryFriendlyByteBuf.writerIndex(uncompressed.length);
            registryFriendlyByteBuf.setBytes(0, uncompressed);

            ClientboundLevelChunkWithLightPacket packet = new ClientboundLevelChunkWithLightPacket(registryFriendlyByteBuf);
            return packet;
        }
        catch (Exception e)
        {
            // On Error: reset file as its format is not compatible or some other I/O error occured
            FarsightMod.logDebug("Failed to decode chunk for file: " + path + " resetting file", e);
            clearFile();
        }

        return null;
    }

    /**
     * Writes a chunk entry to the header
     *
     * @param entry
     * @throws IOException
     */
    private void writeEntry(final ChunkEntry entry) throws IOException
    {
        ByteBuffer buffer = ByteBuffer.allocate(CHUNK_ENTRY_SIZE);
        buffer.putLong(entry.chunkPos);
        buffer.putLong(entry.byteOffset);
        buffer.putInt(entry.length);
        buffer.flip();

        writeFully(8L + (long) entry.getHeaderIndex() * CHUNK_ENTRY_SIZE, buffer);
    }

    /**
     * Reads a chunk entry from the header
     *
     * @param index
     * @param chunkX
     * @param chunkZ
     * @return
     * @throws IOException
     */
    private ChunkEntry readEntry(int index, final int chunkX, final int chunkZ) throws IOException
    {
        if (indexToChunkEntryMap.containsKey(index))
        {
            return indexToChunkEntryMap.get(index);
        }

        ByteBuffer buffer = ByteBuffer.allocate(CHUNK_ENTRY_SIZE);
        readFully(8L + (long) index * CHUNK_ENTRY_SIZE, buffer);

        long position = buffer.getLong();
        long offset = buffer.getLong();
        int length = buffer.getInt();

        if (ChunkPos.pack(chunkX, chunkZ) != position && length > 0)
        {
            FarsightMod.logDebug("Chunk position in file not matching!");
        }

        indexToChunkEntryMap.put(index, new ChunkEntry(position, offset, length));
        return indexToChunkEntryMap.get(index);
    }

    /**
     * Calculates the index for the chunk entry in the header
     *
     * @param chunkX
     * @param chunkZ
     * @return
     */
    private static int headerIndex(int chunkX, int chunkZ)
    {
        int localX = chunkX & 31;
        int localZ = chunkZ & 31;
        return localX + localZ * 32;
    }

    /**
     * Helper to make sure all bytes are getting read, also flips(exchanges reading/write pos) the buffer after for reading
     *
     * @param offset
     * @param buffer
     * @throws IOException
     */
    private void readFully(long offset, ByteBuffer buffer) throws IOException
    {
        channel.position(offset);

        while (buffer.hasRemaining())
        {
            int read = channel.read(buffer);
            if (read < 0)
            {
                throw new EOFException("Unexpected EOF");
            }
        }

        buffer.flip();
    }

    /**
     * Helper to make sure all bytes are getting written
     *
     * @param offset
     * @param buffer
     * @throws IOException
     */
    private void writeFully(long offset, ByteBuffer buffer) throws IOException
    {
        channel.position(offset);

        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
    }

    /**
     * Compresses the given input
     *
     * @param input
     * @return
     * @throws IOException
     */
    private static byte[] compress(byte[] input) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (DeflaterOutputStream deflater = new DeflaterOutputStream(out))
        {
            deflater.write(input);
        }

        return out.toByteArray();
    }

    /**
     * Decompresses the given input
     *
     * @param input
     * @return
     * @throws IOException
     */
    private static byte[] decompress(byte[] input) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(input)))
        {
            inflater.transferTo(out);
        }

        return out.toByteArray();
    }

    @Override
    public void close() throws IOException
    {
        channel.force(false);
        channel.close();
    }
}
