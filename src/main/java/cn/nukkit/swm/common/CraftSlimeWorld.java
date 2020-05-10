package cn.nukkit.swm.common;

import cn.nukkit.Server;
import cn.nukkit.nbt.stream.NBTOutputStream;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.swm.api.exceptions.UnknownWorldException;
import cn.nukkit.swm.api.exceptions.WorldAlreadyExistsException;
import cn.nukkit.swm.api.exceptions.WorldInUseException;
import cn.nukkit.swm.api.loaders.SlimeLoader;
import cn.nukkit.swm.api.utils.SlimeFormat;
import cn.nukkit.swm.api.world.SlimeChunk;
import cn.nukkit.swm.api.world.SlimeChunkSection;
import cn.nukkit.swm.api.world.SlimeWorld;
import cn.nukkit.swm.api.world.properties.SlimePropertyMap;
import cn.nukkit.swm.loader.FileLoader;
import com.github.luben.zstd.Zstd;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CraftSlimeWorld implements SlimeWorld {

    private final String name;

    private final Map<Long, SlimeChunk> chunks;

    private final CompoundTag extraData;

    private final List<CompoundTag> worldMaps;

    private final SlimePropertyMap propertyMap;

    private final boolean readOnly;

    private final boolean locked;

    private SlimeLoader loader;

    private byte version;

    public static void main(final String[] args) throws IOException {
        final File file = new File("C:\\Users\\hasan\\AppData\\Roaming\\.infumia\\saves");
        final FileLoader loader = new FileLoader(file);
        final CraftSlimeWorld world = new CraftSlimeWorld("test", new HashMap<>(), new CompoundTag(),
            new ArrayList<>(), new SlimePropertyMap(), false, false, loader, (byte) 2);
        loader.saveWorld("test",  world.serialize(), false);
    }

    private static void writeBitSetAsBytes(final DataOutputStream outStream, final BitSet set, final int fixedSize) throws IOException {
        final byte[] array = set.toByteArray();
        outStream.write(array);

        final int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
    }

    private static byte[] serializeChunks(final List<SlimeChunk> chunks, final byte worldVersion) throws IOException {
        final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        final DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (final SlimeChunk chunk : chunks) {
            // Height Maps
            if (worldVersion >= 0x04) {
                final byte[] heightMaps = CraftSlimeWorld.serializeCompoundTag(chunk.getHeightMaps());
                outStream.writeInt(heightMaps.length);
                outStream.write(heightMaps);
            } else {
                final int[] heightMap = chunk.getHeightMaps().getIntArray("heightMap");

                for (int i = 0; i < 256; i++) {
                    outStream.writeInt(heightMap[i]);
                }
            }

            // Biomes
            final int[] biomes = chunk.getBiomes();
            if (worldVersion >= 0x04) {
                outStream.writeInt(biomes.length);
            }

            for (final int biome : biomes) {
                outStream.writeInt(biome);
            }

            // Chunk sections
            final SlimeChunkSection[] sections = chunk.getSections();
            final BitSet sectionBitmask = new BitSet(16);

            for (int i = 0; i < sections.length; i++) {
                sectionBitmask.set(i, sections[i] != null);
            }

            CraftSlimeWorld.writeBitSetAsBytes(outStream, sectionBitmask, 2);

            for (final SlimeChunkSection section : sections) {
                if (section == null) {
                    continue;
                }

                // Block Light
                final boolean hasBlockLight = section.getBlockLight() != null;
                outStream.writeBoolean(hasBlockLight);

                if (hasBlockLight) {
                    outStream.write(section.getBlockLight().getBacking());
                }

                // Block Data
                if (worldVersion >= 0x04) {
                    // Palette
                    final List<CompoundTag> palette = section.getPalette().getAll();
                    outStream.writeInt(palette.size());

                    for (final CompoundTag value : palette) {
                        final byte[] serializedValue = CraftSlimeWorld.serializeCompoundTag(value);

                        outStream.writeInt(serializedValue.length);
                        outStream.write(serializedValue);
                    }

                    // Block states
                    final long[] blockStates = section.getBlockStates();

                    outStream.writeInt(blockStates.length);

                    for (final long value : section.getBlockStates()) {
                        outStream.writeLong(value);
                    }
                } else {
                    outStream.write(section.getBlocks());
                    outStream.write(section.getData().getBacking());
                }

                // Sky Light
                final boolean hasSkyLight = section.getSkyLight() != null;
                outStream.writeBoolean(hasSkyLight);

                if (hasSkyLight) {
                    outStream.write(section.getSkyLight().getBacking());
                }
            }
        }

        return outByteStream.toByteArray();
    }

    private static byte[] serializeCompoundTag(final CompoundTag tag) throws IOException {
        if (tag == null || tag.parseValue().isEmpty()) {
            return new byte[0];
        }
        final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        final NBTOutputStream outStream = new NBTOutputStream(outByteStream, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);

        return outByteStream.toByteArray();
    }

    @Override
    public SlimeChunk getChunk(final int x, final int z) {
        synchronized (this.chunks) {
            final Long index = (long) z * Integer.MAX_VALUE + (long) x;

            return this.chunks.get(index);
        }
    }

    @Override
    public SlimeWorld.SlimeProperties getProperties() {
        return SlimeWorld.SlimeProperties.builder().spawnX(this.propertyMap.getInt(cn.nukkit.swm.api.world.properties.SlimeProperties.SPAWN_X))
            .spawnY(this.propertyMap.getInt(cn.nukkit.swm.api.world.properties.SlimeProperties.SPAWN_Y))
            .spawnZ(this.propertyMap.getInt(cn.nukkit.swm.api.world.properties.SlimeProperties.SPAWN_Z))
            .environment(this.propertyMap.getString(cn.nukkit.swm.api.world.properties.SlimeProperties.ENVIRONMENT))
            .pvp(this.propertyMap.getBoolean(cn.nukkit.swm.api.world.properties.SlimeProperties.PVP))
            .allowMonsters(this.propertyMap.getBoolean(cn.nukkit.swm.api.world.properties.SlimeProperties.ALLOW_MONSTERS))
            .allowAnimals(this.propertyMap.getBoolean(cn.nukkit.swm.api.world.properties.SlimeProperties.ALLOW_ANIMALS))
            .difficulty(Server.getDifficultyFromString(this.propertyMap.getString(cn.nukkit.swm.api.world.properties.SlimeProperties.DIFFICULTY).toLowerCase(Locale.ENGLISH)))
            .readOnly(this.readOnly).build();
    }

    // World Serialization methods

    @Override
    public SlimeWorld clone(final String worldName) {
        try {
            return this.clone(worldName, null);
        } catch (final WorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    @Override
    public SlimeWorld clone(final String worldName, final SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        return this.clone(worldName, loader, true);
    }

    @Override
    public SlimeWorld clone(final String worldName, final SlimeLoader loader, final boolean lock) throws WorldAlreadyExistsException, IOException {
        if (this.name.equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }

        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }

        if (loader != null) {
            if (loader.worldExists(worldName)) {
                throw new WorldAlreadyExistsException(worldName);
            }
        }

        final CraftSlimeWorld world;

        synchronized (this.chunks) {
            world = new CraftSlimeWorld(worldName, new HashMap<>(this.chunks), this.extraData.clone(), new ArrayList<>(this.worldMaps),
                this.propertyMap, loader == null, lock, loader == null ? this.loader : loader, this.version);
        }

        if (loader != null) {
            loader.saveWorld(worldName, world.serialize(), lock);
        }

        return world;
    }

    public void updateChunk(final SlimeChunk chunk) {
        if (!chunk.getWorldName().equals(this.getName())) {
            throw new IllegalArgumentException("Chunk (" + chunk.getX() + ", " + chunk.getZ() + ") belongs to world '"
                + chunk.getWorldName() + "', not to '" + this.getName() + "'!");
        }

        synchronized (this.chunks) {
            this.chunks.put((long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX(), chunk);
        }
    }

    public byte[] serialize() {
        final List<SlimeChunk> sortedChunks;

        synchronized (this.chunks) {
            sortedChunks = new ArrayList<>(this.chunks.values());
        }

        sortedChunks.sort(Comparator.comparingLong(chunk -> (long) chunk.getZ() * Integer.MAX_VALUE + (long) chunk.getX()));
        sortedChunks.removeIf(chunk -> chunk == null || Arrays.stream(chunk.getSections()).allMatch(Objects::isNull)); // Remove empty chunks to save space

        // Store world properties
        this.extraData.parseValue().put("properties", this.propertyMap.toCompound());

        final ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        final DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SlimeFormat.SLIME_HEADER);
            outStream.write(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeByte(this.version);

            // Lowest chunk coordinates
            final int minX = sortedChunks.stream().mapToInt(SlimeChunk::getX).min().orElse(0);
            final int minZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).min().orElse(0);
            final int maxX = sortedChunks.stream().mapToInt(SlimeChunk::getX).max().orElse(0);
            final int maxZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).max().orElse(0);

            outStream.writeShort(minX);
            outStream.writeShort(minZ);

            // Width and depth
            final int width = maxX - minX + 1;
            final int depth = maxZ - minZ + 1;

            outStream.writeShort(width);
            outStream.writeShort(depth);

            // Chunk Bitmask
            final BitSet chunkBitset = new BitSet(width * depth);

            for (final SlimeChunk chunk : sortedChunks) {
                final int bitsetIndex = (chunk.getZ() - minZ) * width + chunk.getX() - minX;

                chunkBitset.set(bitsetIndex, true);
            }

            final int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
            CraftSlimeWorld.writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);

            // Chunks
            final byte[] chunkData = CraftSlimeWorld.serializeChunks(sortedChunks, this.version);
            final byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            // Tile Entities
            final List<CompoundTag> tileEntitiesList = sortedChunks.stream()
                .flatMap(chunk -> chunk.getTileEntities().stream())
                .collect(Collectors.toList());
            final ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles", tileEntitiesList);
            final CompoundTag tileEntitiesCompound = new CompoundTag("");
            final List<ListTag<CompoundTag>> listTags = Collections.singletonList(tileEntitiesNbtList);
            listTags.forEach(compoundTagListTag ->
                tileEntitiesCompound.put(compoundTagListTag.getName(), compoundTagListTag));
            final byte[] tileEntitiesData = CraftSlimeWorld.serializeCompoundTag(tileEntitiesCompound);
            final byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);

            // Entities
            final List<CompoundTag> entitiesList = sortedChunks.stream().flatMap(chunk -> chunk.getEntities().stream()).collect(Collectors.toList());

            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {
                final ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities");
                entitiesList.forEach(entitiesNbtList::add);
                final CompoundTag entitiesCompound = new CompoundTag("");
                Collections.singletonList(entitiesNbtList).forEach(compoundTagListTag ->
                    entitiesCompound.put(compoundTagListTag.getName(), compoundTagListTag));
                final byte[] entitiesData = CraftSlimeWorld.serializeCompoundTag(entitiesCompound);
                final byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }

            // Extra Tag
            final byte[] extra = CraftSlimeWorld.serializeCompoundTag(this.extraData);
            final byte[] compressedExtra = Zstd.compress(extra);

            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);

            final CompoundTag mapsCompound = new CompoundTag("");
            final ListTag<CompoundTag> maps = new ListTag<>("maps");
            this.worldMaps.forEach(maps::add);
            mapsCompound.put("maps", maps);

            final byte[] mapArray = CraftSlimeWorld.serializeCompoundTag(mapsCompound);
            final byte[] compressedMapArray = Zstd.compress(mapArray);

            outStream.writeInt(compressedMapArray.length);
            outStream.writeInt(mapArray.length);
            outStream.write(compressedMapArray);
        } catch (final IOException ex) { // Ignore
            ex.printStackTrace();
        }

        return outByteStream.toByteArray();
    }

}
