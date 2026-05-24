package eu.kryocloud.launcher;

import eu.kryocloud.node.KryoNode;
import eu.kryocloud.wrapper.KryoWrapper;

public final class KryoLauncher {

    private KryoLauncher() {
    }

    public static void main(String[] args) {
        System.out.println("Starting KryoCloud launcher...");
        KryoNode node = new KryoNode();
        KryoWrapper wrapper = new KryoWrapper();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            wrapper.shutdown();
            node.shutdown();
        }, "kryocloud-launcher-shutdown"));
    }
}
