package io.quarkus.bot.release.util;

/**
 * See https://github.com/orgs/community/discussions/16925
 */
public final class Admonitions {

    private Admonitions() {
    }

    public static String note(String note) {
        return admonition(AdmonitionType.NOTE, note);
    }

    public static String tip(String tip) {
        return admonition(AdmonitionType.TIP, tip);
    }

    public static String important(String important) {
        return admonition(AdmonitionType.IMPORTANT, important);
    }

    public static String warning(String warning) {
        return admonition(AdmonitionType.WARNING, warning);
    }

    public static String caution(String caution) {
        return admonition(AdmonitionType.CAUTION, caution);
    }

    private static String admonition(AdmonitionType type, String admonition) {
        StringBuilder formattedAdmonition = new StringBuilder();
        formattedAdmonition.append("> [!").append(type.name()).append("]\n");
        formattedAdmonition.append("> ").append(admonition.replace("\n", "\n> "));
        return formattedAdmonition.toString();
    }

    private enum AdmonitionType {
        NOTE,
        TIP,
        IMPORTANT,
        WARNING,
        CAUTION;
    }
}
