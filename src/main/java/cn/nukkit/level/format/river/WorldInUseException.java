package cn.nukkit.level.format.river;

/**
 * Exception thrown when a world is locked
 * and is being accessed on write-mode.
 */
public class WorldInUseException extends Exception {

    public WorldInUseException(final String world) {
        super(world);
    }

}
