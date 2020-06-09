package cn.nukkit;

import cn.nukkit.nbt.tag.CompoundTag;

import java.util.UUID;

public interface PlayerCompoundProvider {

    CompoundTag getPlayerCompound(UUID uuid, String name, String xuid);

    void savePlayerCompound(UUID uuid, String name, String xuid, CompoundTag tag);
}
