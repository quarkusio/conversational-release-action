package io.quarkus.bot.release.step;

import java.util.Map;

public record StepResult(int exitCode, Map<String, String> properties) {

    private static final StepResult SUCCESS = new StepResult(0, Map.of());

    public static StepResult success() {
        return SUCCESS;
    }

    public static StepResult success(Map<String, String> properties) {
        return new StepResult(0, properties);
    }

    public static StepResult of(int exitCode) {
        if (exitCode == 0) {
            return SUCCESS;
        }
        return new StepResult(exitCode, Map.of());
    }
}
