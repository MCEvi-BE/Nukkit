package cn.nukkit.level.format.river.serialize;

import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.river.Chunk;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import com.github.luben.zstd.Zstd;

import java.io.*;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class TestSerialize {

    private static final byte MAGIC = 5;
    private static final byte VERSION = 0x03;


    public static void main(String args[])throws Exception {


        List<Chunk> chunks = new ArrayList<>();

        chunks.add(new Chunk(0,1));
        chunks.add(new Chunk(2,2));

        CompoundTag test = new CompoundTag();
        test.putString("test", "lo test lo mest lo");

        byte[] data = serialize(chunks, test, new ArrayList<>());

        deserialize(data);


    }

    public static void deserialize(byte[] data) throws Exception {

        DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));

        //Head------------------------------------
        byte header = stream.readByte();
        byte version = stream.readByte();
        //Head------------------------------------

        //LowMax------------------------------------
        int minX = stream.readShort();
        int minZ = stream.readShort();
        int width = stream.readShort();
        int depth = stream.readShort();
        //LowMax------------------------------------

        //BitSet------------------------------------
        int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
        byte[] chunkBitmask = new byte[bitmaskSize];
        stream.read(chunkBitmask);
        BitSet chunkBitset = BitSet.valueOf(chunkBitmask);
        //BitSet------------------------------------

        //Chunks------------------------------------
        int compressedChunkDataLength = stream.readInt();
        int chunkDataLength = stream.readInt();
        byte[] compressedChunkData = new byte[compressedChunkDataLength];
        byte[] chunkData = new byte[chunkDataLength];
        stream.read(compressedChunkData);
        //Chunks------------------------------------

        //Tiles-------------------------------------
        int compressedTileEntitiesLength = stream.readInt();
        int tileEntitiesLength = stream.readInt();
        byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
        byte[] tileEntities = new byte[tileEntitiesLength];
        stream.read(compressedTileEntities);
        //Tiles-------------------------------------

        //Entities-------------------------------------
        byte[] compressedEntities = new byte[0];
        byte[] entities = new byte[0];

        boolean hasEntities = stream.readBoolean();

        if (hasEntities) {
            int compressedEntitiesLength = stream.readInt();
            int entitiesLength = stream.readInt();
            compressedEntities = new byte[compressedEntitiesLength];
            entities = new byte[entitiesLength];

            stream.read(compressedEntities);
        }
        //Entities-------------------------------------


        //Extra-------------------------------------
        byte[] compressedExtraTag = new byte[0];
        byte[] extraTag = new byte[0];

        int compressedExtraTagLength = stream.readInt();
        int extraTagLength = stream.readInt();
        compressedExtraTag = new byte[compressedExtraTagLength];
        extraTag = new byte[extraTagLength];

        stream.read(compressedExtraTag);
        //Extra-------------------------------------


        //LevelDat-------------------------------------
        byte[] compressedMapsTag = new byte[0];
        byte[] mapsTag = new byte[0];

        int compressedMapsTagLength = stream.readInt();
        int mapsTagLength = stream.readInt();
        compressedMapsTag = new byte[compressedMapsTagLength];
        mapsTag = new byte[mapsTagLength];

        stream.read(compressedMapsTag);

        //LevelDat-------------------------------------


    }

    public static byte[] serialize(List<Chunk> chunks, CompoundTag extraCompound, List<CompoundTag> worldMap) throws Exception{
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(stream);

        //Head------------------------------------
        outStream.writeByte(MAGIC);
        outStream.writeByte(VERSION);
        //Head------------------------------------

        //LowMax------------------------------------
        int minX = chunks.stream().mapToInt(Chunk::getX).min().orElse(0);
        int minZ = chunks.stream().mapToInt(Chunk::getZ).min().orElse(0);
        int maxX = chunks.stream().mapToInt(Chunk::getX).max().orElse(0);
        int maxZ = chunks.stream().mapToInt(Chunk::getZ).max().orElse(0);
        //LowMax------------------------------------

        int width = maxX - minX + 1;
        int depth = maxZ - minZ + 1;

        outStream.writeShort(minX);
        outStream.writeShort(minZ);
        outStream.writeShort(width);
        outStream.writeShort(depth);

        //BitSet------------------------------------
        BitSet chunkBitset = new BitSet(width * depth);
        for (Chunk chunk : chunks) { int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);chunkBitset.set(bitsetIndex, true); }
        int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
        writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);
        //BitSet------------------------------------

        //Chunks------------------------------------
        byte[] chunkData = serializeChunks(chunks);
        byte[] compressedChunkData = Zstd.compress(chunkData);
        outStream.writeInt(compressedChunkData.length);
        outStream.writeInt(chunkData.length);
        outStream.write(compressedChunkData);
        //Chunks------------------------------------

        //Tiles-------------------------------------
        List<CompoundTag> tileEntitiesList = new ArrayList<>();
        chunks.forEach(chunk -> chunk.getBlockEntities().values().forEach(blockEntity -> {
            blockEntity.saveNBT();
            tileEntitiesList.add(blockEntity.namedTag);
        }));
        ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles");
        tileEntitiesNbtList.setAll(tileEntitiesList);
        CompoundTag tag = new CompoundTag();
        tag.putList(tileEntitiesNbtList);
        byte[] tileEntitiesData = NBTIO.write(tag, ByteOrder.BIG_ENDIAN);
        byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);
        outStream.writeInt(compressedTileEntitiesData.length);
        outStream.writeInt(tileEntitiesData.length);
        outStream.write(compressedTileEntitiesData);
        //Tiles-------------------------------------


        //Entities-------------------------------------
        List<CompoundTag> entitiesList = new ArrayList<>();
        chunks.forEach(chunk -> {
            chunk.getEntities().values().forEach(entity -> {
                entity.saveNBT();
                entitiesList.add(entity.namedTag);
            });
        });
        outStream.writeBoolean(!entitiesList.isEmpty());
        if (!entitiesList.isEmpty()) {
            ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities");
            CompoundTag entitiesCompound = new CompoundTag("");
            entitiesCompound.putList(entitiesNbtList);
            byte[] entitiesData = NBTIO.write(tag, ByteOrder.BIG_ENDIAN);
            byte[] compressedEntitiesData = Zstd.compress(entitiesData);
            outStream.writeInt(compressedEntitiesData.length);
            outStream.writeInt(entitiesData.length);
            outStream.write(compressedEntitiesData);
        }
        //Entities-------------------------------------



        //Extra-------------------------------------
        byte[] extra = NBTIO.write(extraCompound, ByteOrder.BIG_ENDIAN);
        byte[] compressedExtra = Zstd.compress(extra);

        outStream.writeInt(compressedExtra.length);
        outStream.writeInt(extra.length);
        outStream.write(compressedExtra);
        //Extra-------------------------------------

        //LevelDat-------------------------------------

        ListTag<CompoundTag> mapList = new ListTag<>("maps");
        mapList.setAll(worldMap);

        CompoundTag mapsCompound = new CompoundTag("");
        mapsCompound.putList(mapList);

        byte[] mapArray = NBTIO.write(mapsCompound, ByteOrder.BIG_ENDIAN);
        byte[] compressedMapArray = Zstd.compress(mapArray);

        outStream.writeInt(compressedMapArray.length);
        outStream.writeInt(mapArray.length);
        outStream.write(compressedMapArray);
        //LevelDat-------------------------------------


        return stream.toByteArray();
    }




    private static byte[] serializeChunks(List<Chunk> chunks) throws Exception {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (Chunk chunk : chunks) {
            serializeChunk(chunk, outStream);
        }

        return outByteStream.toByteArray();
    }




    private static void serializeChunk(Chunk chunk,DataOutputStream stream) throws Exception {

        byte[] heightMap = chunk.getHeightMapArray();
        for (int i = 0; i < 256; i++) {
            stream.writeByte(heightMap[i]);
        }

        byte[] biomeMap = chunk.getBiomeIdArray();
        stream.writeInt(biomeMap.length);

        for (byte r : biomeMap)
            stream.writeByte(r);

        ChunkSection[] sections = chunk.getSections();
        BitSet sectionBitmask = new BitSet(16);

        for (int i = 0; i < sections.length; i++) {
            sectionBitmask.set(i, sections[i] != null);
        }

        writeBitSetAsBytes(stream, sectionBitmask, 2);

        for (ChunkSection section : sections) {

            if (section == null) continue;


            boolean hasBlockLight = section.hasBlockLight();
            if (hasBlockLight) {
                stream.write(section.getLightArray());
            }

            byte[] ids = section.getIdArray();
            stream.write(ids);

            byte[] data = section.getDataArray();
            stream.write(data);

            boolean hasSkyLight = section.hasSkyLight();
            if (hasSkyLight) {
                stream.write(section.getSkyLightArray());
            }

        }

    }



    private static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize) throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }


}
