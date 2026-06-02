package eu.kryocloud.network.stream;

public interface KryoStreamListener {

    default void connected(KryoStreamConnection connection) {
    }

    default void disconnected(KryoStreamConnection connection) {
    }

    default void opened(KryoStreamConnection connection, KryoStream stream, byte[] payload) {
    }

    default void received(KryoStreamConnection connection, KryoStream stream, byte[] payload) {
    }

    default void ended(KryoStreamConnection connection, KryoStream stream, byte[] payload) {
    }

    default void reset(KryoStreamConnection connection, KryoStream stream, String reason) {
    }
}