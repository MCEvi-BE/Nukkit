package cn.nukkit.level.generator;

import cn.nukkit.block.BlockID;
import cn.nukkit.level.ChunkManager;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.math.NukkitRandom;
import cn.nukkit.math.Vector3;
import java.util.HashMap;
import java.util.Map;

public final class Void extends Generator {

    private final Map<String, Object> options;

    private ChunkManager level;

    private boolean init = false;

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
        final BaseFullChunk chunk = this.level.getChunk(chunkX, chunkZ);
        if (!this.init) {
            this.init = true;
            chunk.setBlock(0, 60, 0, BlockID.BEDROCK);
        }
        chunk.setGenerated(true);
        for (int Z = 0; Z < 16; ++Z) {
            for (int X = 0; X < 16; ++X) {
                for (int y = 0; y < 256; ++y) {
                    chunk.setBlock(X, y, Z, BlockID.AIR);
                }
            }
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

}
