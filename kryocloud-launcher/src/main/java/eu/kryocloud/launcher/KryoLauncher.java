package eu.kryocloud.launcher;

import eu.kryocloud.node.KryoNode;
import eu.kryocloud.wrapper.KryoWrapper;

public class KryoLauncher {

    private KryoLauncher() {

    }

    static void main() {
        IO.println("Starting KryoCloud launcher...");
        new KryoNode();
        new KryoWrapper();
    }
}