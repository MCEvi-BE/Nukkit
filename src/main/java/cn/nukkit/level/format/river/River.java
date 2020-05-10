package cn.nukkit.level.format.river;

import cn.nukkit.level.GameRules;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.ChunkException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class River extends BaseLevelProvider {

    public River(final Level level, final String path, final boolean f) {
        super(level, path, true);
    }

    public static String getProviderName() {
        return "river";
    }

    public static boolean isValid(final String path) {
        final File worldNameDir = new File(path);
        if (!worldNameDir.exists()) {
            return false;
        }
        final File[] files = worldNameDir.listFiles();
        if (files == null) {
            return false;
        }
        for (final File file : files) {
            if (file.getName().endsWith(".slime")) {
                return true;
            }
        }
        return false;
    }

    public static byte getProviderOrder() {
        return LevelProvider.ORDER_ZXY;
    }

    public static boolean usesChunkSection() {
        return true;
    }

    public static void generate(final String path, final String name, final long seed,
                                final Class<? extends Generator> generator, final Map<String, String> options)
        throws IOException {
        final File worldDir = new File(path);
        if (!worldDir.exists()) {
            worldDir.mkdirs();
        }
        final File slimeFile = new File(worldDir, name + ".slime");
        slimeFile.createNewFile();
    }

    @Override
    public AsyncTask requestChunkTask(final int X, final int Z) {
        return null;
    }

    @Override
    public BaseFullChunk getEmptyChunk(final int x, final int z) {
        return new RiverChunk(x, z);
    }

    @Override
    public void saveChunk(final int x, final int z) {
        this.saveChunk(x, z, this.getChunk(x, z));
    }

    @Override
    public void saveChunk(final int x, final int z, final FullChunk chunk) {
        if (chunk instanceof RiverChunk) {
            try {
                this.getLevel().setChunk(x, z, (RiverChunk) chunk);
            } catch (final Exception e) {
                throw new ChunkException("Error saving chunk (" + x + ", " + z + ")", e);
            }
        }
    }

    @Override
    public BaseFullChunk loadChunk(final long index, final int chunkX, final int chunkZ, final boolean create) {
        final RiverChunk chunk = this.getLevel().getChunk(chunkX, chunkZ);
        final RiverChunk tmp;
        if (chunk == null) {
            tmp = new RiverChunk(chunkX, chunkZ);
        } else {
            tmp = chunk;
        }
        return tmp;
    }

    @Override
    public String getGenerator() {
        return "void";
    }

    @Override
    public RiverLevel getLevel() {
        return (RiverLevel) super.getLevel();
    }

    @Override
    public GameRules getGamerules() {
        return GameRules.getDefault();
    }

}
