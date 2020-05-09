package cn.nukkit.level.format.river.serialize;

import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.river.Chunk;
import cn.nukkit.level.format.river.RiverWorld;
import cn.nukkit.nbt.tag.CompoundTag;
import com.github.luben.zstd.Zstd;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class RiverSerialize {


    private static final byte[] HEADER = new byte[]{-79,11};
    private static final byte VERSION = (byte) 9;

    private static final byte version = 0x09;

    public static void main(String[] args) throws Exception{

        RiverWorld world = new RiverWorld();

        world.updateChunk(new Chunk(10,9));
        world.updateChunk(new Chunk(8,7));
        world.updateChunk(new Chunk(5,6));
        world.updateChunk(new Chunk(4,3));

        byte[] data = serialize(world);


        System.out.println("======================= reading");
        deserialize(data);
        System.out.println("======================= reading");

    }


    private static byte[] serialize(RiverWorld world) throws Exception{

        List<Chunk> sortedChunks;

        synchronized (world.getChunks()) {
            sortedChunks = new ArrayList<>(world.getChunks().values());
        }

        sortedChunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        sortedChunks.removeIf(chunk -> chunk == null || Arrays.stream(chunk.getSections()).allMatch(Objects::isNull)); // Remove empty chunks to save space

        // Store world properties
        world.getExtraData().put("properties", new CompoundTag());

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);


        // File Header and Slime version
        outStream.write(HEADER);
        outStream.write(VERSION);

        // Lowest chunk coordinates
        int minX = sortedChunks.stream().mapToInt(Chunk::getX).min().orElse(0);
        int minZ = sortedChunks.stream().mapToInt(Chunk::getZ).min().orElse(0);
        int maxX = sortedChunks.stream().mapToInt(Chunk::getX).max().orElse(0);
        int maxZ = sortedChunks.stream().mapToInt(Chunk::getZ).max().orElse(0);

        // Width and depth
        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;





        // Chunk Bitmask
        BitSet chunkBitset = new BitSet(width * depth);

        for (Chunk chunk : sortedChunks) {
            int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);

            chunkBitset.set(bitsetIndex, true);
        }

        int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
        writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);

        System.out.println(chunkMaskSize);

        // Chunks
        byte[] chunkData = serializeChunks(sortedChunks, version);
        byte[] compressedChunkData = Zstd.compress(chunkData);


        outStream.writeInt(compressedChunkData.length);
        outStream.writeInt(chunkData.length);
        outStream.write(compressedChunkData);

        return outByteStream.toByteArray();
    }

    private static RiverWorld deserialize(byte[] serializedWorld) throws Exception {

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(serializedWorld));


        byte[] fileHeader = new byte[HEADER.length];
        dataStream.read(fileHeader);

        if (!Arrays.equals(HEADER, fileHeader)) {
            throw new Exception("header error");
        }

        // Chunk
        int minX = dataStream.readShort();
        int minZ = dataStream.readShort();

        int width = dataStream.readShort();
        int depth = dataStream.readShort();



        System.out.println(minX + " " + minZ);
        System.out.println(width + " " + depth);


        if (width <= 0 || depth <= 0) {
            throw new Exception("width depth error");
        }


        int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
        byte[] chunkBitmask = new byte[bitmaskSize];


        dataStream.read(chunkBitmask);
        BitSet chunkBitset = BitSet.valueOf(chunkBitmask);

        int compressedChunkDataLength = dataStream.readInt();
        int chunkDataLength = dataStream.readInt();
        byte[] compressedChunkData = new byte[compressedChunkDataLength];
        byte[] chunkData = new byte[chunkDataLength];

        dataStream.read(compressedChunkData);


        return new RiverWorld();
    }


    private static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize) throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }



    private static byte[] serializeChunks(List<Chunk> chunks, byte worldVersion) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (Chunk chunk : chunks) {

            // Height Maps

            byte[] heightMap = chunk.getHeightMapArray();

            for (int i = 0; i < 256; i++) {
                outStream.writeInt(heightMap[i]);
            }


            // Biomes
            byte[] biomes = chunk.getBiomeIdArray();
            if (worldVersion >= 0x04) {
                outStream.writeInt(biomes.length);
            }

            for (int biome : biomes) {
                outStream.writeInt(biome);
            }

            // Chunk sections
            ChunkSection[] sections = chunk.getSections();
            BitSet sectionBitmask = new BitSet(16);

            for (int i = 0; i < sections.length; i++) {
                sectionBitmask.set(i, sections[i] != null);
            }

            writeBitSetAsBytes(outStream, sectionBitmask, 2);



            for (ChunkSection section : sections) {
                if (section == null) {
                    continue;
                }

                // Block Light
                boolean hasBlockLight = section.hasBlockLight();
                outStream.writeBoolean(hasBlockLight);

                if (hasBlockLight) {
                    outStream.write(section.getLightArray());
                }

                // Block Data

                outStream.write(section.getIdArray());
                outStream.write(section.getDataArray());


                // Sky Light
                boolean hasSkyLight = section.hasSkyLight();
                outStream.writeBoolean(hasSkyLight);

                if (hasSkyLight) {
                    outStream.write(section.getSkyLightArray());
                }
            }
        }

        return outByteStream.toByteArray();
    }


    public static void debug(String key, Object... o) {
        System.out.println("key: ");
        for (Object r : o) {
            System.out.print(r+", ");
        }
        System.out.println("");
    }

    public static int byteArrayToInt(byte[] b, int offset) {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    private static int readArrayInt() {
        return 0;
    }

    private static void writeArrayInt(int n, DataOutputStream stream) throws Exception {
        byte[] r = ByteBuffer.allocate(4).putInt(n).array();
        stream.writeInt(r.length);
        stream.write(r);
    }
}
