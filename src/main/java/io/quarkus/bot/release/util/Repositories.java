package io.quarkus.bot.release.util;

import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public final class Repositories {

    private static final String QUARKUSIO_QUARKUS = "quarkusio/quarkus";

    private Repositories() {
    }

    public static GHRepository getQuarkusRepository(GitHub gitHub) {
        try {
            return gitHub.getRepository(QUARKUSIO_QUARKUS);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get " + QUARKUSIO_QUARKUS + " repository", e);
        }
    }
}
