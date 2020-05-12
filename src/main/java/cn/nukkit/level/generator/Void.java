package cn.nukkit.level.generator;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Void extends Generator {

    private static final List<Level> loadedLevels = new ArrayList<>();

    private final Map<String, Object> options;

    private ChunkManager level;

    private Level lvl;

    public Void(final Map<String, Object> options) {
        this.options = options;
    }

    @Override
    public int getId() {
        return Generator.TYPE_VOID;
    }

    @Override
    public void init(final ChunkManager level, final NukkitRandom random) {
        this.level = level;
    }

    @Override
    public void generateChunk(final int chunkX, final int chunkZ) {
        if (chunkX < 0 || chunkZ < 0) {
            return;
        }
        final BaseFullChunk chunk = this.level.getChunk(chunkX, chunkZ);
        chunk.setGenerated(true);
        for (int Z = 0; Z < 16; ++Z) {
            for (int X = 0; X < 16; ++X) {
                for (int y = 0; y < 256; ++y) {
                    chunk.setBlock(X, y, Z, BlockID.AIR);
                }
            }
        }
        if (this.lvl != null && !Void.loadedLevels.contains(this.lvl)) {
            Void.loadedLevels.add(this.lvl);
            chunk.setBlock(0, 60, 0, BlockID.BEDROCK);
        }
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

    public void init(final ChunkManager level, final NukkitRandom random, final Level lvl) {
        this.init(level, random);
        this.lvl = lvl;
    }

}
