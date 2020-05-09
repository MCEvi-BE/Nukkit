package cn.nukkit.level.format.river;

import cn.nukkit.nbt.tag.CompoundTag;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RiverWorld {     private final byte worldVersion = 0x2;   public static final byte[] HEADER = new byte[]{-79,11};public static final byte VERSION = (byte)9;

    private Map<Long, Chunk> chunks = new HashMap<>();

    private CompoundTag extraData = new CompoundTag();
    private List<CompoundTag> worldMaps = new ArrayList<>();



    public static void main(String[] args) throws Exception {


        RiverWorld world = new RiverWorld();

        world.updateChunk(new Chunk(0,1));
        world.updateChunk(new Chunk(0,1));
        world.updateChunk(new Chunk(1,2));
        world.updateChunk(new Chunk(1,2));


        long start = System.currentTimeMillis();





        File file = new File("/Users/cinax/Desktop/test.river");

        if (!file.exists())
            file.createNewFile();

        RandomAccessFile accessFile = new RandomAccessFile(file,"rw");


        accessFile.seek(0); // Make sure we're at the start of the file
        accessFile.setLength(0); // Delete old data


        accessFile.close();

        System.out.println(System.currentTimeMillis() - start + " ms");

    }

    public void updateChunk(Chunk chunk) {
        synchronized (chunks) {
            chunks.put(((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()), chunk);
        }
    }

    public Chunk getChunk(int x, int z) {
        synchronized (chunks) {
            Long index = (((long) z) * Integer.MAX_VALUE + ((long) x));

            return chunks.get(index);
        }
    }



    public RiverWorld(Map<Long, Chunk> chunks, CompoundTag extraData, List<CompoundTag> worldMaps) {
        this.chunks = chunks;
        this.extraData = extraData;
        this.worldMaps = worldMaps;

    }

    public RiverWorld() {

    }

    public List<CompoundTag> getWorldMaps() {
        return worldMaps;
    }


    public CompoundTag getExtraData() {
        return extraData;
    }

    public byte getWorldVersion() {
        return worldVersion;
    }

    public Map<Long, Chunk> getChunks() {
        return chunks;
    }
}
