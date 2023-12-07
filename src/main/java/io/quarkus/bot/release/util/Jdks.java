package io.quarkus.bot.release.util;

import jakarta.inject.Singleton;

import io.quarkus.arc.Unremovable;

@Singleton
@Unremovable
public class Jdks {

    private static final String MAIN = "main";

    private static final String JDK_11 = "11";
    private static final String JDK_17 = "17";

    public String getJdkVersion(String branch) {
        if (MAIN.equals(branch)) {
            return JDK_17;
        }

        if (Versions.getVersion(branch).compareTo(Versions.VERSION_3_7) >= 0) {
            return JDK_17;
        }

        return JDK_11;
    }
}
