package cn.nukkit.swm.nukkit;

import cn.nukkit.Server;
import cn.nukkit.level.GameRule;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.swm.api.world.properties.SlimeProperties;
import cn.nukkit.swm.api.world.properties.SlimePropertyMap;
import cn.nukkit.swm.common.CraftSlimeWorld;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class CustomLevel extends Level {

    private static final Logger LOGGER = LogManager.getLogger("SWM World");
    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
        .setNameFormat("SWM Pool Thread #%1$d").build());

    @Getter
    private final CraftSlimeWorld slimeWorld;

    private final Object saveLock = new Object();
    private final List<WorldMap> maps = new ArrayList<>();

    @Getter
    @Setter
    private boolean ready = false;

    public CustomLevel(CraftSlimeWorld world, Server server, String name, String path, Class<? extends LevelProvider> provider) {
        super(server, name, path, provider);
        this.slimeWorld = world;
        SlimePropertyMap propertyMap = world.getPropertyMap();
        server.setPropertyInt("difficulty", Server.getDifficultyFromString(propertyMap.getString(SlimeProperties.DIFFICULTY).toUpperCase()));
        setSpawnLocation(new Vector3(propertyMap.getInt(SlimeProperties.SPAWN_X), propertyMap.getInt(SlimeProperties.SPAWN_Y), propertyMap.getInt(SlimeProperties.SPAWN_Z)));
        gameRules.setGameRule(GameRule.DO_MOB_SPAWNING, propertyMap.getBoolean(SlimeProperties.ALLOW_MONSTERS));
        gameRules.setGameRule(GameRule.PVP, propertyMap.getBoolean(SlimeProperties.PVP));
        final File base = new File(path).getParentFile();
        final File newbase = new File(base, path);
        new File(newbase, "level.dat").delete();
        new File(newbase, "region").delete();
        newbase.delete();
        newbase.getParentFile().delete();
        for (CompoundTag mapTag : world.getWorldMaps()) {
            int id = mapTag.getInt("id");
            WorldMap map = new WorldMap("map_" + id);
            map.a(mapTag);
            a(map);
        }
    }

}
