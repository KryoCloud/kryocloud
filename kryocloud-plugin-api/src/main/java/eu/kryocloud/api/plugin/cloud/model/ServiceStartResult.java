package eu.kryocloud.api.plugin.cloud.model;

public record ServiceStartResult(String service, String group, String wrapper, boolean success, String message) {

    public ServiceStartResult {
        service = service == null ? "" : service.trim();
        group = group == null ? "" : group.trim();
        wrapper = wrapper == null ? "" : wrapper.trim();
        message = message == null ? "" : message.trim();
    }

}
