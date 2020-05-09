package cn.nukkit.swm.api.utils;

import lombok.Getter;

/**
 * Credits to Minikloon for this class.
 * <p>
 * Source: https://github.com/Minikloon/CraftyWorld/blob/master/crafty-common/src/main/kotlin/world/crafty/common/utils/NibbleArray.kt
 */
public class NibbleArray {

    private final int size;

    @Getter
    private final byte[] backing;

    public NibbleArray(final int size) {
        this(new byte[size / 2]);
    }

    public NibbleArray(final byte[] backing) {
        this.backing = backing;
        this.size = backing.length * 2;
    }

    public int get(final int index) {
        final int value = this.backing[index / 2];

        return index % 2 == 0 ? value & 0xF : (value & 0xF0) >> 4;
    }

    public void set(final int index, final int value) {
        final int nibble = value & 0xF;
        final int halfIndex = index / 2;
        final int previous = this.backing[halfIndex];

        if (index % 2 == 0) {
            this.backing[halfIndex] = (byte) (previous & 0xF0 | nibble);
        } else {
            this.backing[halfIndex] = (byte) (previous & 0xF | nibble << 4);
        }
    }

}
