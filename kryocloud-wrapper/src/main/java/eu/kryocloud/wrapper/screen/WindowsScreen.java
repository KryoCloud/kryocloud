package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;

import java.io.*;

public class WindowsScreen implements IScreen {

    private final String session;
    private Process process;
    private BufferedWriter writer;
    private final StringBuilder buffer = new StringBuilder();

    public WindowsScreen(String session) {
        this.session = session;
    }

    public void start(String command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("cmd.exe");
        pb.redirectErrorStream(true);

        process = pb.start();

        writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (buffer) {
                        buffer.append(line).append("\n");
                    }
                }

            } catch (IOException ignored) {}
        }).start();

        send(command);
    }

    public void send(String command) throws IOException {
        writer.write(command);
        writer.newLine();
        writer.flush();
    }

    public String capture() {
        synchronized (buffer) {
            return buffer.toString();
        }
    }

    public void stop() throws IOException {
        if (process == null) return;

        send("exit");
        process.destroy();
    }

    public boolean exists() {
        return process != null && process.isAlive();
    }
}