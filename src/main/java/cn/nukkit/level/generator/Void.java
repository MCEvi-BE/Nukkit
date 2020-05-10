package cn.nukkit.level.generator;

import cn.nukkit.level.ChunkManager;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import java.util.HashMap;
import java.util.Map;

public final class Void extends Generator {

    private final Map<String, Object> options;

    private ChunkManager level;

    public Void(final Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public int getId() {
        return Generator.TYPE_VOID;
    }

    @Override
    public void init(final ChunkManager level, final NukkitRandom random) {

    }

    @Override
    public void generateChunk(final int chunkX, final int chunkZ) {
        // empty
    }

    @Override
    public void populateChunk(final int chunkX, final int chunkZ) {
        // empty
    }

    @Override
    public Map<String, Object> getSettings() {
        return new HashMap<>();
    }

    @Override
    public String getName() {
        return "void";
    }

    @Override
    public Vector3 getSpawn() {
        return new Vector3(0.0d, 64.0d, 0.0d);
    }

    @Override
    public ChunkManager getChunkManager() {
        return this.level;
    }

}
