package io.quarkus.bot.release.util;

import java.util.List;

import io.quarkus.bot.release.ReleaseInformation;

public class Branches {

    public static final String MAIN = "main";
    public static final List<String> LTS_BRANCHES = List.of("3.2", "2.13");

    public static String getPlatformPreparationBranch(ReleaseInformation releaseInformation) {
        if (releaseInformation.isFinal() && !releaseInformation.isFirstFinal()) {
            return releaseInformation.getBranch();
        }

        return MAIN;
    }

    public static String getPlatformReleaseBranch(ReleaseInformation releaseInformation) {
        if (releaseInformation.isFinal()) {
            return releaseInformation.getBranch();
        }

        return MAIN;
    }

    private Branches() {
    }
}
