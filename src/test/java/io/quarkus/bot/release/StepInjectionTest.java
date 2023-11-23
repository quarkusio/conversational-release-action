package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.bot.release.step.Step;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StepInjectionTest {

    @Test
    void testStepInjection() {
        for (Step step : Step.values()) {
            assertThat(Arc.container().instance(step.getStepHandler()).get()).isNotNull();
        }
    }
}
