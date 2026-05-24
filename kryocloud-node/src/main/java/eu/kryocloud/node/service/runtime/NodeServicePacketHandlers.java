package eu.kryocloud.node.service.runtime;

import eu.kryocloud.common.logging.TextColor;
import eu.kryocloud.network.packet.bus.KryoPacketBus;
import eu.kryocloud.network.packet.bus.PacketContext;
import eu.kryocloud.network.packet.bus.PacketSubscription;
import eu.kryocloud.network.packet.type.service.ServiceCleanupResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceCommandResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceLogsResponsePacket;
import eu.kryocloud.network.packet.type.service.ServiceMetricsPacket;
import eu.kryocloud.network.packet.type.service.ServiceStatePacket;
import eu.kryocloud.network.protocol.PeerType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class NodeServicePacketHandlers implements AutoCloseable {

    private final NodeServiceRegistry serviceRegistry;
    private final AtomicBoolean registered = new AtomicBoolean(false);
    private final List<PacketSubscription> subscriptions = new ArrayList<>();

    public NodeServicePacketHandlers(NodeServiceRegistry serviceRegistry) {
        if (serviceRegistry == null) {
            throw new IllegalArgumentException("serviceRegistry must not be null");
        }

        this.serviceRegistry = serviceRegistry;
    }

    public void register() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }

        subscriptions.add(KryoPacketBus.listen(ServiceStatePacket.class, this::handleServiceState));
        subscriptions.add(KryoPacketBus.listen(ServiceCommandResponsePacket.class, this::handleCommandResponse));
        subscriptions.add(KryoPacketBus.listen(ServiceLogsResponsePacket.class, this::handleLogsResponse));
        subscriptions.add(KryoPacketBus.listen(ServiceMetricsPacket.class, this::handleMetrics));
        subscriptions.add(KryoPacketBus.listen(ServiceCleanupResponsePacket.class, this::handleCleanupResponse));
    }

    @Override
    public void close() {
        if (!registered.compareAndSet(true, false)) {
            return;
        }

        for (PacketSubscription subscription : subscriptions) {
            subscription.close();
        }

        subscriptions.clear();
    }

    private void handleServiceState(PacketContext context, ServiceStatePacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        NodeServiceSnapshot snapshot = serviceRegistry.update(packet);
        context.printState(TextColor.hex("#5EEAD4").apply("Service " + snapshot.serviceId()) + " is now " + stateColor(snapshot.state().name()) + " on " + snapshot.wrapperId());
    }

    private void handleCommandResponse(PacketContext context, ServiceCommandResponsePacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        if (packet.success()) {
            context.printState(TextColor.hex("#3DDC97").apply("Command accepted by " + packet.serviceId() + ": ") + packet.message());
            return;
        }

        context.printState(TextColor.hex("#FF5C5C").apply("Command failed for " + packet.serviceId() + ": ") + packet.message());
    }

    private void handleLogsResponse(PacketContext context, ServiceLogsResponsePacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        if (!packet.success()) {
            context.printState(TextColor.hex("#FF5C5C").apply("Logs unavailable for " + packet.serviceId() + ": ") + packet.message());
            return;
        }

        context.printState("");
        context.printState(TextColor.BOLD.code() + TextColor.hex("#F8F9FA").code() + "Logs for " + packet.serviceId() + TextColor.RESET.code());
        context.printState(TextColor.hex("#8B949E").apply(packet.message()));
        context.printState(packet.logs().isBlank() ? TextColor.hex("#8B949E").apply("No log lines available.") : packet.logs());
    }



    private void handleMetrics(PacketContext context, ServiceMetricsPacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        serviceRegistry.updateMetrics(packet);
    }

    private void handleCleanupResponse(PacketContext context, ServiceCleanupResponsePacket packet) {
        if (!validWrapper(context)) {
            context.connection().close();
            return;
        }

        String mode = packet.dryRun() ? "cleanup preview" : "cleanup";
        String title = TextColor.hex("#3DDC97").apply("Wrapper " + packet.wrapperId() + " " + mode + " finished");
        context.printState(title + TextColor.hex("#B6F09C").apply(" scanned=" + packet.scanned()) + TextColor.hex("#7DD3FC").apply(" deleted=" + packet.deleted()) + TextColor.hex("#FDE68A").apply(" skipped=" + packet.skipped()) + TextColor.hex("#FDA4AF").apply(" failed=" + packet.failed()));

        if (packet.details().isBlank()) {
            return;
        }

        context.printState(TextColor.hex("#D8B4FE").apply(packet.details()));
    }

    private String stateColor(String state) {
        if ("RUNNING".equals(state)) {
            return TextColor.hex("#3DDC97").apply(state);
        }

        if ("FAILED".equals(state)) {
            return TextColor.hex("#FF5C5C").apply(state);
        }

        if ("STOPPED".equals(state)) {
            return TextColor.hex("#8B949E").apply(state);
        }

        if ("STOPPING".equals(state)) {
            return TextColor.hex("#FFD166").apply(state);
        }

        return TextColor.hex("#D0D7DE").apply(state);
    }

    private boolean validWrapper(PacketContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }

        if (!context.authenticated()) {
            return false;
        }

        return context.connection().peerType() == PeerType.WRAPPER;
    }
}
