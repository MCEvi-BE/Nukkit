package cn.nukkit;

import cn.nukkit.nbt.tag.CompoundTag;

import java.util.UUID;

public interface CustomPlayerData {

    CompoundTag onDataGet(UUID uuid, String name, String xuid);

}
