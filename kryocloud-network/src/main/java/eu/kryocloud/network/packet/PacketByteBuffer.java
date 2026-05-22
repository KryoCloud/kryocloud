package eu.kryocloud.network.packet;

import eu.kryocloud.network.KryoProtocol;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class PacketByteBuffer {

    private final ByteBuf handle;

    public PacketByteBuffer(ByteBuf handle) {
        if (handle == null) {
            throw new IllegalArgumentException("handle must not be null");
        }

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
        requireReadable(Integer.BYTES);
        return handle.readInt();
    }

    public void writeLong(long value) {
        handle.writeLong(value);
    }

    public long readLong() {
        requireReadable(Long.BYTES);
        return handle.readLong();
    }

    public void writeBoolean(boolean value) {
        handle.writeBoolean(value);
    }

    public boolean readBoolean() {
        requireReadable(Byte.BYTES);
        return handle.readBoolean();
    }

    public void writeByte(int value) {
        handle.writeByte(value);
    }

    public byte readByte() {
        requireReadable(Byte.BYTES);
        return handle.readByte();
    }

    public void writeBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes must not be null");
        }

        handle.writeBytes(bytes);
    }

    public byte[] readBytes(int length) {
        if (length < 0) {
            throw new IllegalStateException("length must not be negative");
        }

        requireReadable(length);

        byte[] bytes = new byte[length];
        handle.readBytes(bytes);
        return bytes;
    }

    public void writeString(String value) {
        if (value == null) {
            writeInt(-1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);

        if (bytes.length > KryoProtocol.MAX_STRING_BYTES) {
            throw new IllegalArgumentException("String exceeds maximum byte length: " + bytes.length);
        }

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

        if (length > KryoProtocol.MAX_STRING_BYTES) {
            throw new IllegalStateException("String exceeds maximum byte length: " + length);
        }

        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public void writeUuid(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        writeLong(value.getMostSignificantBits());
        writeLong(value.getLeastSignificantBits());
    }

    public UUID readUuid() {
        return new UUID(readLong(), readLong());
    }

    public void writeEnum(Enum<?> value) {
        if (value == null) {
            throw new IllegalArgumentException("value must not be null");
        }

        writeString(value.name());
    }

    public <T extends Enum<T>> T readEnum(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }

        String name = readString();

        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Enum value for " + type.getName() + " is missing");
        }

        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Unknown enum value '" + name + "' for " + type.getName(), exception);
        }
    }

    private void requireReadable(int bytes) {
        if (handle.readableBytes() < bytes) {
            throw new IllegalStateException(
                    "Buffer underflow: required=" + bytes + ", readable=" + handle.readableBytes()
            );
        }
    }
}