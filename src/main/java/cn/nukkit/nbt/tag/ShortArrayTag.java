package cn.nukkit.nbt.tag;

import cn.nukkit.nbt.stream.NBTInputStream;
import cn.nukkit.nbt.stream.NBTOutputStream;

import java.io.IOException;
import java.util.Arrays;

public class ShortArrayTag extends Tag {
    public short[] data;

    public ShortArrayTag(String name) {
        super(name);
    }

    public ShortArrayTag(String name, short[] data) {
        super(name);
        this.data = data;
    }

    @Override
    void write(NBTOutputStream dos) throws IOException {
        dos.writeInt(data.length);
        for (int aData : data) {
            dos.writeShort(aData);
        }
    }

    @Override
    void load(NBTInputStream dis) throws IOException {
        int length = dis.readInt();
        data = new short[length];
        for (int i = 0; i < length; i++) {
            data[i] = dis.readShort();
        }
    }

    public short[] getData() {
        return data;
    }

    @Override
    public short[] parseValue() {
        return this.data;
    }

    @Override
    public byte getId() {
        return TAG_Short_Array;
    }

    @Override
    public String toString() {
        return "ShortArrayTag " + this.getName() + " [" + data.length + " bytes]";
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            ShortArrayTag shortArrayTag = (ShortArrayTag) obj;
            return ((data == null && shortArrayTag.data == null) || (data != null && Arrays.equals(data, shortArrayTag.data)));
        }
        return false;
    }

    @Override
    public Tag copy() {
        short[] cp = new short[data.length];
        System.arraycopy(data, 0, cp, 0, data.length);
        return new ShortArrayTag(getName(), cp);
    }
}
