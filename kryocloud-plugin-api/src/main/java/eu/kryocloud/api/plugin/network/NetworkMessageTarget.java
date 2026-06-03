package eu.kryocloud.api.plugin.network;

import java.util.Map;

public record NetworkMessageTarget(String service, String group, String wrapper) {

    public static final NetworkMessageTarget ALL = new NetworkMessageTarget("", "", "");

    public NetworkMessageTarget {
        service = service == null ? "" : service.trim();
        group = group == null ? "" : group.trim();
        wrapper = wrapper == null ? "" : wrapper.trim();
    }

    public static NetworkMessageTarget all() {
        return ALL;
    }

    public static NetworkMessageTarget service(String service) {
        return new NetworkMessageTarget(service, "", "");
    }

    public static NetworkMessageTarget group(String group) {
        return new NetworkMessageTarget("", group, "");
    }

    public static NetworkMessageTarget wrapper(String wrapper) {
        return new NetworkMessageTarget("", "", wrapper);
    }

    public Map<String, String> payload() {
        return Map.of("targetService", service, "targetGroup", group, "targetWrapper", wrapper);
    }

}
