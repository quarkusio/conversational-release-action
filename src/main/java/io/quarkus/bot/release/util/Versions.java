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
        if (Branches.MAIN.equals(version)) {
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

    public static String getPreviousMinorBranch(NavigableSet<ComparableVersion> existingBranches, ComparableVersion currentBranch) {
        String previousMinor = null;

        for (ComparableVersion branchCandidate : existingBranches.descendingSet()) {
            if (branchCandidate.compareTo(currentBranch) >= 0) {
                continue;
            }

            previousMinor = branchCandidate.toString();
            break;
        }

        if (previousMinor == null) {
            throw new IllegalStateException("Unable to determine previous minor for current branch " + currentBranch
                    + " and existing branches " + existingBranches);
        }

        return previousMinor;
    }

    public static String getMinorVersion(String version) {
        String[] elements = version.split("\\.");

        if (elements.length < 2) {
            return version;
        }

        return elements[0] + "." + elements[1];
    }

    public static String getDot0(String version) {
        return getMinorVersion(version) + ".0";
    }

    public static String getDot1(String version) {
        return getMinorVersion(version) + ".1";
    }

    public static boolean isDot0(String version) {
        return version.endsWith(".0");
    }

    public static boolean isFirstMicroMaintenanceRelease(String version) {
        return version.endsWith(".1");
    }
}
