package cn.nukkit.level.format.slime;

import cn.nukkit.Player;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.entity.Entity;
import cn.nukkit.level.format.generic.EmptyChunkSection;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.nbt.tag.Tag;
import cn.nukkit.swm.api.world.SlimeChunk;
import com.github.luben.zstd.Zstd;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class SlimeWorld {


    private final static byte[] SLIME_HEADER = {-79,11};
    private final static byte VERSION = 9;

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

    public static void main(String[] args) throws IOException {


        SlimeLoader loader = new SlimeLoader("/Users/cinax/Desktop/world/", 0,-1);
        SlimeWorld world = new SlimeWorld();



        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {

                if (loader.chunkExists(x,z)) {
                    Chunk chunk = loader.readChunk(x,z);
                    System.out.println("found " + x + ", " + z);
                    world.updateChunk(chunk);

                }

            }
        }





        byte[] ser = world.serialize();

        File file = new File("/Users/cinax/Desktop/seksi.world");
        if (!file.exists())
            file.createNewFile();

        Files.write(file.toPath(), ser);

        System.out.print(ser.length);

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

            outStream.writeShort(width);
            outStream.writeShort(depth);

            BitSet chunkBitset = new BitSet(width*depth);

            for (Chunk chunk : sortedChunks) {
                int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);

                chunkBitset.set(bitsetIndex, true);
            }

            // Chunksspo
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

}
