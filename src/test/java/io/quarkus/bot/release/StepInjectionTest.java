package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.bot.release.step.Prerequisites;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class StepInjectionTest {

    @Test
    void testStepInjection() {
        assertThat(Arc.container().instance(Prerequisites.class).get()).isNotNull();
    }
}
