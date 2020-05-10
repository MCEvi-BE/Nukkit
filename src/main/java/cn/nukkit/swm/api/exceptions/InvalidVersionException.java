package cn.nukkit.swm.api.exceptions;

/**
 * Exception thrown when SWM is loaded
 * on a non-supported Spigot version.
 */
public class InvalidVersionException extends SlimeException {

    public InvalidVersionException(final String version) {
        super("SlimeWorldManager does not support Spigot " + version + "!");
    }

}