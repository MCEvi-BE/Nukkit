package cn.nukkit.swm.common;

import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.swm.api.world.SlimeChunk;
import cn.nukkit.swm.api.world.SlimeChunkSection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public class CraftSlimeChunk implements SlimeChunk {

    private final String worldName;
    private final int x;
    private final int z;

    @Setter
    private final SlimeChunkSection[] sections;
    private final CompoundTag heightMaps;
    private final int[] biomes;
    private final List<CompoundTag> tileEntities;
    private final List<CompoundTag> entities;

    // Optional data for 1.13 world upgrading
    private CompoundTag upgradeData;
}
