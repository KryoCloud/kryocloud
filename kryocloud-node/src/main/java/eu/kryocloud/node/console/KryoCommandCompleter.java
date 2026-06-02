package eu.kryocloud.node.console;

import eu.kryocloud.api.group.IGroup;
import eu.kryocloud.node.KryoNode;
import eu.kryocloud.node.service.runtime.NodeServiceSnapshot;
import eu.kryocloud.node.wrapper.WrapperSnapshot;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import java.util.List;

public final class KryoCommandCompleter implements Completer {

    private final KryoNode node;
    private final CommandRegistry commandRegistry;

    public KryoCommandCompleter(KryoNode node, CommandRegistry commandRegistry) {
        if (node == null) {
            throw new IllegalArgumentException("node must not be null");
        }

        if (commandRegistry == null) {
            throw new IllegalArgumentException("commandRegistry must not be null");
        }

        this.node = node;
        this.commandRegistry = commandRegistry;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        if (line == null) {
            return;
        }

        if (candidates == null) {
            return;
        }

        List<String> words = line.words();
        int wordIndex = line.wordIndex();

        if (wordIndex == 0) {
            add(candidates, commandRegistry.commandNames());
            return;
        }

        if (words.isEmpty()) {
            add(candidates, commandRegistry.commandNames());
            return;
        }

        String root = words.getFirst().toLowerCase();

        if ("cloud".equals(root) || "home".equals(root) || "kryo".equals(root) || "kryocloud".equals(root)) {
            completeCloud(words, wordIndex, candidates);
            return;
        }

        if ("group".equals(root) || "groups".equals(root)) {
            completeGroup(words, wordIndex, candidates);
            return;
        }

        if ("service".equals(root) || "services".equals(root) || "server".equals(root) || "servers".equals(root)) {
            completeService(words, wordIndex, candidates);
            return;
        }

        if ("wrapper".equals(root) || "wrappers".equals(root)) {
            completeWrapper(words, wordIndex, candidates);
            return;
        }

        if ("ip".equals(root) || "ips".equals(root) || "address".equals(root) || "addresses".equals(root) || "bind".equals(root)) {
            completeIp(words, wordIndex, candidates);
            return;
        }

        if ("version".equals(root) || "versions".equals(root)) {
            completeVersion(words, wordIndex, candidates);
            return;
        }

        if ("stats".equals(root) || "usage".equals(root) || "metrics".equals(root)) {
            completeStats(words, wordIndex, candidates);
        }
    }


    private void completeCloud(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "info", "home");
            return;
        }

        if (wordIndex == 2 && "home".equalsIgnoreCase(words.get(1))) {
            add(candidates, "set", "reset");
        }
    }

    private void completeGroup(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "setup", "list");
            addGroups(candidates);
            return;
        }

        if (wordIndex == 2 && !keyword(words.get(1), "setup", "list")) {
            add(candidates, "info", "start", "stop", "kill", "restart");
        }
    }

    private void completeService(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "list", "running", "cleanup");
            addServices(candidates);
            return;
        }

        if (wordIndex == 2 && !keyword(words.get(1), "list", "running", "cleanup")) {
            add(candidates, "info", "stop", "kill", "logs", "cmd", "command");
            return;
        }

        if (wordIndex == 2 && "cleanup".equalsIgnoreCase(words.get(1))) {
            add(candidates, "all", "--dry-run");
            addWrappers(candidates);
            return;
        }

        if (wordIndex == 3 && "cleanup".equalsIgnoreCase(words.get(1))) {
            add(candidates, "--dry-run");
        }
    }

    private void completeWrapper(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "list", "timedout");
            addWrappers(candidates);
            return;
        }

        if (wordIndex == 2 && !keyword(words.get(1), "list", "timedout")) {
            add(candidates, "info");
        }
    }

    private void completeIp(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "list", "add", "remove", "default");
            return;
        }

        if (wordIndex == 2 && keyword(words.get(1), "add", "remove", "default")) {
            add(candidates, "server", "proxy");
            return;
        }

        if (wordIndex == 3 && keyword(words.get(1), "remove", "default")) {
            addAddresses(words.get(2), candidates);
        }
    }

    private void completeVersion(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "list", "refresh", "install", "create", "scan");
            add(candidates, node.versionStorage().availableSoftware());
            return;
        }

        if (wordIndex == 2 && keyword(words.get(1), "install")) {
            add(candidates, node.versionStorage().availableSoftware());
            return;
        }

        if (wordIndex == 2 && !keyword(words.get(1), "list", "refresh", "create", "scan")) {
            add(candidates, "list", "versions", "info", "install");
            return;
        }

        if (wordIndex == 3 && !keyword(words.get(1), "list", "refresh", "create", "scan")) {
            addVersions(words.get(1), candidates);
            return;
        }

        if (wordIndex == 3 && "install".equalsIgnoreCase(words.get(1))) {
            addVersions(words.get(2), candidates);
        }
    }

    private void completeStats(List<String> words, int wordIndex, List<Candidate> candidates) {
        if (wordIndex == 1) {
            add(candidates, "groups", "group", "live");
            return;
        }

        if (wordIndex == 2 && "group".equalsIgnoreCase(words.get(1))) {
            addGroups(candidates);
        }
    }

    private void addVersions(String software, List<Candidate> candidates) {
        try {
            add(candidates, "latest");
            add(candidates, node.versionStorage().availableVersions(software));
        } catch (Exception exception) {
            add(candidates, "latest");
        }
    }

    private void addGroups(List<Candidate> candidates) {
        for (IGroup group : node.groupManager().groups()) {
            candidates.add(new Candidate(group.name()));
        }
    }

    private void addServices(List<Candidate> candidates) {
        for (NodeServiceSnapshot service : node.serviceRegistry().services()) {
            candidates.add(new Candidate(service.serviceId()));
        }
    }

    private void addWrappers(List<Candidate> candidates) {
        for (WrapperSnapshot wrapper : node.wrapperRegistry().wrappers()) {
            candidates.add(new Candidate(wrapper.wrapperId()));
        }
    }

    private void addAddresses(String type, List<Candidate> candidates) {
        if ("proxy".equalsIgnoreCase(type) || "public".equalsIgnoreCase(type)) {
            add(candidates, node.networkAddressConfig().getProxyAddresses());
            return;
        }

        add(candidates, node.networkAddressConfig().getServerAddresses());
    }

    private void add(List<Candidate> candidates, String... values) {
        for (String value : values) {
            candidates.add(new Candidate(value));
        }
    }

    private void add(List<Candidate> candidates, List<String> values) {
        for (String value : values) {
            candidates.add(new Candidate(value));
        }
    }

    private boolean keyword(String value, String... keywords) {
        for (String keyword : keywords) {
            if (keyword.equalsIgnoreCase(value)) {
                return true;
            }
        }

        return false;
    }
}
