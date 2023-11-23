package io.quarkus.bot.release.step;

public enum Step {

    PREREQUISITES("Prerequisites", Prerequisites.class, false, false),
    APPROVE_CORE_RELEASE("Approve the core release", ApproveCoreRelease.class, true, false),
    CORE_RELEASE_PREPARE("Prepare the core release", CoreReleasePrepare.class, true, false);
//    CORE_RELEASE_PERFORM("Perform the core release", true, false),
//    SYNC_CORE_RELEASE_TO_MAVEN_CENTRAL("Sync core release to Maven Central", true, false),
//    RELEASE_GRADLE_PLUGIN("Release Gradle plugin", true, false),
//    POST_CORE_RELEASE("Execute post-core-release operations", true, false),
//    TRIGGER_PERFORMANCE_LAB("Trigger performance lab", true, false),
//    APPROVE_PLATFORM_RELEASE("Approve Quarkus Platform release", true, false),
//    RELEASE_PLATFORM("Release the Quarkus Platform", true, false),
//    SYNC_PLATFORM_RELEASE_TO_MAVEN_CENTRAL("Sync Quarkus Platform release to Maven Central", true, false),
//    RELEASE_EXTENSION_CATALOG("Release extension catalog", true, false),
//    POST_PLATFORM_RELEASE("Execute post-Platform-release operations", true, false),
//    UPDATE_JBANG_CATALOG("Update JBang catalog", true, true),
//    UPDATE_QUICKSTARTS("Update quickstarts", true, true),
//    UPDATE_DOCUMENTATION("Update documentation", true, true),
//    SYNC_CODE_QUARKUS_IO("Wait for code.quarkus.io to be updated", true, true);

    private final String description;

    private final Class<? extends StepHandler> stepHandler;

    private final boolean forFinalReleasesOnly;

    private final boolean recoverable;

    Step(String description, Class<? extends StepHandler> stepHandler, boolean recoverable, boolean forFinalReleasesOnly) {
        this.description = description;
        this.stepHandler = stepHandler;
        this.forFinalReleasesOnly = forFinalReleasesOnly;
        this.recoverable = recoverable;
    }

    public String getDescription() {
        return description;
    }

    public Class<? extends StepHandler> getStepHandler() {
        return stepHandler;
    }

    public boolean isRecoverable() {
        return recoverable;
    }

    public boolean isForFinalReleasesOnly() {
        return forFinalReleasesOnly;
    }
}
