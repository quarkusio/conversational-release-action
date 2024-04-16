package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.util.Labels;

public class LabelsTest {

    @Test
    void testForVersion() {
        assertThat(Labels.backportForVersion("3.8")).isEqualTo("triage/backport-3.8");
        assertThat(Labels.backportForVersion("3.10")).isEqualTo("triage/backport-3.10");
        assertThat(Labels.backportForVersion("3.8").replace("/", "%2F")).isEqualTo("triage%2Fbackport-3.8");
    }
}
