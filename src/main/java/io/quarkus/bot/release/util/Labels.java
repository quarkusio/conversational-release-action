package io.quarkus.bot.release.util;

public final class Labels {

    public static final String BACKPORT_LABEL = "triage/backport";
    public static final String BACKPORT_LABEL_COLOR = "7fe8cd";

    private Labels() {
    }

    public static String forVersion(String branch) {
        return BACKPORT_LABEL + "-" + branch;
    }
}
