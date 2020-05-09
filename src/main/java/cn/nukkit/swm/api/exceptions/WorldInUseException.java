package cn.nukkit.swm.api.exceptions;

/**
 * Exception thrown when a world is locked
 * and is being accessed on write-mode.
 */
public class WorldInUseException extends SlimeException {

    public WorldInUseException(final String world) {
        super(world);
    }

}
