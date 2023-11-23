package io.quarkus.bot.release.error;

public class StepExecutionException extends RuntimeException {

    public StepExecutionException(String message) {
        super(message);
    }

    public StepExecutionException(String message, Throwable e) {
        super(message, e);
    }
}
