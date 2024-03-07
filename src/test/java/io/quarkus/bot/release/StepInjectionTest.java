package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.step.Step;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StepInjectionTest {

    @Test
    void testStepInjection() {
        for (Step step : Step.values()) {
            assertThat(step.getStepHandler())
                    .as("Step handler for %s does not exist or is not an unremovable CDI bean", step.name())
                    .isNotNull();
        }
    }
}
