package io.quarkus.bot.release.util;

import java.util.NavigableSet;

import org.apache.maven.artifact.versioning.ComparableVersion;

public final class Versions {

    public static final ComparableVersion MAIN = new ComparableVersion("999-SNAPSHOT");
    public static final ComparableVersion VERSION_3_2 = new ComparableVersion("3.2");
    public static final ComparableVersion VERSION_3_6 = new ComparableVersion("3.6");
    public static final ComparableVersion VERSION_3_7 = new ComparableVersion("3.7");

    private Versions() {
    }

    public static ComparableVersion getVersion(String version) {
        if ("main".equals(version)) {
            return MAIN;
        }

        return new ComparableVersion(version);
    }

    public static ComparableVersion getBranch(String version) {
        String[] elements = version.split("\\.");

        if (elements.length < 2) {
            return getVersion(version);
        }

        return getVersion(elements[0] + "." + elements[1]);
    }

    public static String getPreviousMinor(NavigableSet<ComparableVersion> existingBranches, ComparableVersion currentBranch) {
        String previousVersion = null;

        for (ComparableVersion branchCandidate : existingBranches.descendingSet()) {
            if (branchCandidate.compareTo(currentBranch) >= 0) {
                continue;
            }

            previousVersion = branchCandidate.toString() + ".0";
            break;
        }

        if (previousVersion == null) {
            throw new IllegalStateException("Unable to determine previous minor for current branch " + currentBranch
                    + " and existing branches " + existingBranches);
        }

        return previousVersion;
    }
}
