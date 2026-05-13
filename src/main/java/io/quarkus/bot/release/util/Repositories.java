package io.quarkus.bot.release.util;

import java.io.IOException;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

public final class Repositories {

    private static final String QUARKUSIO_QUARKUS = "quarkusio/quarkus";
    private static final String QUARKUSIO_QUARKUS_PLATFORM = "quarkusio/quarkus-platform";
    private static final String GITHUB_URL = "https://github.com/";

    private Repositories() {
    }

    public static GHRepository getQuarkusRepository(GitHub quarkusBotGitHub) {
        return getRepository(quarkusBotGitHub, QUARKUSIO_QUARKUS);
    }

    public static GHRepository getQuarkusPlatformRepository(GitHub quarkusBotGitHub) {
        return getRepository(quarkusBotGitHub, QUARKUSIO_QUARKUS_PLATFORM);
    }

    public static String getCoreBranchLink(String branch) {
        return "[`" + branch + "`](" + GITHUB_URL + QUARKUSIO_QUARKUS + "/tree/" + branch + ")";
    }

    public static String getPlatformBranchLink(String branch) {
        return "[`" + branch + "`](" + GITHUB_URL + QUARKUSIO_QUARKUS_PLATFORM + "/tree/" + branch + ")";
    }

    private static GHRepository getRepository(GitHub quarkusBotGitHub, String repository) {
        try {
            return quarkusBotGitHub.getRepository(repository);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get " + repository + " repository", e);
        }
    }
}
