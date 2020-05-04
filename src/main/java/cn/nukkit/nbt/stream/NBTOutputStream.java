package cn.nukkit.nbt.stream;

import cn.nukkit.nbt.tag.*;
import cn.nukkit.utils.VarInt;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.List;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class NBTOutputStream implements DataOutput, AutoCloseable {
    private final DataOutputStream stream;
    private final ByteOrder endianness;
    private final boolean network;

    public NBTOutputStream(OutputStream stream) {
        this(stream, ByteOrder.BIG_ENDIAN);
    }

    public NBTOutputStream(OutputStream stream, ByteOrder endianness) {
        this(stream, endianness, false);
    }

    public NBTOutputStream(OutputStream stream, ByteOrder endianness, boolean network) {
        this.stream = stream instanceof DataOutputStream ? (DataOutputStream) stream : new DataOutputStream(stream);
        this.endianness = endianness;
        this.network = network;
    }

    public ByteOrder getEndianness() {
        return endianness;
    }

    public boolean isNetwork() {
        return network;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.stream.write(bytes);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        this.stream.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        this.stream.write(b);
    }

    @Override
    public void writeBoolean(boolean v) throws IOException {
        this.stream.writeBoolean(v);
    }

    @Override
    public void writeByte(int v) throws IOException {
        this.stream.writeByte(v);
    }

    @Override
    public void writeShort(int v) throws IOException {
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            v = Integer.reverseBytes(v) >> 16;
        }
        this.stream.writeShort(v);
    }

    @Override
    public void writeChar(int v) throws IOException {
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            v = Character.reverseBytes((char) v);
        }
        this.stream.writeChar(v);
    }

    @Override
    public void writeInt(int v) throws IOException {
        if (network) {
            VarInt.writeVarInt(this.stream, v);
        } else {
            if (endianness == ByteOrder.LITTLE_ENDIAN) {
                v = Integer.reverseBytes(v);
            }
            this.stream.writeInt(v);
        }
    }

    @Override
    public void writeLong(long v) throws IOException {
        if (network) {
            VarInt.writeVarLong(this.stream, v);
        } else {
            if (endianness == ByteOrder.LITTLE_ENDIAN) {
                v = Long.reverseBytes(v);
            }
            this.stream.writeLong(v);
        }
    }

    @Override
    public void writeFloat(float v) throws IOException {
        int i = Float.floatToIntBits(v);
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            i = Integer.reverseBytes(i);
        }
        this.stream.writeInt(i);
    }

    @Override
    public void writeDouble(double v) throws IOException {
        long l = Double.doubleToLongBits(v);
        if (endianness == ByteOrder.LITTLE_ENDIAN) {
            l = Long.reverseBytes(l);
        }
        this.stream.writeLong(l);
    }

    @Override
    public void writeBytes(String s) throws IOException {
        this.stream.writeBytes(s);
    }

    @Override
    public void writeChars(String s) throws IOException {
        this.stream.writeChars(s);
    }

    @Override
    public void writeUTF(String s) throws IOException {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (network) {
            VarInt.writeUnsignedVarInt(stream, bytes.length);
        } else {
            this.writeShort(bytes.length);
        }
        this.stream.write(bytes);
    }

    @Override
    public void close() throws IOException {
        this.stream.close();
    }

    public void writeTag(Tag tag) throws IOException {
        String name = tag.getName();
        this.stream.writeByte(tag.getId());
        this.stream.writeUTF(name);
        if (tag.getId() == Tag.TAG_End) {
            throw new IOException("Named TAG_End not permitted.");
        } else {
            this.writeTagPayload(tag);
        }
    }

    private void writeTagPayload(Tag tag) throws IOException {
        switch (tag.getId()) {
            case 0:
                this.writeEndTagPayload((EndTag) tag);
                break;
            case 1:
                this.writeByteTagPayload((ByteTag) tag);
                break;
            case 2:
                this.writeShortTagPayload((ShortTag) tag);
                break;
            case 3:
                this.writeIntTagPayload((IntTag) tag);
                break;
            case 4:
                this.writeLongTagPayload((LongTag) tag);
                break;
            case 5:
                this.writeFloatTagPayload((FloatTag) tag);
                break;
            case 6:
                this.writeDoubleTagPayload((DoubleTag) tag);
                break;
            case 7:
                this.writeByteArrayTagPayload((ByteArrayTag) tag);
                break;
            case 8:
                this.writeStringTagPayload((StringTag) tag);
                break;
            case 9:
                this.writeListTagPayload((ListTag<? extends Tag>) tag);
                break;
            case 10:
                this.writeCompoundTagPayload((CompoundTag) tag);
                break;
            case 11:
                this.writeIntArrayTagPayload((IntArrayTag) tag);
                break;
            case 12:
                this.writeLongArrayTagPayload((LongArrayTag) tag);
                break;
            case 13:
                this.writeShortArrayTagPayload((ShortArrayTag) tag);
                break;
            default:
                throw new IOException("Invalid tag type: " + tag.getId() + ".");
        }

    }

    private void writeByteTagPayload(ByteTag tag) throws IOException {
        this.stream.writeByte(tag.getData());
    }

    private void writeByteArrayTagPayload(ByteArrayTag tag) throws IOException {
        byte[] bytes = tag.getData();
        this.stream.writeInt(bytes.length);
        this.stream.write(bytes);
    }

    private void writeCompoundTagPayload(CompoundTag tag) throws IOException {
        Iterator var2 = tag.parseValue().values().iterator();

        while (var2.hasNext()) {
            Tag childTag = (Tag) var2.next();
            this.writeTag(childTag);
        }

        this.stream.writeByte(Tag.TAG_End);
    }

    private void writeListTagPayload(ListTag<? extends Tag> tag) throws IOException {
        List<? extends Tag> tags = tag.getAll();
        int size = tags.size();
        this.stream.writeByte(tag.type);
        this.stream.writeInt(size);
        Iterator<? extends Tag> var4 = tags.iterator();

        while (var4.hasNext()) {
            Tag tag1 = var4.next();
            this.writeTagPayload(tag1);
        }

    }

    private void writeStringTagPayload(StringTag tag) throws IOException {
        this.stream.writeUTF(tag.parseValue());
    }

    private void writeDoubleTagPayload(DoubleTag tag) throws IOException {
        this.stream.writeDouble(tag.getData());
    }

    private void writeFloatTagPayload(FloatTag tag) throws IOException {
        this.stream.writeFloat(tag.getData());
    }

    private void writeLongTagPayload(LongTag tag) throws IOException {
        this.stream.writeLong(tag.getData());
    }

    private void writeIntTagPayload(IntTag tag) throws IOException {
        this.stream.writeInt(tag.getData());
    }

    private void writeShortTagPayload(ShortTag tag) throws IOException {
        this.stream.writeShort(tag.getData());
    }

    private void writeIntArrayTagPayload(IntArrayTag tag) throws IOException {
        int[] ints = tag.getData();
        this.stream.writeInt(ints.length);

        for (int i = 0; i < ints.length; ++i) {
            this.stream.writeInt(ints[i]);
        }

    }

    private void writeLongArrayTagPayload(LongArrayTag tag) throws IOException {
        long[] longs = tag.getData();
        this.stream.writeInt(longs.length);

        for (int i = 0; i < longs.length; ++i) {
            this.stream.writeLong(longs[i]);
        }

    }

    private void writeShortArrayTagPayload(ShortArrayTag tag) throws IOException {
        short[] shorts = tag.getData();
        this.stream.writeInt(shorts.length);

        for (int i = 0; i < shorts.length; ++i) {
            this.stream.writeShort(shorts[i]);
        }

    }

    private void writeEndTagPayload(EndTag tag) {
    }

}
