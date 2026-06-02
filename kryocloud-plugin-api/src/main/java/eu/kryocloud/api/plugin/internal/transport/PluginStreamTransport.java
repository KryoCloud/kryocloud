package eu.kryocloud.api.plugin.internal.transport;

import eu.kryocloud.api.plugin.internal.protocol.PluginWireCodec;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessage;
import eu.kryocloud.api.plugin.internal.protocol.PluginWireMessageType;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrame;
import eu.kryocloud.api.plugin.internal.transport.frame.StreamFrameCodec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class PluginStreamTransport implements AutoCloseable {

    private static final String CHANNEL = "kryocloud:plugin";

    private final String host;
    private final int port;
    private final Duration timeout;
    private final Consumer<StreamFrame> frameConsumer;
    private final Consumer<Throwable> errorConsumer;
    private final AtomicBoolean connected = new AtomicBoolean();
    private final Object writeLock = new Object();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private Socket socket;
    private DataInputStream input;
    private DataOutputStream output;

    public PluginStreamTransport(String host, int port, Duration timeout, Consumer<StreamFrame> frameConsumer, Consumer<Throwable> errorConsumer) {
        this.host = host;
        this.port = port;
        this.timeout = timeout;
        this.frameConsumer = frameConsumer;
        this.errorConsumer = errorConsumer;
    }

    public CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                Socket activeSocket = new Socket();
                activeSocket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
                activeSocket.setTcpNoDelay(true);
                activeSocket.setKeepAlive(true);

                this.socket = activeSocket;
                this.input = new DataInputStream(new BufferedInputStream(activeSocket.getInputStream()));
                this.output = new DataOutputStream(new BufferedOutputStream(activeSocket.getOutputStream()));
                this.connected.set(true);
                this.executor.execute(this::readLoop);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not connect to KryoCloud at " + host + ":" + port, exception);
            }
        }, executor);
    }

    public boolean connected() {
        return connected.get() && socket != null && socket.isConnected() && !socket.isClosed();
    }

    public UUID send(PluginWireMessage message) {
        UUID streamId = UUID.randomUUID();
        write(StreamFrame.open(streamId, CHANNEL, PluginWireCodec.encode(message)));
        return streamId;
    }

    public void ack(UUID streamId) {
        write(StreamFrame.end(streamId, CHANNEL, PluginWireCodec.encode(new PluginWireMessage(UUID.randomUUID(), PluginWireMessageType.ACK, "ack", "", java.util.Map.of()))));
    }

    public void reset(UUID streamId, String reason) {
        write(StreamFrame.reset(streamId, CHANNEL, reason));
    }

    @Override
    public void close() {
        if (!connected.getAndSet(false)) {
            executor.shutdownNow();
            return;
        }

        try {
            socket.close();
        } catch (IOException ignored) {
        }

        executor.shutdownNow();
    }

    private void write(StreamFrame frame) {
        if (!connected()) {
            throw new IllegalStateException("KryoCloud transport is not connected");
        }

        synchronized (writeLock) {
            try {
                StreamFrameCodec.write(output, frame);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not write stream frame", exception);
            }
        }
    }

    private void readLoop() {
        while (connected()) {
            try {
                StreamFrame frame = StreamFrameCodec.read(input);
                frameConsumer.accept(frame);
            } catch (Throwable throwable) {
                if (connected.get()) {
                    errorConsumer.accept(throwable);
                }

                close();
                return;
            }
        }
    }

}
