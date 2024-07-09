package io.quarkus.bot.release.util;

import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public final class Repositories {

    private static final String QUARKUSIO_QUARKUS = "quarkusio/quarkus";
    private static final String QUARKUSIO_QUARKUS_PLATFORM = "quarkusio/quarkus-platform";

    private Repositories() {
    }

    public static GHRepository getQuarkusRepository(GitHub quarkusBotGitHub) {
        return getRepository(quarkusBotGitHub, QUARKUSIO_QUARKUS);
    }

    public static GHRepository getQuarkusPlatformRepository(GitHub quarkusBotGitHub) {
        return getRepository(quarkusBotGitHub, QUARKUSIO_QUARKUS_PLATFORM);
    }

    private static GHRepository getRepository(GitHub quarkusBotGitHub, String repository) {
        try {
            return quarkusBotGitHub.getRepository(repository);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get " + repository + " repository", e);
        }
    }
}
