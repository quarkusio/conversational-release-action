package io.quarkus.bot.release.error;

public class StepExecutionException extends RuntimeException {

    private final String contextualTip;

    private final boolean fatal;

    public StepExecutionException(String message) {
        super(message);
        this.contextualTip = null;
        this.fatal = false;
    }

    public StepExecutionException(String message, Throwable e) {
        super(message, e);
        this.contextualTip = null;
        this.fatal = false;
    }

    public StepExecutionException(String message, String contextualTip) {
        super(message);
        this.contextualTip = contextualTip;
        this.fatal = false;
    }

    public StepExecutionException(String message, String contextualTip, Throwable e) {
        super(message, e);
        this.contextualTip = contextualTip;
        this.fatal = false;
    }

    public StepExecutionException(String message, boolean fatal) {
        super(message);
        this.contextualTip = null;
        this.fatal = fatal;
    }

    public StepExecutionException(String message, boolean fatal, Throwable e) {
        super(message, e);
        this.contextualTip = null;
        this.fatal = fatal;
    }

    public StepExecutionException(String message, boolean fatal, String contextualTip) {
        super(message);
        this.contextualTip = contextualTip;
        this.fatal = fatal;
    }

    public StepExecutionException(String message, boolean fatal, String contextualTip, Throwable e) {
        super(message, e);
        this.contextualTip = contextualTip;
        this.fatal = fatal;
    }

    public String getContextualTip() {
        return contextualTip;
    }

    public boolean isFatal() {
        return fatal;
    }
}
