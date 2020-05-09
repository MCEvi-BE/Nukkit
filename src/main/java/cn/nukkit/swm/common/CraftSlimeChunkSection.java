package cn.nukkit.swm.common;

import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.swm.api.utils.NibbleArray;
import cn.nukkit.swm.api.world.SlimeChunkSection;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CraftSlimeChunkSection implements SlimeChunkSection {

    // Pre 1.13 block data
    private final byte[] blocks;

    private final NibbleArray data;

    // Post 1.13 block data
    private final ListTag<CompoundTag> palette;

    private final long[] blockStates;

    private final NibbleArray blockLight;

    private final NibbleArray skyLight;

}
