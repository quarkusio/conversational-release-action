package io.quarkus.bot.release.util;

import java.util.List;

import org.apache.maven.artifact.versioning.ComparableVersion;

import io.quarkus.bot.release.ReleaseInformation;

public class Branches {

    public static final String MAIN = "main";
    private static final List<String> LTS_BRANCHES = List.of("3.27", "3.20", "3.15", "3.8", "3.2");
    public static final String BRANCH_3_8 = "3.8";

    public static String getPlatformPreparationBranch(ReleaseInformation releaseInformation) {
        if (releaseInformation.isFinal()
                && (!releaseInformation.isFirstFinal() || Branches.isLts(releaseInformation.getBranch()))) {
            return releaseInformation.getBranch();
        }
        if (!releaseInformation.isOriginBranchMain()) {
            return releaseInformation.getBranch();
        }

        return MAIN;
    }

    public static String getPlatformReleaseBranch(ReleaseInformation releaseInformation) {
        if (releaseInformation.isFinal()) {
            return releaseInformation.getBranch();
        }
        if (!releaseInformation.isOriginBranchMain()) {
            return releaseInformation.getBranch();
        }

        return MAIN;
    }

    public static boolean isLts(String branch) {
        return LTS_BRANCHES.contains(branch);
    }

    public static List<String> getLtsVersionsReleasedBefore(String version) {
        ComparableVersion comparableVersion = new ComparableVersion(version);

        return LTS_BRANCHES.stream()
                .map(v -> new ComparableVersion(v))
                .filter(lts -> lts.compareTo(comparableVersion) < 0)
                .sorted()
                .map(v -> v.toString())
                .toList();
    }

    public static String getNextMinor(String branch) {
        String[] segments = branch.split("\\.");

        if (segments.length < 2) {
            throw new IllegalArgumentException("Invalid branch format: " + branch);
        }

        return segments[0] + "." + (Integer.parseInt(segments[1]) + 1);
    }

    public static String getPreviousMinor(String branch) {
        String[] segments = branch.split("\\.");

        if (segments.length < 2) {
            throw new IllegalArgumentException("Invalid branch format: " + branch);
        }

        int minorSegment = Integer.parseInt(segments[1]);

        if (minorSegment == 0) {
            throw new IllegalArgumentException("Unable to generate previous minor for .0 releases: " + branch);
        }

        return segments[0] + "." + (minorSegment - 1);
    }

    public static String getFullBranch(String branch) {
        if (isLts(branch)) {
            return branch + " LTS";
        }

        return branch;
    }

    public static boolean isLtsBranchWithRegularReleaseCadence(String branch) {
        return Branches.isLts(branch) && new ComparableVersion(branch).compareTo(new ComparableVersion(BRANCH_3_8)) >= 0;
    }

    private Branches() {
    }
}
