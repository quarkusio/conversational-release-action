package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.arc.Arc;
import io.quarkus.bot.release.step.Prerequisites;
import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepStatus;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class IssuesTest {

    @Inject
    Issues issues;

    @Test
    void testExtractReleaseInformationFromForm() {
        String description = """
            ### Branch

            3.6

            ### Qualifier

            _No response_

            ### Major version

            - [ ] This release is a major version.
            """;

        assertThat(issues.extractReleaseInformationFromForm(description)).isEqualTo(new ReleaseInformation(null, "3.6", null, false));

        description = """
                ### Branch

                main

                ### Qualifier

                CR1

                ### Major version

                - [X] This release is a major version.
                """;

        assertThat(issues.extractReleaseInformationFromForm(description)).isEqualTo(new ReleaseInformation(null, "main", "CR1", true));

        assertThrows(IllegalStateException.class, () -> issues.extractReleaseInformationFromForm("foobar"));
    }

    @Test
    void testAppendReleaseInformation() {
        assertThat(issues.appendReleaseInformation("", new ReleaseInformation(null, "3.6", null, false))).isEqualTo("""


                <!-- quarkus-release/release-information:
                ---
                version: null
                branch: "3.6"
                qualifier: null
                major: false
                -->""");

        assertThat(issues.appendReleaseInformation("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                branch: "3.6"
                qualifier: null
                major: false
                -->""", new ReleaseInformation("3.7.1", "3.7", "CR1", true))).isEqualTo("""
                        This is a comment.

                        <!-- quarkus-release/release-information:
                        ---
                        version: "3.7.1"
                        branch: "3.7"
                        qualifier: "CR1"
                        major: true
                        -->""");
    }

    @Test
    void testExtractReleaseInformation() {
        assertThat(issues.extractReleaseInformation("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                version: null
                branch: "4.0"
                qualifier: CR1
                major: true
                -->

                <!-- quarkus-release/release-status:
                ---
                currentStep: "APPROVE_RELEASE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->""")).isEqualTo(new ReleaseInformation(null, "4.0", "CR1", true));

        assertThat(issues.extractReleaseInformation("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                version: "4.0.0.CR1"
                branch: "4.0"
                qualifier: CR1
                major: true
                -->

                <!-- quarkus-release/release-status:
                ---
                currentStep: "APPROVE_RELEASE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->""")).isEqualTo(new ReleaseInformation("4.0.0.CR1", "4.0", "CR1", true));
    }

    @Test
    void testAppendReleaseStatus() {
        assertThat(issues.appendReleaseStatus("", new ReleaseStatus(Status.STARTED, Step.APPROVE_CORE_RELEASE, StepStatus.STARTED, 123L))).isEqualTo("""


                <!-- quarkus-release/release-status:
                ---
                status: "STARTED"
                currentStep: "APPROVE_CORE_RELEASE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->""");

        assertThat(issues.appendReleaseStatus("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                branch: "3.6"
                qualifier: null
                major: false
                -->

                <!-- quarkus-release/release-status:
                ---
                status: "STARTED"
                currentStep: "APPROVE_CORE_RELEASE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->""", new ReleaseStatus(Status.COMPLETED, Step.CORE_RELEASE_PREPARE, StepStatus.COMPLETED, 145L))).isEqualTo("""
                        This is a comment.

                        <!-- quarkus-release/release-information:
                        ---
                        branch: "3.6"
                        qualifier: null
                        major: false
                        -->

                        <!-- quarkus-release/release-status:
                        ---
                        status: "COMPLETED"
                        currentStep: "CORE_RELEASE_PREPARE"
                        currentStepStatus: "COMPLETED"
                        workflowRunId: 145
                        -->""");
    }

    @Test
    void testExtractReleaseStatus() {
        assertThat(issues.extractReleaseStatus("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                branch: "4.0"
                qualifier: CR1
                major: true
                -->

                <!-- quarkus-release/release-status:
                ---
                status: "STARTED"
                currentStep: "APPROVE_CORE_RELEASE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->""")).isEqualTo(new ReleaseStatus(Status.STARTED, Step.APPROVE_CORE_RELEASE, StepStatus.STARTED, 123L));
    }
}
