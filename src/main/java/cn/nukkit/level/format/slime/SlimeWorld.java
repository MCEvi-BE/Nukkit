package cn.nukkit.level.format.slime;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.format.anvil.util.BlockStorage;
import cn.nukkit.level.format.anvil.util.NibbleArray;
import cn.nukkit.level.format.generic.EmptyChunkSection;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.tag.*;
import cn.nukkit.swm.api.world.SlimeChunk;
import cn.nukkit.swm.api.world.properties.SlimePropertyMap;
import com.github.luben.zstd.Zstd;

import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class SlimeWorld {


    private final static byte[] SLIME_HEADER = {-79,11};
    private final static byte VERSION = 9;
    private static SlimePropertyMap propertyMap;

    private Map<Long,Chunk> chunks = new HashMap<>();
    private CompoundTag extraData = new CompoundTag("");
    private List<CompoundTag> maps = new ArrayList<>();

    private byte version;


    public static byte[] getSlimeHeader() {
        return SLIME_HEADER;
    }

    public static byte getVERSION() {
        return VERSION;
    }

    public Map<Long, Chunk> getChunks() {
        return chunks;
    }

    public CompoundTag getExtraData() {
        return extraData;
    }

    public List<CompoundTag> getMaps() {
        return maps;
    }

    public byte getVersion() {
        return version;
    }

    public static void maijn(String[] args) throws Exception {

        System.out.println("======== LOAD  ======");
        loadtest();
        System.out.println("======== LOAD  ======");
    }

    public static void loadtest() throws Exception {
        RandomAccessFile file = new RandomAccessFile(new File("/Users/cinax/Desktop/world.slime"), "rw");

        byte[] serializedWorld = new byte[(int) file.length()];
        file.seek(0); // Make sure we're at the start of the file
        file.readFully(serializedWorld);



        SlimeWorld world = SlimeWorld.deserializeWorld("test", serializedWorld);

        System.out.println(world.chunks.size());
    }

    public static void savetest() {


        try {
            RandomAccessFile file = new RandomAccessFile(new File("/Users/cinax/Desktop/seksi.world"), "rw");

            System.out.println("b");


            SlimeLoader loader = new SlimeLoader("/Users/cinax/Desktop/world/",0,0);
            SlimeWorld world = new SlimeWorld();


            int r = 0;
            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {

                    if (loader.chunkExists(x,z)) {

                        Chunk chunk = loader.readChunk(x,z);

                        world.updateChunk(chunk);
                        r++;

                    }

                }
            }

            System.out.println(r + " chunk");
            byte[] a = world.serialize();

            System.out.println(a.length + " length");


            file.seek(0); // Make sure we're at the start of the file
            file.setLength(0); // Delete old data
            file.write(a);



            file.close();



        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void updateChunk(Chunk chunk) {

        synchronized (chunks) {
            chunks.put(((long) chunk.getZ()) * Integer.MAX_VALUE + ((long) chunk.getX()), chunk);
        }

    }










    public byte[] serialize() {

        List<Chunk> sortedChunks;

        synchronized (chunks) {
            sortedChunks = new ArrayList<>(chunks.values());
        }

        sortedChunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        sortedChunks.removeIf(chunk -> chunk == null ||
                Arrays.stream(chunk.getSections()).allMatch(Objects::isNull) ||
                Arrays.stream(chunk.getSections()).allMatch(chunkSection -> chunkSection instanceof EmptyChunkSection)); // Remove empty chunks to save space

        extraData.put("properties", new CompoundTag());

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {

            // File Header and Slime version
            outStream.write(SLIME_HEADER);
            outStream.write(VERSION);

            // World version
            outStream.write(version);

            // Lowest chunk coordinates
            int minX = sortedChunks.stream().mapToInt(Chunk::getX).min().orElse(0);
            int minZ = sortedChunks.stream().mapToInt(Chunk::getZ).min().orElse(0);
            int maxX = sortedChunks.stream().mapToInt(Chunk::getX).max().orElse(0);
            int maxZ = sortedChunks.stream().mapToInt(Chunk::getZ).max().orElse(0);

            outStream.writeShort(minX);
            outStream.writeShort(minZ);


            // Width and depth
            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;

            System.out.println(minX + " " + minZ + " " + maxX + " " + maxZ);

            outStream.writeShort(width);
            outStream.writeShort(depth);

            BitSet chunkBitset = new BitSet(width*depth);

            for (Chunk chunk : sortedChunks) {
                int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);

                chunkBitset.set(bitsetIndex, true);
            }

            // Chunks
            int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
            writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);
            byte[] chunkData = serializeChunks(sortedChunks);
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);



            // Tile Entities
            List<CompoundTag> tileEntitiesList = new ArrayList<>();
            for (Chunk chunk : sortedChunks) {
                for (BlockEntity entity : chunk.getBlockEntities().values()) {
                    entity.saveNBT();
                    tileEntitiesList.add(entity.namedTag);
                }
            }

            ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles");
            tileEntitiesNbtList.setAll(tileEntitiesList);
            CompoundTag tileEntitiesCompound = new CompoundTag("");
            tileEntitiesCompound.putList(tileEntitiesNbtList);
                                       //     CompoundTag tileEntitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);


            // Entities
            List<CompoundTag> entitiesList = new ArrayList<>();

            for (Chunk chunk : sortedChunks) {
                for (Entity entity : chunk.getEntities().values()) {
                    if (!(entity instanceof Player) && !entity.closed) {
                        entity.saveNBT();
                        entitiesList.add(entity.namedTag);
                    }
                }
            }


            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {

                ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities");
                entitiesNbtList.setAll(entitiesList);

                CompoundTag entitiesCompound = new CompoundTag("");
                entitiesCompound.putList(entitiesNbtList);

                byte[] entitiesData = serializeCompoundTag(entitiesCompound);
                byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }



            // Extra Tag
            byte[] extra = serializeCompoundTag(extraData);
            byte[] compressedExtra = Zstd.compress(extra);

            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);


            // World Maps
            ListTag<CompoundTag> maps = new ListTag<>();
            maps.setAll(this.maps);

            CompoundTag mapsCompound = new CompoundTag("");
            mapsCompound.putList(maps);

            byte[] mapArray = serializeCompoundTag(mapsCompound);
            byte[] compressedMapArray = Zstd.compress(mapArray);

            outStream.writeInt(compressedMapArray.length);
            outStream.writeInt(mapArray.length);
            outStream.write(compressedMapArray);

        } catch (Exception e) {
            e.printStackTrace();
        }


        return outByteStream.toByteArray();
    }


    public static void de(int r) {
        System.out.println("> " + r);
    }

    public static SlimeWorld deserializeWorld(String worldName, byte[] serializedWorld) throws Exception{

        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(serializedWorld));
        de(10);

        try {
            byte[] fileHeader = new byte[SLIME_HEADER.length];

            dataStream.read(fileHeader);

            if (!Arrays.equals(SLIME_HEADER, fileHeader)) {
                throw new Exception("HEADER ERROR " + worldName);
            }

            de(11);
            byte worldVersion = dataStream.readByte();

            // Chunk
            short minX = dataStream.readShort();
            short minZ = dataStream.readShort();
            int width = dataStream.readShort();
            int depth = dataStream.readShort();

            System.out.println(minX +" " + minZ + " " + width + " " + depth);
            de(20);
            if (width <= 0 || depth <= 0) {
                throw new Exception("WIDTH DEPTH ERROR");
            }
            de(25);

            int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
            byte[] chunkBitmask = new byte[bitmaskSize];
            dataStream.read(chunkBitmask);
            BitSet chunkBitset = BitSet.valueOf(chunkBitmask);


            int compressedChunkDataLength = dataStream.readInt();
            int chunkDataLength = dataStream.readInt();
            byte[] compressedChunkData = new byte[compressedChunkDataLength];
            byte[] chunkData = new byte[chunkDataLength];

            dataStream.read(compressedChunkData);


            // Tile Entities
            int compressedTileEntitiesLength = dataStream.readInt();
            int tileEntitiesLength = dataStream.readInt();
            byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
            byte[] tileEntities = new byte[tileEntitiesLength];

            dataStream.read(compressedTileEntities);

            // Entities
            byte[] compressedEntities = new byte[0];
            byte[] entities = new byte[0];
            boolean hasEntities = dataStream.readBoolean();

            if (hasEntities) {
                int compressedEntitiesLength = dataStream.readInt();
                int entitiesLength = dataStream.readInt();
                compressedEntities = new byte[compressedEntitiesLength];
                entities = new byte[entitiesLength];

                dataStream.read(compressedEntities);
            }


            // Extra NBT tag
            byte[] compressedExtraTag = new byte[0];
            byte[] extraTag = new byte[0];

                int compressedExtraTagLength = dataStream.readInt();
                int extraTagLength = dataStream.readInt();
                compressedExtraTag = new byte[compressedExtraTagLength];
                extraTag = new byte[extraTagLength];

                dataStream.read(compressedExtraTag);



            // World Map NBT tag
            byte[] compressedMapsTag = new byte[0];
            byte[] mapsTag = new byte[0];

                int compressedMapsTagLength = dataStream.readInt();
                int mapsTagLength = dataStream.readInt();
                compressedMapsTag = new byte[compressedMapsTagLength];
                mapsTag = new byte[mapsTagLength];

                dataStream.read(compressedMapsTag);


            if (dataStream.read() != -1) {
                throw new Exception("wtf goin on");
            }

            // Data decompression
            Zstd.decompress(chunkData, compressedChunkData);
            Zstd.decompress(tileEntities, compressedTileEntities);
            Zstd.decompress(entities, compressedEntities);
            Zstd.decompress(extraTag, compressedExtraTag);
            Zstd.decompress(mapsTag, compressedMapsTag);


            Map<Long, Chunk> chunks = readChunks(worldVersion, minX, minZ, width, depth, chunkBitset, chunkData);



            // Entity deserialization
            CompoundTag entitiesCompound = readCompoundTag(entities);

            if (entitiesCompound != null) {

                ListTag<CompoundTag> entitiesList = entitiesCompound.getList("entities", CompoundTag.class);


                for (CompoundTag entityCompound : entitiesList.getAll()) {

                    ListTag<DoubleTag> listTag = entityCompound.getList("Pos", DoubleTag.class);

                    int chunkX = floor(listTag.get(0).getData()) >> 4;
                    int chunkZ = floor(listTag.get(2).getData()) >> 4;

                    long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                    Chunk chunk = chunks.get(chunkKey);

                    if (chunk == null) {
                        throw new Exception("chunk not found");
                    }

                    chunk.getNBTentities().add(entityCompound);
                }
            }


            // Tile Entity deserialization
            CompoundTag tileEntitiesCompound = readCompoundTag(tileEntities);

            if (tileEntitiesCompound != null) {

                ListTag<CompoundTag> tileEntitiesList = tileEntitiesCompound.getList("tiles", CompoundTag.class) ;

                for (CompoundTag tileEntityCompound : tileEntitiesList.getAll()) {

                    int chunkX = tileEntitiesCompound.getInt("x") >> 4;
                    int chunkZ = tileEntitiesCompound.getInt("x") >> 4;

                    long chunkKey = ((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX);
                    Chunk chunk = chunks.get(chunkKey);

                    if (chunk == null) {
                        throw new Exception(" bulamadım q");
                    }

                    chunk.getNBTtiles().add(tileEntityCompound);
                }
            }

            // Extra Data
            CompoundTag extraCompound = readCompoundTag(extraTag);

            if (extraCompound == null) {
                extraCompound = new CompoundTag("");
            }


            // World Maps
            CompoundTag mapsCompound = readCompoundTag(mapsTag);
            List<CompoundTag> mapList = new ArrayList<>();

            if (mapsCompound != null) {
                List<CompoundTag> finalMapList = new ArrayList<>();
                mapsCompound.getList("maps", CompoundTag.class).getAll().forEach(compoundTag -> finalMapList.add(compoundTag));
                mapList = finalMapList;
            } else {
                mapList = new ArrayList<>();
            }


            // World properties
            SlimePropertyMap worldPropertyMap = null;
            Optional<CompoundTag> propertiesTag = Optional.of(extraCompound.getCompound("properties")).flatMap(d -> Optional.empty());


            if (propertiesTag.isPresent()) {
                worldPropertyMap = SlimePropertyMap.fromCompound(propertiesTag.get());
                worldPropertyMap.merge(propertyMap); // Override world properties
            } else if (propertyMap == null) { // Make sure the property map is never null
                worldPropertyMap = new SlimePropertyMap();
            }

            return new SlimeWorld(chunks, extraCompound, mapList, (byte) 0);
        } catch (Exception e) {
            System.out.println(e);
        }

        return new SlimeWorld();
    }

    public SlimeWorld() {
    }

    public SlimeWorld(Map<Long, Chunk> chunks, CompoundTag extraData, List<CompoundTag> maps, byte version) {
        this.chunks = chunks;

        System.out.println(chunks.size() + " chunks");
        this.extraData = extraData;
        this.maps = maps;
        this.version = version;
    }

    private static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    public static byte[] serializeChunks(List<Chunk> chunks)
    throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);


        for (Chunk chunk : chunks) {

            //Height Maps
            byte[] heightMap = chunk.getHeightMapArray();
            for (int i = 0; i < 256; i++) {
                outStream.writeInt(heightMap[i]);
            }

            //Biomes
            byte[] biomes = chunk.getBiomeIdArray();
            for (byte biome : biomes) {
                outStream.writeInt(biome);
            }

            //Chunk Sections
            cn.nukkit.level.format.ChunkSection[] sections = chunk.getSections();
            BitSet sectionBitMask = new BitSet(16);

            for (int i = 0; i < sections.length; i++) {
                sectionBitMask.set(i, sections[i] != null);
            }

            writeBitSetAsBytes(outStream, sectionBitMask, 2);

            for (cn.nukkit.level.format.ChunkSection section : sections) {

                if (section == null) continue;

                //Block Light
                boolean hasBlockLight = section.getLightArray() != null;
                outStream.writeBoolean(hasBlockLight);

                if (hasBlockLight) {
                    outStream.write(section.getLightArray());
                }

                outStream.write(section.getIdArray());
                outStream.write(section.getDataArray());

                boolean hasSkyLight = section.getSkyLightArray() != null;
                outStream.writeBoolean(hasSkyLight);

                if (hasSkyLight) {
                    outStream.write(section.getSkyLightArray());
                }

            }

        }


        return outByteStream.toByteArray();
    }

    private static byte[] serializeCompoundTag(CompoundTag tag) throws IOException {
        if (tag == null || tag.getAllTags().isEmpty()) {
            return new byte[0];
        }


        /*
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream = new NBTOutputStream(outByteStream, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);
*/
        return NBTIO.write(tag, ByteOrder.BIG_ENDIAN);
    }


    private static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize) throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }

    private static ChunkSection[] readChunkSections(DataInputStream dataStream,  int version) throws IOException {
        ChunkSection[] chunkSectionArray = new ChunkSection[16];
        byte[] sectionBitmask = new byte[2];
        dataStream.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitset.get(i)) {

                // Block Light Nibble Array
                NibbleArray blockLightArray;

                boolean hasBlockLight = dataStream.readBoolean();

                if (version < 5 || hasBlockLight) {
                    byte[] blockLightByteArray = new byte[2048];
                    dataStream.read(blockLightByteArray);
                    blockLightArray = new NibbleArray((blockLightByteArray));
                } else {
                    blockLightArray = null;
                }

                // Block data
                byte[] blockArray;
                NibbleArray dataArray;

                blockArray = new byte[4096];
                dataStream.read(blockArray);

                    // Block Data Nibble Array
                byte[] dataByteArray = new byte[2048];
                dataStream.read(dataByteArray);
                dataArray = new NibbleArray((dataByteArray));

                // Sky Light Nibble Array
                NibbleArray skyLightArray;
                boolean hasSkyLight = dataStream.readBoolean();

                if (version < 5 || hasBlockLight) {
                    byte[] skyLightByteArray = new byte[2048];
                    dataStream.read(skyLightByteArray);
                    skyLightArray = new NibbleArray((skyLightByteArray));
                } else {
                    skyLightArray = null;
                }



                BlockStorage storage = new BlockStorage(blockArray, dataArray);

                ChunkSection section = new ChunkSection(i, storage, blockLightArray.getData(), skyLightArray.getData(), null,hasBlockLight, hasSkyLight);

                chunkSectionArray[i] = section;
            }
        }

        return chunkSectionArray;
    }

    private static Map<Long, Chunk> readChunks(int version, int minX, int minZ, int width, int depth, BitSet chunkBitset, byte[] chunkData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Map<Long, Chunk> chunkMap = new HashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int bitsetIndex = z * width + x;

                if (chunkBitset.get(bitsetIndex)) {
                    // Height Maps
                    byte[] heightMap = new byte[256];
                    for (int i = 0; i < 256; i++) {
                        heightMap[i] = (byte) dataStream.readInt();
                    }

                    // Biome array
                    byte[] biomes = new byte[256];;
                    for (int i = 0; i < biomes.length; i++) {
                        biomes[i] = (byte) dataStream.readInt();
                    }

                    // Chunk Sections
                    ChunkSection[] sections = readChunkSections(dataStream, version);

                    Chunk chunk = new Chunk(minX + z, minZ + z, heightMap, biomes, sections);


                    chunkMap.put(((long) minZ + z) * Integer.MAX_VALUE + ((long) minX + x), chunk);
                }
            }
        }

        return chunkMap;
    }


    public Chunk getChunk(int x, int z) {
        synchronized (chunks) {
            Long index = (((long) z) * Integer.MAX_VALUE + ((long) x));

            return chunks.get(index);
        }
    }


    private static CompoundTag readCompoundTag(byte[] serializedCompound) throws IOException {
        if (serializedCompound.length == 0) {
            return null;
        }

        return NBTIO.read(serializedCompound);
       // NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(serializedCompound), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);

    //    return (CompoundTag) stream.readTag();
    }
}
