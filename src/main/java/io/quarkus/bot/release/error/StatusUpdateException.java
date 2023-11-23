package io.quarkus.bot.release.error;

public class StatusUpdateException extends RuntimeException {

    public StatusUpdateException(String message) {
        super(message);
    }

    public StatusUpdateException(String message, Throwable e) {
        super(message, e);
    }
}
