package eu.kryocloud.wrapper.screen;

import eu.kryocloud.api.screen.IScreen;

import java.io.*;

public class UnixScreen implements IScreen {

    private final String session;

    public UnixScreen(String session) {
        this.session = session;
    }

    public void start(String command) throws IOException {
        run("screen", "-dmS", session, "bash", "-c", command);
    }

    public void send(String command) throws IOException {
        run("screen", "-S", session, "-X", "stuff", command + "\n");
    }

    public String capture() throws IOException {
        File temp = File.createTempFile("screen_" + session, ".log");

        run("screen", "-S", session, "-X", "hardcopy", temp.getAbsolutePath());

        BufferedReader reader = new BufferedReader(new FileReader(temp));
        StringBuilder out = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            out.append(line).append("\n");
        }

        reader.close();
        temp.delete();

        return out.toString();
    }

    public void stop() throws IOException {
        run("screen", "-S", session, "-X", "quit");
    }

    public boolean exists() throws IOException {
        Process p = new ProcessBuilder("screen", "-ls").start();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream())
        );

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains("." + session)) return true;
        }

        return false;
    }

    private void run(String... cmd) throws IOException {
        new ProcessBuilder(cmd).start();
    }
}