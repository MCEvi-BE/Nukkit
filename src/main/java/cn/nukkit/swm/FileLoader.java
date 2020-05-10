package cn.nukkit.swm;

import cn.nukkit.swm.api.exceptions.UnknownWorldException;
import cn.nukkit.swm.api.exceptions.WorldInUseException;
import cn.nukkit.swm.api.loaders.SlimeLoader;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");

    private final Map<String, RandomAccessFile> worldFiles = new HashMap<>();

    private final File worldDir;

    public FileLoader(final File worldDir) {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            System.out.println("A file named '" + worldDir.getName() + "' has been deleted, as this is the name used for the worlds directory.");
            worldDir.delete();
        }

        worldDir.mkdirs();
    }

    @Override
    public byte[] loadWorld(final String worldName, final boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        if (!this.worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        final RandomAccessFile file = this.worldFiles.computeIfAbsent(worldName, world -> {

            try {
                return new RandomAccessFile(new File(this.worldDir, worldName + ".slime"), "rw");
            } catch (final FileNotFoundException ex) {
                return null; // This is never going to happen as we've just checked if the world exists
            }

        });

        if (!readOnly) {
            final FileChannel channel = file.getChannel();

            try {
                if (channel.tryLock() == null) {
                    throw new WorldInUseException(worldName);
                }
            } catch (final OverlappingFileLockException ex) {
                throw new WorldInUseException(worldName);
            }
        }

        if (file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        final byte[] serializedWorld = new byte[(int) file.length()];
        file.seek(0); // Make sure we're at the start of the file
        file.readFully(serializedWorld);

        return serializedWorld;
    }

    @Override
    public boolean worldExists(final String worldName) {
        return new File(this.worldDir, worldName + ".slime").exists();
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        final String[] worlds = this.worldDir.list(FileLoader.WORLD_FILE_FILTER);

        if (worlds == null) {
            throw new NotDirectoryException(this.worldDir.getPath());
        }

        return Arrays.stream(worlds).map(c -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(final String worldName, final byte[] serializedWorld, final boolean lock) throws IOException {
        RandomAccessFile worldFile = this.worldFiles.get(worldName);
        final boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(this.worldDir, worldName + ".slime"), "rw");
        }

        worldFile.seek(0); // Make sure we're at the start of the file
        worldFile.setLength(0); // Delete old data
        worldFile.write(serializedWorld);

        if (lock) {
            final FileChannel channel = worldFile.getChannel();

            try {
                channel.tryLock();
            } catch (final OverlappingFileLockException ignored) {

            }
        }

        if (tempFile) {
            worldFile.close();
        }
    }

    @Override
    public void unlockWorld(final String worldName) throws UnknownWorldException, IOException {
        if (!this.worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        final RandomAccessFile file = this.worldFiles.remove(worldName);

        if (file != null) {
            file.close();
        }
    }

    @Override
    public boolean isWorldLocked(final String worldName) throws IOException {
        RandomAccessFile file = this.worldFiles.get(worldName);
        boolean closeOnFinish = false;

        if (file == null) {
            file = new RandomAccessFile(new File(this.worldDir, worldName + ".slime"), "rw");
            closeOnFinish = true;
        }

        final FileChannel channel = file.getChannel();

        try {
            final FileLock fileLock = channel.tryLock();

            if (fileLock != null) {
                fileLock.release();
                return true;
            }
        } catch (final OverlappingFileLockException ignored) {

        } finally {
            if (closeOnFinish) {
                file.close();
            }
        }

        return false;
    }

    @Override
    public void deleteWorld(final String worldName) throws UnknownWorldException {
        if (!this.worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        new File(this.worldDir, worldName + ".slime").delete();
    }

}