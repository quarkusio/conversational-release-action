package io.quarkus.bot.release.util;

import java.util.Locale;

public final class Users {

    public static final String QUARKUS_BOT = "quarkusbot";

    public static final String BOT_SUFFIX = "[bot]";
    public static final String BOT_SUFFIX_2 = "-bot";

    private Users() {
    }

    public static boolean isIgnored(String login) {
        if (login == null || login.isBlank()) {
            return true;
        }

        String normalizedLogin = login.toLowerCase(Locale.ENGLISH).trim();
        return normalizedLogin.endsWith(BOT_SUFFIX)
                || normalizedLogin.endsWith(BOT_SUFFIX_2);
    }
}
