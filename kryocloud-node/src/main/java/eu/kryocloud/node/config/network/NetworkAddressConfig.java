package eu.kryocloud.node.config.network;

import eu.kryocloud.common.config.Comment;
import eu.kryocloud.common.config.Config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NetworkAddressConfig extends Config {

    @Comment("Comma separated addresses used by backend Minecraft servers. Usually local/private addresses.")
    private String serverAddresses = "127.0.0.1";

    @Comment("Comma separated addresses used by proxy Minecraft services. Usually public addresses.")
    private String proxyAddresses = "0.0.0.0";

    @Comment("Default address for backend server groups")
    private String defaultServerAddress = "127.0.0.1";

    @Comment("Default address for proxy groups")
    private String defaultProxyAddress = "0.0.0.0";

    public NetworkAddressConfig(Path path) {
        super(path);
    }

    public List<String> getServerAddresses() {
        return split(serverAddresses);
    }

    public List<String> getProxyAddresses() {
        return split(proxyAddresses);
    }

    public String getDefaultServerAddress() {
        return defaultServerAddress;
    }

    public String getDefaultProxyAddress() {
        return defaultProxyAddress;
    }

    public void addServerAddress(String address) {
        List<String> addresses = getServerAddresses();
        addUnique(addresses, normalizeAddress(address, "address"));
        serverAddresses = join(addresses);
    }

    public void addProxyAddress(String address) {
        List<String> addresses = getProxyAddresses();
        addUnique(addresses, normalizeAddress(address, "address"));
        proxyAddresses = join(addresses);
    }

    public boolean removeServerAddress(String address) {
        List<String> addresses = getServerAddresses();
        boolean removed = addresses.removeIf(entry -> entry.equalsIgnoreCase(normalizeAddress(address, "address")));
        serverAddresses = join(addresses);
        return removed;
    }

    public boolean removeProxyAddress(String address) {
        List<String> addresses = getProxyAddresses();
        boolean removed = addresses.removeIf(entry -> entry.equalsIgnoreCase(normalizeAddress(address, "address")));
        proxyAddresses = join(addresses);
        return removed;
    }

    public void setDefaultServerAddress(String address) {
        String normalized = normalizeAddress(address, "address");
        addServerAddress(normalized);
        defaultServerAddress = normalized;
    }

    public void setDefaultProxyAddress(String address) {
        String normalized = normalizeAddress(address, "address");
        addProxyAddress(normalized);
        defaultProxyAddress = normalized;
    }

    public List<String> addressesFor(String serviceType) {
        String normalized = serviceType == null ? "" : serviceType.toUpperCase();

        if ("PROXY".equals(normalized)) {
            return getProxyAddresses();
        }

        return getServerAddresses();
    }

    public String defaultFor(String serviceType) {
        String normalized = serviceType == null ? "" : serviceType.toUpperCase();

        if ("PROXY".equals(normalized)) {
            return defaultProxyAddress;
        }

        return defaultServerAddress;
    }

    public void normalize() {
        List<String> servers = getServerAddresses();
        List<String> proxies = getProxyAddresses();

        if (servers.isEmpty()) {
            servers.add("127.0.0.1");
        }

        if (proxies.isEmpty()) {
            proxies.add("0.0.0.0");
        }

        defaultServerAddress = normalizeAddress(defaultServerAddress == null || defaultServerAddress.isBlank() ? servers.getFirst() : defaultServerAddress, "defaultServerAddress");
        defaultProxyAddress = normalizeAddress(defaultProxyAddress == null || defaultProxyAddress.isBlank() ? proxies.getFirst() : defaultProxyAddress, "defaultProxyAddress");
        addUnique(servers, defaultServerAddress);
        addUnique(proxies, defaultProxyAddress);
        serverAddresses = join(servers);
        proxyAddresses = join(proxies);
    }

    private List<String> split(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }

        return new ArrayList<>(Arrays.stream(raw.split(",")).map(String::trim).filter(entry -> !entry.isBlank()).distinct().toList());
    }

    private String join(List<String> addresses) {
        return String.join(",", addresses);
    }

    private void addUnique(List<String> addresses, String address) {
        if (addresses.stream().anyMatch(entry -> entry.equalsIgnoreCase(address))) {
            return;
        }

        addresses.add(address);
    }

    private String normalizeAddress(String address, String name) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        String normalized = address.trim();

        if (!normalized.matches("[A-Za-z0-9_.:\\-]+")) {
            throw new IllegalArgumentException(name + " contains unsupported characters: " + address);
        }

        return normalized;
    }
}
