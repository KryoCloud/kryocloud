package eu.kryocloud.plugins.proxybridge.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import eu.kryocloud.plugins.proxybridge.command.CloudCommandAudience;
import eu.kryocloud.plugins.proxybridge.command.CloudCommandBridge;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class VelocityCloudCommand implements SimpleCommand {

    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacySection();

    private final CloudCommandBridge bridge;

    public VelocityCloudCommand(CloudCommandBridge bridge) {
        this.bridge = bridge;
    }

    @Override
    public void execute(Invocation invocation) {
        bridge.execute(new VelocityAudience(invocation.source()), List.of(invocation.arguments()));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        return bridge.suggest(new VelocityAudience(invocation.source()), Arrays.asList(invocation.arguments()));
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return true;
    }

    private record VelocityAudience(CommandSource source) implements CloudCommandAudience {

        @Override
        public boolean hasPermission(String permission) {
            return source.hasPermission(permission);
        }

        @Override
        public void sendMessage(String message) {
            source.sendMessage(SERIALIZER.deserialize(message));
        }

    }

}
