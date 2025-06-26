package io.quarkus.bot.release.util;

public final class Labels {

    public static final String BACKPORT_LABEL = "triage/backport";
    public static final String BACKPORT_LABEL_COLOR = "7fe8cd";

    public static final String RELEASE_NOTEWORTHY_FEATURE_LABEL = "release/noteworthy-feature";
    public static final String RELEASE_BREAKING_CHANGE_LABEL = "release/breaking-change";

    private Labels() {
    }

    public static String backportForVersion(String branch) {
        return BACKPORT_LABEL + "-" + branch;
    }
}
