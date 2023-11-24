package io.quarkus.bot.release.util;

import java.util.regex.Pattern;

public class UpdatedIssueBody {

    private String body;

    public UpdatedIssueBody(String body) {
        this.body = body;
    }

    public String getBody() {
        return body;
    }

    public boolean contains(String section) {
        if (isBlank()) {
            return false;
        }

        return body.contains(section);
    }

    public String append(String append) {
        this.body = (this.body != null ? this.body + "\n\n" : "") + append;
        return this.body;
    }

    public String replace(Pattern pattern, String replacement) {
        this.body = pattern.matcher(body).replaceFirst(replacement);
        return this.body;
    }

    public boolean isBlank() {
        return body == null || body.isBlank();
    }

    @Override
    public String toString() {
        return body;
    }
}
