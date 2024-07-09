package io.quarkus.bot.release.util;

public final class Strings {

    private Strings() {
    }

    public static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }
}
