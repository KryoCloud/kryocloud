package eu.kryocloud.api.node;

import eu.kryocloud.api.util.WriteOnceReference;

final class NodeAccess {
    private static final WriteOnceReference<INode> NODE = new WriteOnceReference<>();

    static void initialize(INode node) {
        NODE.setOrThrow(node);
    }

    static INode get() {
        return NODE.get();
    }

    private NodeAccess() {
    }
}
