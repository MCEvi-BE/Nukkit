package cn.nukkit.level.format.river;

import cn.nukkit.level.Level;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.format.generic.BaseFullChunk;
import cn.nukkit.level.format.generic.BaseLevelProvider;
import cn.nukkit.scheduler.AsyncTask;

import java.io.IOException;

public class River extends BaseLevelProvider {

    public River(Level level, String path, boolean f) throws IOException {
        super(level, path, f);
    }

    @Override
    public BaseFullChunk loadChunk(long index, int chunkX, int chunkZ, boolean create) {
        return null;
    }

    @Override
    public AsyncTask requestChunkTask(int X, int Z) {
        return null;
    }

    @Override
    public BaseFullChunk getEmptyChunk(int x, int z) {
        return null;
    }

    @Override
    public void saveChunk(int X, int Z) {

    }

    @Override
    public void saveChunk(int X, int Z, FullChunk chunk) {

    }
}
