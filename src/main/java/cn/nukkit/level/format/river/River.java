package cn.nukkit.level.format.river;

import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntitySpawnable;
import cn.nukkit.level.GameRules;
import cn.nukkit.level.Level;
import cn.nukkit.level.format.ChunkSection;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.LevelProvider;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.level.generator.Generator;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.scheduler.AsyncTask;
import cn.nukkit.utils.BinaryStream;
import cn.nukkit.utils.ChunkException;
import cn.nukkit.utils.ThreadCache;
import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class River extends BaseLevelProvider {

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
    public AsyncTask requestChunkTask(final int x, final int z) {
        final RiverChunk chunk = (RiverChunk) this.getChunk(x, z, false);
        if (chunk == null) {
            throw new ChunkException("Invalid Chunk Set");
        }

        final long timestamp = chunk.getChanges();

        byte[] blockEntities = new byte[0];

        final List<CompoundTag> tagList = new ArrayList<>();

        final Map<Long, BlockEntity> entities = chunk.getBlockEntities();
        for (final BlockEntity blockEntity : entities.values()) {
            if (blockEntity instanceof BlockEntitySpawnable) {
                tagList.add(((BlockEntitySpawnable) blockEntity).getSpawnCompound());
            }
        }

        try {
            if (!tagList.isEmpty()) {
                blockEntities = NBTIO.write(tagList, ByteOrder.BIG_ENDIAN, true);
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
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
        final ChunkSection[] sections = chunk.getSections();
        for (int i = sections.length - 1; i >= 0; i--) {
            if (!sections[i].isEmpty()) {
                count = i + 1;
                break;
            }
        }
        for (int i = 0; i < count; i++) {
            sections[i].writeTo(stream);
        }
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
    public BaseFullChunk getEmptyChunk(final int x, final int z) {
        return new RiverChunk(this, x, z);
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
        this.level.timings.syncChunkLoadDataTimer.startTiming();
        if (chunk == null) {
            tmp = new RiverChunk(this, chunkX, chunkZ);
        } else {
            tmp = chunk;
        }
        this.level.timings.syncChunkLoadDataTimer.stopTiming();
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
