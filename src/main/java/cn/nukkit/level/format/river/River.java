package cn.nukkit.level.format.river;

import cn.nukkit.level.GameRules;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.anvil.Anvil;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.AsyncTask;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Map;

public class River extends BaseLevelProvider {

    public River(final RiverLevel level, final String path, final boolean f) {
        super(level, path, f);
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
        final File slimeFile = new File(worldDir, name + ".slime");
        slimeFile.createNewFile();
        final CompoundTag data = new CompoundTag("Data")
            .putString("LevelName", name)
            .putInt("SpawnX", 0)
            .putInt("SpawnY", 64)
            .putInt("SpawnZ", 0)
            .putLong("Time", 0)
            .putLong("SizeOnDisk", 0);
        NBTIO.writeZSTDCompressed(data, new FileOutputStream(slimeFile), ByteOrder.BIG_ENDIAN);
    }

    @Override
    public BaseFullChunk loadChunk(final long index, final int chunkX, final int chunkZ, final boolean create) {
        return null;
    }

    @Override
    public AsyncTask requestChunkTask(final int X, final int Z) {
        return null;
    }

    @Override
    public BaseFullChunk getEmptyChunk(final int x, final int z) {
        return null;
    }

    @Override
    public void saveChunk(final int X, final int Z) {

    }

    @Override
    public void saveChunk(final int X, final int Z, final FullChunk chunk) {

    }

    @Override
    public GameRules getGamerules() {
        return GameRules.getDefault();
    }

}
