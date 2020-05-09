package cn.nukkit.level.format.slime;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySpawnable;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.level.format.generic.BaseRegionLoader;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.ChunkException;
import cn.nukkit.utils.ThreadCache;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class Slime extends BaseLevelProvider {

    public static final int VERSION = 19133;

    private static final byte[] PAD_256 = new byte[256];

    private int lastPosition = 0;

    private SlimeWorld slimeWorld = null;
    private static FileLoader loader;

    private String name = "unknown";

    public Slime(final Level level, final String path) throws Exception {
        super(level, new File(path).getAbsoluteFile().getName(), true);
        loader = new FileLoader(new File("fastworld/"));

        final CompoundTag levelData = new CompoundTag("Data")
                .putCompound("GameRules", new CompoundTag())

                .putLong("DayTime", 0)
                .putInt("GameType", 0)
                .putString("generatorName", "DEFAULT")
                .putString("generatorOptions", "")
                .putInt("generatorVersion", 1)
                .putBoolean("hardcore", false)
                .putBoolean("initialized", true)
                .putLong("LastPlayed", System.currentTimeMillis() / 1000)
                .putString("LevelName", name)
                .putBoolean("raining", false)
                .putInt("rainTime", 0)
                .putLong("RandomSeed", 32456357466L)
                .putInt("SpawnX", 128)
                .putInt("SpawnY", 70)
                .putInt("SpawnZ", 128)
                .putBoolean("thundering", false)
                .putInt("thunderTime", 0)
                .putInt("version", Slime.VERSION)
                .putLong("Time", 0)
                .putLong("SizeOnDisk", 0);

        this.levelData = levelData;


        this.name = new File(path).getAbsoluteFile().getName();
        slimeWorld = SlimeWorld.deserializeWorld(new File(path).getAbsoluteFile().getName(), loader.loadWorld(new File(path).getAbsoluteFile().getName(), false));
    }

    public static String getProviderName() {
        return "slime";
    }

    public static byte getProviderOrder() {
        return LevelProvider.ORDER_YZX;
    }

    @Override
    public void updateLevelName(String name) {
        super.updateLevelName(name);
    }


    @Override
    public String getGenerator() {
        return "DEFAULT";
    }

    @Override
    public String getName() {
        return this.name;
    }


    public static boolean usesChunkSection() {
        return true;
    }

    public static boolean isValid(final String path) {
        if (loader == null) {
            new FileLoader(new File("fastworld/"));
        }

        System.out.println("isValid()");
        System.out.println(path);
        System.out.println(new File(path).getAbsoluteFile().getName());
        return loader.worldExists(new File(path).getAbsoluteFile().getName());
    }

    public static void generate(final String path, final String name, final long seed, final Class<? extends Generator> generator) throws IOException {
        Slime.generate(path, name, seed, generator, new HashMap<>());
    }

    public static void generate(final String path, final String name, final long seed, final Class<? extends Generator> generator, final Map<String, String> options) throws IOException {
        if (loader == null) {
            loader = new FileLoader(new File("fastworld/"));
        }

        final CompoundTag levelData = new CompoundTag("Data")
            .putCompound("GameRules", new CompoundTag())

            .putLong("DayTime", 0)
            .putInt("GameType", 0)
            .putString("generatorName", Generator.getGeneratorName(generator))
            .putString("generatorOptions", options.getOrDefault("preset", ""))
            .putInt("generatorVersion", 1)
            .putBoolean("hardcore", false)
            .putBoolean("initialized", true)
            .putLong("LastPlayed", System.currentTimeMillis() / 1000)
            .putString("LevelName", name)
            .putBoolean("raining", false)
            .putInt("rainTime", 0)
            .putLong("RandomSeed", seed)
            .putInt("SpawnX", 128)
            .putInt("SpawnY", 70)
            .putInt("SpawnZ", 128)
            .putBoolean("thundering", false)
            .putInt("thunderTime", 0)
            .putInt("version", Slime.VERSION)
            .putLong("Time", 0)
            .putLong("SizeOnDisk", 0);

        if (!isValid(path)) {
            loader.saveWorld(name, new byte[0], false);
        }

    }

    public static ChunkSection createChunkSection(final int y) {
        final ChunkSection cs = new ChunkSection(y);
        cs.hasSkyLight = true;
        return cs;
    }

    @Override
    public AsyncTask requestChunkTask(final int x, final int z) throws ChunkException {
        final Chunk chunk = (Chunk) this.getChunk(x, z, false);
        if (chunk == null) {
            throw new ChunkException("Invalid Chunk Set");
        }

        final long timestamp = chunk.getChanges();

        byte[] blockEntities = new byte[0];

        if (!chunk.getBlockEntities().isEmpty()) {
            final List<CompoundTag> tagList = new ArrayList<>();

            for (final BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                if (blockEntity instanceof BlockEntitySpawnable) {
                    tagList.add(((BlockEntitySpawnable) blockEntity).getSpawnCompound());
                }
            }

            try {
                blockEntities = NBTIO.write(tagList, ByteOrder.LITTLE_ENDIAN, true);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        final Map<Integer, Integer> extra = chunk.getBlockExtraDataArray();
        final BinaryStream extraData;
        if (!extra.isEmpty()) {
            extraData = new BinaryStream();
            extraData.putVarInt(extra.size());
            for (final Map.Entry<Integer, Integer> entry : extra.entrySet()) {
                extraData.putVarInt(entry.getKey());
                extraData.putLShort(entry.getValue());
            }
        } else {
            extraData = null;
        }

        final BinaryStream stream = ThreadCache.binaryStream.get().reset();
        int count = 0;
        final cn.nukkit.level.format.ChunkSection[] sections = chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            if (!sections[i].isEmpty()) {
                count = i + 1;
                break;
            }
        }
//        stream.putByte((byte) count);  count is now sent in packet
        for (int i = 0; i < count; i++) {
            sections[i].writeTo(stream);
        }
//        for (byte height : chunk.getHeightMapArray()) {
//            stream.putByte(height);
//        } computed client side?
        stream.put(chunk.getBiomeIdArray());
        stream.putByte((byte) 0);
        if (extraData != null) {
            stream.put(extraData.getBuffer());
        } else {
            stream.putVarInt(0);
        }
        stream.put(blockEntities);

        this.getLevel().chunkRequestCallback(timestamp, x, z, count, stream.getBuffer());

        return null;
    }

    @Override
    public Chunk getEmptyChunk(final int chunkX, final int chunkZ) {
        return Chunk.getEmptyChunk(chunkX, chunkZ, this);
    }


    @Override
    public void saveChunks() {
        super.saveChunks();

        try {
            loader.saveWorld(this.name, slimeWorld.serialize(), false);
        } catch (IOException e) {
            System.out.println(e + "    save error motherfucker");
        }
    }

    @Override
    public synchronized void saveChunk(final int X, final int Z) {
        final BaseFullChunk chunk = this.getChunk(X, Z);
        if (chunk != null) {
            try {
                slimeWorld.updateChunk((Chunk) chunk);
            } catch (final Exception e) {
                throw new ChunkException("Error saving chunk (" + X + ", " + Z + ")", e);
            }
        }
    }

    @Override
    public synchronized void saveChunk(final int x, final int z, final FullChunk chunk) {
        if (!(chunk instanceof Chunk)) {
            throw new ChunkException("Invalid Chunk class");
        }
        chunk.setX(x);
        chunk.setZ(z);
        try {
            slimeWorld.updateChunk((Chunk) chunk);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void doGarbageCollection(final long time) {
        final long start = System.currentTimeMillis();
        final int maxIterations = this.size();
        if (this.lastPosition > maxIterations) {
            this.lastPosition = 0;
        }
        int i;
        synchronized (this.chunks) {
            ObjectIterator<BaseFullChunk> iter = this.chunks.values().iterator();
            if (this.lastPosition != 0) {
                iter.skip(this.lastPosition);
            }
            for (i = 0; i < maxIterations; i++) {
                if (!iter.hasNext()) {
                    iter = this.chunks.values().iterator();
                }
                if (!iter.hasNext()) {
                    break;
                }
                final BaseFullChunk chunk = iter.next();
                if (chunk == null) {
                    continue;
                }
                if (chunk.isGenerated() && chunk.isPopulated() && chunk instanceof Chunk) {
                    final Chunk anvilChunk = (Chunk) chunk;
                    anvilChunk.compress();
                    if (System.currentTimeMillis() - start >= time) {
                        break;
                    }
                }
            }
        }
        this.lastPosition += i;
    }

    @Override
    public synchronized BaseFullChunk loadChunk(final long index, final int chunkX, final int chunkZ, final boolean create) {
        this.level.timings.syncChunkLoadDataTimer.startTiming();
        BaseFullChunk chunk;
        try {
           // chunk = region.readChunk(chunkX - regionX * 32, chunkZ - regionZ * 32);
            chunk = slimeWorld.getChunk(chunkX, chunkZ);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (chunk == null) {
            if (create) {
                chunk = this.getEmptyChunk(chunkX, chunkZ);
                this.putChunk(index, chunk);
            }
        } else {
            this.putChunk(index, chunk);
        }
        this.level.timings.syncChunkLoadDataTimer.stopTiming();
        return chunk;
    }


}
