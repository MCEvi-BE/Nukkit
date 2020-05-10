package cn.nukkit.level.format.river;

import cn.nukkit.block.Block;
import cn.nukkit.level.format.generic.BaseChunk;
import cn.nukkit.level.format.generic.EmptyChunkSection;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class Chunk extends BaseChunk {

    @Override
    public Chunk clone() {
        return (Chunk) super.clone();
    }

    public Chunk(int x, int z) {

        this.x = x;
        this.z = z;

        this.heightMap = new byte[256];
        this.biomes = new byte[256];

        Arrays.fill(this.heightMap, (byte) 255);

        this.sections = new cn.nukkit.level.format.ChunkSection[16];
        System.arraycopy(EmptyChunkSection.EMPTY, 0, this.sections, 0, 16);


    }

    public Chunk(int x, int z, cn.nukkit.level.format.ChunkSection[] sections,byte[] heightMap, byte[] biomes) {

        this.x = x;
        this.z = z;

        this.heightMap = heightMap;
        this.biomes = biomes;

        this.sections = sections;

        this.NBTentities = new ArrayList<>();
        this.NBTtiles = new ArrayList<>();

    }

    @Override
    public boolean isPopulated() {
        return true;
    }

    @Override
    public void setPopulated() {
        this.setPopulated(true);
    }

    @Override
    public void setPopulated(boolean value) {

    }

    @Override
    public boolean isGenerated() {
        return true;
    }

    @Override
    public void setGenerated() {
        this.setGenerated(true);
    }

    @Override
    public void setGenerated(boolean value) {

    }

    @Override
    public byte[] toFastBinary() {
        return new byte[0];
    }

    @Override
    public byte[] toBinary() {
      return new byte[0];
    }

    @Override
    public int getBlockSkyLight(int x, int y, int z) {
        cn.nukkit.level.format.ChunkSection section = this.sections[y >> 4];
        if (section instanceof RiverChunkSection) {
            RiverChunkSection anvilSection = (RiverChunkSection) section;
            if (anvilSection.skyLight != null) {
                return section.getBlockSkyLight(x, y & 0x0f, z);
            } else if (!anvilSection.hasSkyLight) {
                return 0;
            } else {
                int height = getHighestBlockAt(x, z);
                if (height < y) {
                    return 15;
                } else if (height == y) {
                    return Block.transparent[getBlockId(x, y, z)] ? 15 : 0;
                } else {
                    return section.getBlockSkyLight(x, y & 0x0f, z);
                }
            }
        } else {
            return section.getBlockSkyLight(x, y & 0x0f, z);
        }
    }

    @Override
    public int getBlockLight(int x, int y, int z) {
        cn.nukkit.level.format.ChunkSection section = this.sections[y >> 4];
        if (section instanceof RiverChunkSection) {
            RiverChunkSection anvilSection = (RiverChunkSection) section;
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
    public boolean compress() {
        super.compress();
        boolean result = false;
        for (cn.nukkit.level.format.ChunkSection section : getSections()) {
            if (section instanceof RiverChunkSection) {
                RiverChunkSection anvilSection = (RiverChunkSection) section;
                if (!anvilSection.isEmpty()) {
                    result |= anvilSection.compress();
                }
            }
        }
        return result;
    }













}
