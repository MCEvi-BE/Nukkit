package cn.nukkit.level.format.river;

/**
 * Exception thrown when a
 * world could not be found.
 */
public class UnknownWorldException extends Exception {

    public UnknownWorldException(final String world) {
        super("Unknown world " + world);
    }

}
