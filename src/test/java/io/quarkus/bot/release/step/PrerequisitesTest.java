package io.quarkus.bot.release.step;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class PrerequisitesTest {

    @ParameterizedTest
    // previousVersion, micro, emergency, qualifier, expectedVersion
    @CsvSource({
            // Pre-CR1: Beta2 after Beta1 (previous was preview, keep base version)
            "4.0.0.Beta1, true, false, Beta2, 4.0.0.Beta2",
            // Pre-CR1: CR1 after Beta2
            "4.0.0.Beta2, true, false, CR1, 4.0.0.CR1",
            // CR2 after CR1
            "4.0.0.CR1, true, false, CR2, 4.0.0.CR2",
            // Final after last CR (no qualifier)
            "4.0.0.CR2, true, false, '', 4.0.0",
            // Bugfix bump (3-segment version)
            "3.39.3, true, false, '', 3.39.4",
            // First CR with no prior preview (3-segment version)
            "3.39.0, true, false, CR1, 3.39.1.CR1",
            // Minor bump (non-micro, non-emergency)
            "3.39.2, false, false, '', 3.40.0",
            // Emergency release (no prior emergency)
            "3.20.2, false, true, '', 3.20.2.1",
            // Emergency release (prior emergency)
            "3.20.2.1, false, true, '', 3.20.2.2",
            // Final qualifier handling
            "3.39.0.Final, true, false, '', 3.39.1.Final",
    })
    public void testComputeNewVersion(String previousVersion, boolean micro, boolean emergency, String qualifier,
            String expectedVersion) {
        assertThat(Prerequisites.computeNewVersion(previousVersion, micro, emergency, qualifier))
                .as("previousVersion=%s, micro=%s, emergency=%s, qualifier=%s", previousVersion, micro, emergency, qualifier)
                .isEqualTo(expectedVersion);
    }
}
