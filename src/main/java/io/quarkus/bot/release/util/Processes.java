package io.quarkus.bot.release.util;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Singleton;

@Singleton
public class Processes {

    public int execute(List<String> commands) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(commands)
                .inheritIO()
                .start();
        return process.waitFor();
    }
}
