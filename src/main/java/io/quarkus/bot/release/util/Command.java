package io.quarkus.bot.release.util;

import java.util.Locale;

public enum Command {

    YES("yes"),
    CONTINUE("continue"),
    RETRY("retry");

    private final String[] commands;

    Command(String... commands) {
        this.commands = commands;
    }

    public boolean matches(String body) {
        for (String command : commands) {
            if (body.toLowerCase(Locale.ENGLISH).startsWith(command)) {
                return true;
            }
        }

        return false;
    }
}
