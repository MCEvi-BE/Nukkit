package cn.nukkit.level.format.river;

import cn.nukkit.block.Block;
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.generic.BaseChunk;
import cn.nukkit.level.format.generic.EmptyChunkSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public final class RiverChunk extends BaseChunk {

    protected boolean terrainPopulated;

    protected boolean terrainGenerated;

    public RiverChunk(final LevelProvider provider, final int x, final int z) {
        this.provider = provider;
        this.providerClass = River.class;
        this.setPosition(x, z);
        this.heightMap = new byte[256];
        this.biomes = new byte[256];
        Arrays.fill(this.heightMap, (byte) 255);
        this.extraData = new HashMap<>();
        this.sections = new ChunkSection[16];
        try {
            System.arraycopy(EmptyChunkSection.EMPTY, 0, this.sections, 0, 16);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        this.NBTentities = null;
        this.NBTtiles = null;
    }

    public RiverChunk(final int x, final int z, final ChunkSection[] sections, final byte[] heightMap, final byte[] biomes) {
        this.providerClass = River.class;
        this.x = x;
        this.z = z;
        this.heightMap = heightMap;
        this.biomes = biomes;
        this.sections = sections;
        this.NBTentities = new ArrayList<>();
        this.NBTtiles = new ArrayList<>();
    }

    public static RiverChunk getEmptyChunk(final int chunkX, final int chunkZ) {
        return RiverChunk.getEmptyChunk(chunkX, chunkZ, null);
    }

    public static RiverChunk getEmptyChunk(final int chunkX, final int chunkZ, final LevelProvider provider) {
        try {
            final RiverChunk chunk = new RiverChunk(provider, chunkX, chunkZ);
            chunk.setPosition(chunkX, chunkZ);

            chunk.heightMap = new byte[256];
//            chunk.lightPopulated = false;
            return chunk;
        } catch (final Exception e) {
            return null;
        }
    }

    @Override
    public RiverChunk clone() {
        return (RiverChunk) super.clone();
    }

    @Override
    public int getBlockSkyLight(final int x, final int y, final int z) {
        final ChunkSection section = this.sections[y >> 4];
        if (section instanceof RiverChunkSection) {
            final RiverChunkSection anvilSection = (RiverChunkSection) section;
            if (anvilSection.skyLight != null) {
                return section.getBlockSkyLight(x, y & 0x0f, z);
            } else if (!anvilSection.hasSkyLight) {
                return 0;
            } else {
                final int height = this.getHighestBlockAt(x, z);
                if (height < y) {
                    return 15;
                } else if (height == y) {
                    return Block.transparent[this.getBlockId(x, y, z)] ? 15 : 0;
                } else {
                    return section.getBlockSkyLight(x, y & 0x0f, z);
                }
            }
        } else {
            return section.getBlockSkyLight(x, y & 0x0f, z);
        }
    }

    @Override
    public int getBlockLight(final int x, final int y, final int z) {
        final ChunkSection section = this.sections[y >> 4];
        if (section instanceof RiverChunkSection) {
            final RiverChunkSection anvilSection = (RiverChunkSection) section;
            if (anvilSection.blockLight != null) {
                return section.getBlockLight(x, y & 0x0f, z);
            } else if (!anvilSection.hasBlockLight) {
                return 0;
            } else {
                return section.getBlockLight(x, y & 0x0f, z);
            }
        } else {
            return section.getBlockLight(x, y & 0x0f, z);
        }
    }

    @Override
    public boolean isPopulated() {
        return this.terrainPopulated;
    }

    @Override
    public void setPopulated() {
        this.setPopulated(true);
    }

    @Override
    public void setPopulated(final boolean value) {
        if (value != this.terrainPopulated) {
            this.terrainPopulated = value;
            this.setChanged();
        }
    }

    @Override
    public boolean isGenerated() {
        return this.terrainGenerated || this.terrainPopulated;
    }

    @Override
    public void setGenerated() {
        this.setGenerated(true);
    }

    @Override
    public void setGenerated(final boolean value) {
        if (this.terrainGenerated != value) {
            this.terrainGenerated = value;
            this.setChanged();
        }
    }

    @Override
    public byte[] toBinary() {
        return new byte[0];
    }

    @Override
    public byte[] toFastBinary() {
        return new byte[0];
    }

    @Override
    public boolean compress() {
        super.compress();
        boolean result = false;
        for (final ChunkSection section : this.getSections()) {
            if (section instanceof RiverChunkSection) {
                final RiverChunkSection anvilSection = (RiverChunkSection) section;
                if (!anvilSection.isEmpty()) {
                    result |= anvilSection.compress();
                }
            }
        }
        return result;
    }

}
