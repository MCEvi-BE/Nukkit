package cn.nukkit.level.format.river.serialize;

import cn.nukkit.Player;
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.anvil.util.BlockStorage;
import cn.nukkit.level.format.anvil.util.NibbleArray;
import cn.nukkit.level.format.river.Chunk;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.DoubleTag;
import cn.nukkit.nbt.tag.ListTag;
import com.github.luben.zstd.Zstd;

import java.io.*;
import java.nio.ByteOrder;
import java.util.*;

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




        //Decompress-------------------------------------
        Zstd.decompress(chunkData, compressedChunkData);
        Zstd.decompress(tileEntities, compressedTileEntities);
        Zstd.decompress(entities, compressedEntities);
        Zstd.decompress(extraTag, compressedExtraTag);
        Zstd.decompress(mapsTag, compressedMapsTag);
        //Decompress-------------------------------------

        //Chunk deserialization-------------------------------------
        Map<Long, Chunk> chunks = readChunks(minX, minZ, width, depth, chunkBitset, chunkData);
        //Chunk deserialization-------------------------------------


        //Entities
        CompoundTag entitiesCompound = NBTIO.read(entities, ByteOrder.BIG_ENDIAN);

        ListTag<CompoundTag> entitiesList = entitiesCompound.getList("entities", CompoundTag.class);

        for (CompoundTag tag : entitiesList.getAll())
        {
            ListTag<DoubleTag> listTag = tag.getList("Pos", DoubleTag.class);

            int chunkX = floor(listTag.get(0).getData()) >> 4;
            int chunkZ = floor(listTag.get(2).getData()) >> 4;
            long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
            Chunk chunk = chunks.get(chunkKey);

            if (chunk == null) {
                throw new Exception("fuck entity");
            }

            chunk.getNBTentities().add(tag);
        }


        // Tile Entity deserialization
        CompoundTag tileEntitiesCompound = NBTIO.read(tileEntities);

        if (tileEntitiesCompound != null) {
            ListTag<CompoundTag> tileEntitiesList = tileEntitiesCompound.getList("tiles",CompoundTag.class);

            for (CompoundTag tileEntityCompound : tileEntitiesList.getAll()) {

                int chunkX = tileEntitiesCompound.getInt("x") >> 4;
                int chunkZ = tileEntitiesCompound.getInt("x") >> 4;


                long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                Chunk chunk = chunks.get(chunkKey);

                if (chunk == null) {
                    throw new Exception("fuck tile entity");
                }

                chunk.getNBTtiles().add(tileEntityCompound);
            }
        }


        // Extra Data
        CompoundTag extraCompound = NBTIO.read(extraTag);

        if (extraCompound == null) {
            extraCompound = new CompoundTag("");
        }

        // World Maps
        CompoundTag mapsCompound = NBTIO.read(mapsTag, ByteOrder.BIG_ENDIAN);
        List<CompoundTag> mapList = new ArrayList<>();

        if (mapsCompound != null) {
            mapList = mapsCompound.getList("maps", CompoundTag.class).getAll();
        }


        // World properties


        int compressedMapArrayLength = stream.readInt();
        int MapArrayLength = stream.readInt();

        byte[] compressedMapArray = new byte[compressedMapArrayLength];
        byte[] mapArray = new byte[MapArrayLength];
        stream.read(compressedMapArray);


        Zstd.decompress(compressedMapArray, MapArrayLength);

        CompoundTag map = NBTIO.read(mapArray);

    }


    private static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
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
            chunk.getEntities().values().stream().filter(entity -> !(entity instanceof Player) && !entity.isClosed()).forEach(entity -> {
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



    public static Map<Long, Chunk> readChunks(int minX, int minZ, int width, int depth, BitSet chunkBitSet, byte[] chunkData) throws Exception{
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Map<Long, Chunk> chunkMap = new HashMap<>();


        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {

                int bitsetIndex = z * width + x;

                if (chunkBitSet.get(bitsetIndex))
                {

                    byte[] heightMap = new byte[256];
                    for (int i = 0; i < 256; i++) {
                        heightMap[i] = dataStream.readByte();
                    }

                    int biomeLength = dataStream.readInt();
                    byte[] biomeMap = new byte[biomeLength];

                    for (int i = 0; i < biomeLength; i++) {
                        biomeMap[i] = dataStream.readByte();
                    }


                    ChunkSection[] sections = readChunkSections(dataStream);

                    chunkMap.put(((long) minZ + z) * Integer.MAX_VALUE + ((long) minX + x),
                            new Chunk(minX + x, minZ + z,sections, heightMap, biomeMap));

                }

            }
        }

        return chunkMap;
    }



    private static ChunkSection[] readChunkSections(DataInputStream dataStream) throws Exception {


        ChunkSection[] chunkSectionArray = new ChunkSection[16];
        byte[] sectionBitmask = new byte[2];
        dataStream.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitset.get(i))
            {

                byte[] blockLight = new byte[2048];
                boolean hasBlockLight = dataStream.readBoolean();

                if (hasBlockLight) {
                    dataStream.read(blockLight);
                }

                byte[] blockArray = new byte[4096];
                dataStream.read(blockArray);

                byte[] dataArray = new byte[2048];
                dataStream.read(dataArray);

                boolean hasSkyLight = dataStream.readBoolean();
                byte[] skyLight = new byte[2048];
                if (hasSkyLight) {
                    dataStream.read(skyLight);
                }

                chunkSectionArray[i] = new cn.nukkit.level.format.river.ChunkSection(i, new BlockStorage(blockArray, new NibbleArray(dataArray)) , blockLight, skyLight, null, hasBlockLight,hasSkyLight);

            }
        }


        return chunkSectionArray;
    }
}
