package eu.kryocloud.network.packet;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public class PacketByteBuffer {

    private final ByteBuf handle;

    public PacketByteBuffer(ByteBuf handle) {
        this.handle = handle;
    }

    public ByteBuf unwrap() {
        return handle;
    }

    public int readableBytes() {
        return handle.readableBytes();
    }

    public void writeInt(int value) {
        handle.writeInt(value);
    }

    public int readInt() {
        return handle.readInt();
    }

    public void writeLong(long value) {
        handle.writeLong(value);
    }

    public long readLong() {
        return handle.readLong();
    }

    public void writeBoolean(boolean value) {
        handle.writeBoolean(value);
    }

    public boolean readBoolean() {
        return handle.readBoolean();
    }

    public void writeByte(int value) {
        handle.writeByte(value);
    }

    public byte readByte() {
        return handle.readByte();
    }

    public void writeBytes(byte[] bytes) {
        handle.writeBytes(bytes);
    }

    public byte[] readBytes(int length) {
        byte[] bytes = new byte[length];
        handle.readBytes(bytes);
        return bytes;
    }

    public void writeString(String value) {
        if(value == null) {
            writeInt(-1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeInt(bytes.length);
        writeBytes(bytes);
    }

    public String readString() {
        int length = readInt();

        if (length == -1) {
            return null;
        }

        if (length < 0) {
            throw new IllegalStateException("Negative string length: " + length);
        }

        if(length > 32767) {
            throw new IllegalStateException("String too large: " + length);
        }

        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

}
