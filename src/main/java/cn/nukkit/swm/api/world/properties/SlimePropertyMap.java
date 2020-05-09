package cn.nukkit.swm.api.world.properties;

import cn.nukkit.nbt.tag.ByteTag;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.IntTag;
import cn.nukkit.nbt.tag.StringTag;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * A Property Map object.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SlimePropertyMap {

    @Getter(AccessLevel.PRIVATE)
    private final Map<SlimeProperty, Object> values;

    public SlimePropertyMap() {
        this(new HashMap<>());
    }

    /**
     * Creates a {@link SlimePropertyMap} based off a {@link CompoundTag}.
     *
     * @param compound A {@link CompoundTag} to get the properties from.
     * @return A {@link SlimePropertyMap} with the properties from the provided {@link CompoundTag}.
     */
    public static SlimePropertyMap fromCompound(final CompoundTag compound) {
        final Map<SlimeProperty, Object> values = new HashMap<>();

        for (final SlimeProperty property : SlimeProperties.VALUES) {
            switch (property.getType()) {
                case STRING:
                    compound.getStringOptional(property.getNbtName()).ifPresent(value -> values.put(property, value));
                    break;
                case BOOLEAN:
                    compound.getByteOptional(property.getNbtName()).map(value -> value == 1).ifPresent(value -> values.put(property, value));
                    break;
                case INT:
                    compound.getIntOptional(property.getNbtName()).ifPresent(value -> values.put(property, value));
                    break;
            }
        }

        return new SlimePropertyMap(values);
    }

    /**
     * Returns the string value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link String} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#STRING}.
     */
    public String getString(final SlimeProperty property) {
        this.ensureType(property, PropertyType.STRING);
        String value = (String) this.values.get(property);

        if (value == null) {
            value = (String) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given string value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#STRING}.
     */
    public void setString(final SlimeProperty property, final String value) {
        Objects.requireNonNull(value, "Property value cannot be null");
        this.ensureType(property, PropertyType.STRING);

        if (property.getValidator() != null && !property.getValidator().apply(value)) {
            throw new IllegalArgumentException("'" + value + "' is not a valid property value.");
        }

        this.values.put(property, value);
    }

    /**
     * Returns the boolean value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the {@link Boolean} value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#BOOLEAN}.
     */
    public Boolean getBoolean(final SlimeProperty property) {
        this.ensureType(property, PropertyType.BOOLEAN);
        Boolean value = (Boolean) this.values.get(property);

        if (value == null) {
            value = (Boolean) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given boolean value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#BOOLEAN}.
     */
    public void setBoolean(final SlimeProperty property, final boolean value) {
        this.ensureType(property, PropertyType.BOOLEAN);
        // There's no need to validate the value, why'd you ever have a validator for a boolean?
        this.values.put(property, value);
    }

    /**
     * Returns the int value of a given property.
     *
     * @param property The property to retrieve the value of.
     * @return the int value of the property or the default value if unset.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#INT}.
     */
    public int getInt(final SlimeProperty property) {
        this.ensureType(property, PropertyType.INT);
        Integer value = (Integer) this.values.get(property);

        if (value == null) {
            value = (Integer) property.getDefaultValue();
        }

        return value;
    }

    /**
     * Updates a property with a given int value.
     *
     * @param property The property to update.
     * @param value The value to set.
     * @throws IllegalArgumentException if the property type is not {@link PropertyType#INT}.
     */
    public void setInt(final SlimeProperty property, final int value) {
        this.ensureType(property, PropertyType.INT);

        if (property.getValidator() != null && !property.getValidator().apply(value)) {
            throw new IllegalArgumentException("'" + value + "' is not a valid property value.");
        }

        this.values.put(property, value);
    }

    /**
     * Copies all values from the specified {@link SlimePropertyMap}.
     * If the same property has different values on both maps, the one
     * on the providen map will be used.
     *
     * @param propertyMap A {@link SlimePropertyMap}.
     */
    public void merge(final SlimePropertyMap propertyMap) {
        this.values.putAll(propertyMap.getValues());
    }

    /**
     * Returns a {@link CompoundTag} containing every property set in this map.
     *
     * @return A {@link CompoundTag} with all the properties stored in this map.
     */
    public CompoundTag toCompound() {
        final CompoundTag properties = new CompoundTag("properties");
        for (final Map.Entry<SlimeProperty, Object> entry : this.values.entrySet()) {
            final SlimeProperty property = entry.getKey();
            final Object value = entry.getValue();

            switch (property.getType()) {
                case STRING:
                    properties.put(property.getNbtName(), new StringTag(property.getNbtName(), (String) value));
                    break;
                case BOOLEAN:
                    properties.put(property.getNbtName(), new ByteTag(property.getNbtName(), (byte) ((Boolean) value ? 1 : 0)));
                    break;
                case INT:
                    properties.put(property.getNbtName(), new IntTag(property.getNbtName(), (Integer) value));
                    break;
            }
        }

        return properties;
    }

    private void ensureType(final SlimeProperty property, final PropertyType requiredType) {
        if (property.getType() != requiredType) {
            throw new IllegalArgumentException("Property " + property.getNbtName() + " type is " + property.getType().name() + ", not " + requiredType.name());
        }
    }

}
