package cn.nukkit.level.format.river.serialize;

import cn.nukkit.Player;
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.anvil.util.BlockStorage;
import cn.nukkit.level.format.anvil.util.NibbleArray;
import cn.nukkit.level.format.river.Chunk;
import cn.nukkit.level.format.river.RiverWorld;
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

    public static void main(final String[] args) throws Exception {

        final List<Chunk> chunks = new ArrayList<>();

        chunks.add(new Chunk(0, 1));
        chunks.add(new Chunk(2, 2));

        final CompoundTag test = new CompoundTag();
        test.putString("test", "lo test lo mest lo");

        final byte[] data = TestSerialize.serialize(chunks, test, new ArrayList<>());

        TestSerialize.deserialize(data);

    }

    public static RiverWorld deserialize(final byte[] data) throws Exception {

        final DataInputStream stream = new DataInputStream(new ByteArrayInputStream(data));

        //Head------------------------------------
        final byte header = stream.readByte();
        final byte version = stream.readByte();
        //Head------------------------------------

        //LowMax------------------------------------
        final int minX = stream.readShort();
        final int minZ = stream.readShort();
        final int width = stream.readShort();
        final int depth = stream.readShort();
        //LowMax------------------------------------

        //BitSet------------------------------------
        final int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
        final byte[] chunkBitmask = new byte[bitmaskSize];
        stream.read(chunkBitmask);
        final BitSet chunkBitset = BitSet.valueOf(chunkBitmask);
        //BitSet------------------------------------

        //Chunks------------------------------------
        final int compressedChunkDataLength = stream.readInt();
        final int chunkDataLength = stream.readInt();
        final byte[] compressedChunkData = new byte[compressedChunkDataLength];
        final byte[] chunkData = new byte[chunkDataLength];
        stream.read(compressedChunkData);
        //Chunks------------------------------------

        //Tiles-------------------------------------
        final int compressedTileEntitiesLength = stream.readInt();
        final int tileEntitiesLength = stream.readInt();
        final byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
        final byte[] tileEntities = new byte[tileEntitiesLength];
        stream.read(compressedTileEntities);
        //Tiles-------------------------------------

        //Entities-------------------------------------
        byte[] compressedEntities = new byte[0];
        byte[] entities = new byte[0];

        final boolean hasEntities = stream.readBoolean();

        if (hasEntities) {
            final int compressedEntitiesLength = stream.readInt();
            final int entitiesLength = stream.readInt();
            compressedEntities = new byte[compressedEntitiesLength];
            entities = new byte[entitiesLength];

            stream.read(compressedEntities);
        }
        //Entities-------------------------------------

        //Extra-------------------------------------
        byte[] compressedExtraTag = new byte[0];
        byte[] extraTag = new byte[0];

        final int compressedExtraTagLength = stream.readInt();
        final int extraTagLength = stream.readInt();
        compressedExtraTag = new byte[compressedExtraTagLength];
        extraTag = new byte[extraTagLength];

        stream.read(compressedExtraTag);
        //Extra-------------------------------------

        //LevelDat-------------------------------------
        byte[] compressedMapsTag = new byte[0];
        byte[] mapsTag = new byte[0];

        final int compressedMapsTagLength = stream.readInt();
        final int mapsTagLength = stream.readInt();
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
        final Map<Long, Chunk> chunks = TestSerialize.readChunks(minX, minZ, width, depth, chunkBitset, chunkData);
        //Chunk deserialization-------------------------------------

        //Entities
        if (hasEntities) {
            final CompoundTag entitiesCompound = NBTIO.read(entities, ByteOrder.BIG_ENDIAN);

            final ListTag<CompoundTag> entitiesList = entitiesCompound.getList("entities", CompoundTag.class);

            for (final CompoundTag tag : entitiesList.getAll()) {
                final ListTag<DoubleTag> listTag = tag.getList("Pos", DoubleTag.class);

                final int chunkX = TestSerialize.floor(listTag.get(0).getData()) >> 4;
                final int chunkZ = TestSerialize.floor(listTag.get(2).getData()) >> 4;
                final long chunkKey = (long) chunkZ * Integer.MAX_VALUE + (long) chunkX;
                final Chunk chunk = chunks.get(chunkKey);

                if (chunk == null) {
                    throw new Exception("fuck entity");
                }

                chunk.getNBTentities().add(tag);
            }
        }

        // Tile Entity deserialization
        final CompoundTag tileEntitiesCompound = NBTIO.read(tileEntities);

        if (tileEntitiesCompound != null) {
            final ListTag<CompoundTag> tileEntitiesList = tileEntitiesCompound.getList("tiles", CompoundTag.class);

            for (final CompoundTag tileEntityCompound : tileEntitiesList.getAll()) {

                final int chunkX = tileEntitiesCompound.getInt("x") >> 4;
                final int chunkZ = tileEntitiesCompound.getInt("x") >> 4;

                final long chunkKey = (long) chunkZ * Integer.MAX_VALUE + (long) chunkX;
                final Chunk chunk = chunks.get(chunkKey);

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
        final CompoundTag mapsCompound = NBTIO.read(mapsTag, ByteOrder.BIG_ENDIAN);
        List<CompoundTag> mapList = new ArrayList<>();

        if (mapsCompound != null) {
            mapList = mapsCompound.getList("maps", CompoundTag.class).getAll();
        }

        return new RiverWorld(chunks, extraCompound, mapList);
    }

    public static byte[] serialize(final List<Chunk> chunks, final CompoundTag extraCompound, final List<CompoundTag> worldMap) throws Exception {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        final DataOutputStream outStream = new DataOutputStream(stream);

        chunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        chunks.removeIf(chunk -> chunk == null
            || Arrays.stream(chunk.getSections()).allMatch(Objects::isNull)); // Remove empty chunks to save space

        //Head------------------------------------
        outStream.writeByte(TestSerialize.MAGIC);
        outStream.writeByte(TestSerialize.VERSION);
        //Head------------------------------------

        //LowMax------------------------------------
        final int minX = chunks.stream().mapToInt(Chunk::getX).min().orElse(0);
        final int minZ = chunks.stream().mapToInt(Chunk::getZ).min().orElse(0);
        final int maxX = chunks.stream().mapToInt(Chunk::getX).max().orElse(0);
        final int maxZ = chunks.stream().mapToInt(Chunk::getZ).max().orElse(0);
        //LowMax------------------------------------

        final int width = maxX - minX + 1;
        final int depth = maxZ - minZ + 1;

        outStream.writeShort(minX);
        outStream.writeShort(minZ);
        outStream.writeShort(width);
        outStream.writeShort(depth);

        if (width <= 0 || depth <= 0) {
            throw new Exception("width depth error bro");
        }

        //BitSet------------------------------------
        final BitSet chunkBitset = new BitSet(width * depth);
        for (final Chunk chunk : chunks) {
            final int bitsetIndex = (chunk.getZ() - minZ) * width + chunk.getX() - minX;
            chunkBitset.set(bitsetIndex, true);
        }
        final int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
        TestSerialize.writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);
        //BitSet------------------------------------

        //Chunks------------------------------------
        final byte[] chunkData = TestSerialize.serializeChunks(chunks);
        final byte[] compressedChunkData = Zstd.compress(chunkData);
        outStream.writeInt(compressedChunkData.length);
        outStream.writeInt(chunkData.length);
        outStream.write(compressedChunkData);
        //Chunks------------------------------------

        //Tiles-------------------------------------
        final List<CompoundTag> tileEntitiesList = new ArrayList<>();
        chunks.forEach(chunk -> chunk.getBlockEntities().values().forEach(blockEntity -> {
            blockEntity.saveNBT();
            tileEntitiesList.add(blockEntity.namedTag);
        }));
        final ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles");
        tileEntitiesNbtList.setAll(tileEntitiesList);
        final CompoundTag tag = new CompoundTag();
        tag.putList(tileEntitiesNbtList);
        final byte[] tileEntitiesData = NBTIO.write(tag, ByteOrder.BIG_ENDIAN);
        final byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);
        outStream.writeInt(compressedTileEntitiesData.length);
        outStream.writeInt(tileEntitiesData.length);
        outStream.write(compressedTileEntitiesData);
        //Tiles-------------------------------------

        //Entities-------------------------------------
        final List<CompoundTag> entitiesList = new ArrayList<>();
        chunks.forEach(chunk -> {
            chunk.getEntities().values().stream().filter(entity -> !(entity instanceof Player) && !entity.isClosed()).forEach(entity -> {
                entity.saveNBT();
                entitiesList.add(entity.namedTag);
            });
        });
        outStream.writeBoolean(!entitiesList.isEmpty());
        if (!entitiesList.isEmpty()) {
            final ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities");
            final CompoundTag entitiesCompound = new CompoundTag("");
            entitiesCompound.putList(entitiesNbtList);
            final byte[] entitiesData = NBTIO.write(tag, ByteOrder.BIG_ENDIAN);
            final byte[] compressedEntitiesData = Zstd.compress(entitiesData);
            outStream.writeInt(compressedEntitiesData.length);
            outStream.writeInt(entitiesData.length);
            outStream.write(compressedEntitiesData);
        }
        //Entities-------------------------------------

        //Extra-------------------------------------
        final byte[] extra = NBTIO.write(extraCompound, ByteOrder.BIG_ENDIAN);
        final byte[] compressedExtra = Zstd.compress(extra);

        outStream.writeInt(compressedExtra.length);
        outStream.writeInt(extra.length);
        outStream.write(compressedExtra);
        //Extra-------------------------------------

        //LevelDat-------------------------------------

        final ListTag<CompoundTag> mapList = new ListTag<>("maps");
        mapList.setAll(worldMap);

        final CompoundTag mapsCompound = new CompoundTag("");
        mapsCompound.putList(mapList);

        final byte[] mapArray = NBTIO.write(mapsCompound, ByteOrder.BIG_ENDIAN);
        final byte[] compressedMapArray = Zstd.compress(mapArray);

        outStream.writeInt(compressedMapArray.length);
        outStream.writeInt(mapArray.length);
        outStream.write(compressedMapArray);

        //LevelDat-------------------------------------

        return stream.toByteArray();
    }

    public static Map<Long, Chunk> readChunks(final int minX, final int minZ, final int width, final int depth, final BitSet chunkBitSet, final byte[] chunkData) throws Exception {
        final DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        final Map<Long, Chunk> chunkMap = new HashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {

                final int bitsetIndex = z * width + x;

                if (chunkBitSet.get(bitsetIndex)) {

                    final byte[] heightMap = new byte[256];
                    for (int i = 0; i < 256; i++) {
                        heightMap[i] = dataStream.readByte();
                    }

                    final int biomeLength = dataStream.readInt();
                    final byte[] biomeMap = new byte[biomeLength];

                    for (int i = 0; i < biomeLength; i++) {
                        biomeMap[i] = dataStream.readByte();
                    }

                    final ChunkSection[] sections = TestSerialize.readChunkSections(dataStream);

                    chunkMap.put(((long) minZ + z) * Integer.MAX_VALUE + (long) minX + x,
                        new Chunk(minX + x, minZ + z, sections, heightMap, biomeMap));

                }

            }
        }

        return chunkMap;
    }

    private static int floor(final double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    private static byte[] serializeChunks(final List<Chunk> chunks) throws Exception {
        final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        final DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (final Chunk chunk : chunks) {
            TestSerialize.serializeChunk(chunk, outStream);
        }

        return outByteStream.toByteArray();
    }

    private static void serializeChunk(final Chunk chunk, final DataOutputStream stream) throws Exception {

        final byte[] heightMap = chunk.getHeightMapArray();
        for (int i = 0; i < 256; i++) {
            stream.writeByte(heightMap[i]);
        }

        final byte[] biomeMap = chunk.getBiomeIdArray();
        stream.writeInt(biomeMap.length);

        for (final byte r : biomeMap) {
            stream.writeByte(r);
        }

        final ChunkSection[] sections = chunk.getSections();
        final BitSet sectionBitmask = new BitSet(16);

        for (int i = 0; i < sections.length; i++) {
            sectionBitmask.set(i, sections[i] != null);
        }

        TestSerialize.writeBitSetAsBytes(stream, sectionBitmask, 2);

        for (final ChunkSection section : sections) {

            if (section == null) {
                continue;
            }

            final boolean hasBlockLight = section.hasBlockLight();
            if (hasBlockLight) {
                stream.write(section.getLightArray());
            }

            final byte[] ids = section.getIdArray();
            stream.write(ids);

            final byte[] data = section.getDataArray();
            stream.write(data);

            final boolean hasSkyLight = section.hasSkyLight();
            if (hasSkyLight) {
                stream.write(section.getSkyLightArray());
            }

        }

    }

    private static void writeBitSetAsBytes(final DataOutputStream outStream, final BitSet set, final int fixedSize) throws IOException {
        final byte[] array = set.toByteArray();
        outStream.write(array);

        final int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }

    private static ChunkSection[] readChunkSections(final DataInputStream dataStream) throws Exception {

        final ChunkSection[] chunkSectionArray = new ChunkSection[16];
        final byte[] sectionBitmask = new byte[2];
        dataStream.read(sectionBitmask);
        final BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitset.get(i)) {

                final byte[] blockLight = new byte[2048];
                final boolean hasBlockLight = dataStream.readBoolean();

                if (hasBlockLight) {
                    dataStream.read(blockLight);
                }

                final byte[] blockArray = new byte[4096];
                dataStream.read(blockArray);

                final byte[] dataArray = new byte[2048];
                dataStream.read(dataArray);

                final boolean hasSkyLight = dataStream.readBoolean();
                final byte[] skyLight = new byte[2048];
                if (hasSkyLight) {
                    dataStream.read(skyLight);
                }

                chunkSectionArray[i] = new cn.nukkit.level.format.river.ChunkSection(i, new BlockStorage(blockArray, new NibbleArray(dataArray)), blockLight, skyLight, null, hasBlockLight, hasSkyLight);

            }
        }

        return chunkSectionArray;
    }

}
