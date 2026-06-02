package eu.kryocloud.node.forwarding;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.api.service.ServiceType;

import java.util.Collection;
import java.util.Locale;

public enum ForwardingMode {

    NONE,
    VELOCITY,
    BUNGEECORD;

    public static ForwardingMode parse(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');

        return switch (normalized) {
            case "VELOCITY", "MODERN" -> VELOCITY;
            case "BUNGEE", "BUNGEECORD", "LEGACY", "WATERFALL", "FLAMECORD" -> BUNGEECORD;
            default -> NONE;
        };
    }

    public static ForwardingMode resolveForProxySoftware(String software) {
        String normalized = software == null ? "" : software.trim().toLowerCase(Locale.ROOT);

        if (normalized.contains("velocity")) {
            return VELOCITY;
        }

        if (normalized.contains("bungee") || normalized.contains("waterfall") || normalized.contains("flamecord")) {
            return BUNGEECORD;
        }

        return BUNGEECORD;
    }

    public static ForwardingMode resolveFromProxyGroups(Collection<IGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return NONE;
        }

        boolean velocity = false;
        boolean bungee = false;

        for (IGroup group : groups) {
            if (group.serviceType() != ServiceType.PROXY) {
                continue;
            }

            ForwardingMode mode = resolveForProxySoftware(group.software());

            if (mode == VELOCITY) {
                velocity = true;
                continue;
            }

            if (mode == BUNGEECORD) {
                bungee = true;
            }
        }

        if (velocity) {
            return VELOCITY;
        }

        if (bungee) {
            return BUNGEECORD;
        }

        return NONE;
    }

}
