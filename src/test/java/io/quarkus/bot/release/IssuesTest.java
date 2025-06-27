package io.quarkus.bot.release;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepStatus;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.UpdatedIssueBody;
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

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThat(issues.extractReleaseInformationFromForm(description))
                .isEqualTo(new ReleaseInformation(null, "3.6", Branches.MAIN, null, false, null, false, false, false));

        description = """
                ### Branch

                main

                ### Origin branch

                3.14

                ### Qualifier

                CR1

                ### Major version

                - [x] This release is a major version.
                """;

        assertThat(issues.extractReleaseInformationFromForm(description))
                .isEqualTo(new ReleaseInformation(null, "main", "3.14", "CR1", false, null, true, false, false));

        description = """
                ### Branch

                3.20

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Emergency release

                - [x] This release is an emergency release.

                ### Emergency release Core branch

                3.20.1-emergency-1

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThat(issues.extractReleaseInformationFromForm(description))
                .isEqualTo(new ReleaseInformation(null, "3.20", "main", null, true, "3.20.1-emergency-1", false, false, false));

        description = """
                ### Branch

                3.20

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Emergency release

                - [x] This release is an emergency release.

                ### Emergency release Core branch

                _No response_

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThat(issues.extractReleaseInformationFromForm(description))
                .isEqualTo(new ReleaseInformation(null, "3.20", "main", null, true, null, false, false, false));

        assertThrows(IllegalStateException.class, () -> issues.extractReleaseInformationFromForm("foobar"));

        final String invalidEmergencyVersionNonLts = """
                ### Branch

                3.17

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Emergency release

                - [x] This release is an emergency release.

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThatThrownBy(() -> issues.extractReleaseInformationFromForm(invalidEmergencyVersionNonLts))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Emergency releases are only supported for LTS branches with regular release cadence.");

        final String invalidEmergencyVersionMajor = """
                ### Branch

                3.20

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Emergency release

                - [x] This release is an emergency release.

                ### Major version

                - [x] This release is a major version.
                """;

        assertThatThrownBy(() -> issues.extractReleaseInformationFromForm(invalidEmergencyVersionMajor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("A release may not be both an emergency and a major release");

        final String invalidEmergencyVersionQualifier = """
                ### Branch

                3.20

                ### Origin branch

                _No response_

                ### Qualifier

                CR1

                ### Emergency release

                - [x] This release is an emergency release.

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThatThrownBy(() -> issues.extractReleaseInformationFromForm(invalidEmergencyVersionQualifier))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An emergency release may not have a qualifier");

        String invalidEmergencyReleaseCoreBranch = """
                ### Branch

                3.20

                ### Origin branch

                _No response_

                ### Qualifier

                _No response_

                ### Emergency release

                - [ ] This release is an emergency release.

                ### Emergency release Core branch

                3.20.1-emergency-1

                ### Major version

                - [ ] This release is a major version.
                """;

        assertThatThrownBy(() -> issues.extractReleaseInformationFromForm(invalidEmergencyReleaseCoreBranch))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("An emergency release Core branch may only be defined for emergency releases");
    }

    @Test
    void testExtractPartialReleaseInformation() {
        assertThat(issues.extractReleaseInformation(new UpdatedIssueBody("""
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
                currentStep: "CORE_RELEASE_APPROVE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->"""))).isEqualTo(new ReleaseInformation(null, "4.0", null, "CR1", false, null, true, false, false));
    }

    @Test
    void testAppendReleaseInformation() {
        assertThat(issues.appendReleaseInformation(new UpdatedIssueBody(""),
                new ReleaseInformation(null, "3.6", Branches.MAIN, null, false, null, false, false, false))).isEqualTo("""


                        <!-- quarkus-release/release-information:
                        ---
                        version: null
                        branch: "3.6"
                        originBranch: "main"
                        qualifier: null
                        emergency: false
                        emergencyReleaseCoreBranch: null
                        major: false
                        firstFinal: false
                        maintenance: false
                        -->""");

        assertThat(issues.appendReleaseInformation(new UpdatedIssueBody("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                branch: "3.6"
                originBranch: "main"
                qualifier: null
                emergency: false
                emergencyReleaseCoreBranch: null
                major: false
                firstFinal: false
                maintenance: false
                -->"""), new ReleaseInformation("3.7.1", "3.7", Branches.MAIN, "CR1", false, null, true, false, false)))
                .isEqualTo("""
                        This is a comment.

                        <!-- quarkus-release/release-information:
                        ---
                        version: "3.7.1"
                        branch: "3.7"
                        originBranch: "main"
                        qualifier: "CR1"
                        emergency: false
                        emergencyReleaseCoreBranch: null
                        major: true
                        firstFinal: false
                        maintenance: false
                        -->""");
    }

    @Test
    void testAppendReleaseInformationEmergencyRelease() {
        assertThat(issues.appendReleaseInformation(new UpdatedIssueBody(""),
                new ReleaseInformation(null, "3.20", Branches.MAIN, null, true, "3.20.1-emergency-1", false, false, false)))
                .isEqualTo("""


                        <!-- quarkus-release/release-information:
                        ---
                        version: null
                        branch: "3.20"
                        originBranch: "main"
                        qualifier: null
                        emergency: true
                        emergencyReleaseCoreBranch: "3.20.1-emergency-1"
                        major: false
                        firstFinal: false
                        maintenance: false
                        -->""");

        assertThat(issues.appendReleaseInformation(new UpdatedIssueBody("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                branch: "3.20"
                originBranch: "main"
                qualifier: null
                emergency: true
                emergencyReleaseCoreBranch: "3.20.1-emergency-1"
                major: false
                firstFinal: false
                maintenance: false
                -->"""), new ReleaseInformation("3.21.1", "3.21", Branches.MAIN, "CR1", false, null, true, false, false)))
                .isEqualTo("""
                        This is a comment.

                        <!-- quarkus-release/release-information:
                        ---
                        version: "3.21.1"
                        branch: "3.21"
                        originBranch: "main"
                        qualifier: "CR1"
                        emergency: false
                        emergencyReleaseCoreBranch: null
                        major: true
                        firstFinal: false
                        maintenance: false
                        -->""");
    }

    @Test
    void testExtractReleaseInformation() {
        assertThat(issues.extractReleaseInformation(new UpdatedIssueBody("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                version: null
                branch: "4.0"
                originBranch: "main"
                qualifier: CR1
                emergency: false
                major: true
                firstFinal: true
                maintenance: true
                -->

                <!-- quarkus-release/release-status:
                ---
                currentStep: "CORE_RELEASE_APPROVE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->"""))).isEqualTo(new ReleaseInformation(null, "4.0", Branches.MAIN, "CR1", false, null, true, true, true));

        assertThat(issues.extractReleaseInformation(new UpdatedIssueBody("""
                This is a comment.

                <!-- quarkus-release/release-information:
                ---
                version: "4.0.0.CR1"
                branch: "4.0"
                originBranch: "3.99"
                qualifier: CR1
                emergency: false
                major: true
                firstFinal: false
                maintenance: false
                -->

                <!-- quarkus-release/release-status:
                ---
                currentStep: "CORE_RELEASE_APPROVE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->"""))).isEqualTo(new ReleaseInformation("4.0.0.CR1", "4.0", "3.99", "CR1", false, null, true, false, false));
    }

    @Test
    void testAppendReleaseStatus() {
        assertThat(issues.appendReleaseStatus(new UpdatedIssueBody(""),
                new ReleaseStatus(Status.STARTED, Step.CORE_RELEASE_APPROVE, StepStatus.STARTED, 123L))).matches("""


                        <!-- quarkus-release/release-status:
                        ---
                        status: "STARTED"
                        currentStep: "CORE_RELEASE_APPROVE"
                        currentStepStatus: "STARTED"
                        workflowRunId: 123
                        date: ".*"
                        -->""");

        assertThat(issues.appendReleaseStatus(new UpdatedIssueBody("""
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
                currentStep: "CORE_RELEASE_APPROVE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->"""), new ReleaseStatus(Status.COMPLETED, Step.CORE_RELEASE_PREPARE, StepStatus.COMPLETED, 145L)))
                .matches("""
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
                        date: ".*"
                        -->""");
    }

    @Test
    void testExtractReleaseStatus() {
        assertThat(issues.extractReleaseStatus(new UpdatedIssueBody("""
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
                currentStep: "CORE_RELEASE_APPROVE"
                currentStepStatus: "STARTED"
                workflowRunId: 123
                -->"""))).isEqualTo(new ReleaseStatus(Status.STARTED, Step.CORE_RELEASE_APPROVE, StepStatus.STARTED, 123L));
    }

}
