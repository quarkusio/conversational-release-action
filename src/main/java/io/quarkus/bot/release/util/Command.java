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
            if (body.toLowerCase(Locale.ENGLISH).startsWith("@" + Users.QUARKUS_BOT + " " + command)) {
                return true;
            }
        }

        return false;
    }

    public String getFullCommand() {
        return "@" + Users.QUARKUS_BOT + " " + commands[0];
    }
}
