package io.quarkus.bot.release.util;

import java.util.List;

import io.quarkus.bot.release.ReleaseInformation;

public class Branches {

    public static final String MAIN = "main";
    public static final List<String> LTS_BRANCHES = List.of("3.8", "3.2", "2.13");
    public static final String BRANCH_2_13 = "2.13";

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

    public static boolean isLts(String branch) {
        return LTS_BRANCHES.contains(branch);
    }

    private Branches() {
    }
}
