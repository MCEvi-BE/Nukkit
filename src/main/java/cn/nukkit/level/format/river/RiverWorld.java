package cn.nukkit.level.format.river;

import cn.nukkit.nbt.tag.CompoundTag;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiverWorld {

    public static final byte[] HEADER = {-79, 11};

    public static final byte VERSION = (byte) 9;

    private final byte worldVersion = 0x2;

    private Map<Long, Chunk> chunks = new HashMap<>();

    private CompoundTag extraData = new CompoundTag();

    private List<CompoundTag> worldMaps = new ArrayList<>();

    public RiverWorld(final Map<Long, Chunk> chunks, final CompoundTag extraData, final List<CompoundTag> worldMaps) {
        this.chunks = chunks;
        this.extraData = extraData;
        this.worldMaps = worldMaps;

    }

    public RiverWorld() {

    }

    public static void main(final String[] args) throws Exception {

        final RiverWorld world = new RiverWorld();

        world.updateChunk(new Chunk(0, 1));
        world.updateChunk(new Chunk(0, 1));
        world.updateChunk(new Chunk(1, 2));
        world.updateChunk(new Chunk(1, 2));

        final long start = System.currentTimeMillis();

        final File file = new File("/Users/cinax/Desktop/test.river");

        if (!file.exists()) {
            file.createNewFile();
        }

        final RandomAccessFile accessFile = new RandomAccessFile(file, "rw");

        accessFile.seek(0); // Make sure we're at the start of the file
        accessFile.setLength(0); // Delete old data

        accessFile.close();

        System.out.println(System.currentTimeMillis() - start + " ms");

    }

    public void updateChunk(final Chunk chunk) {
        synchronized (this.chunks) {
            this.chunks.put((long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX(), chunk);
        }
    }

    public Chunk getChunk(final int x, final int z) {
        synchronized (this.chunks) {
            final Long index = (long) z * Integer.MAX_VALUE + (long) x;

            return this.chunks.get(index);
        }
    }

    public List<CompoundTag> getWorldMaps() {
        return this.worldMaps;
    }

    public CompoundTag getExtraData() {
        return this.extraData;
    }

    public byte getWorldVersion() {
        return this.worldVersion;
    }

    public Map<Long, Chunk> getChunks() {
        return this.chunks;
    }

}
