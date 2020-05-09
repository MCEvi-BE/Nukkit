package cn.nukkit.swm.api.world.properties;

import java.util.function.Function;
import lombok.Getter;

/**
 * A property object.
 */
@Getter
public class SlimeProperty {

    private final String nbtName;

    private final PropertyType type;

    private final Object defaultValue;

    private final Function<Object, Boolean> validator;

    SlimeProperty(final String nbtName, final PropertyType type, final Object defaultValue) {
        this(nbtName, type, defaultValue, null);
    }

    SlimeProperty(final String nbtName, final PropertyType type, final Object defaultValue, final Function<Object, Boolean> validator) {
        this.nbtName = nbtName;
        this.type = type;

        if (defaultValue != null) {
            if (!type.getValueClazz().isInstance(defaultValue)) {
                throw new IllegalArgumentException(defaultValue + " does not match class " + type.getValueClazz().getName());
            }

            if (validator != null) {
                if (!validator.apply(defaultValue)) {
                    throw new IllegalArgumentException("Invalid default value for property " + nbtName + "! " + defaultValue);
                }
            }
        }

        this.defaultValue = defaultValue;
        this.validator = validator;
    }

}
