package io.quarkus.bot.release.step;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public enum Step {

    PREREQUISITES("Prerequisites", Prerequisites.class, false, false),
    APPROVE_CORE_RELEASE("Approve the core release", ApproveCoreRelease.class, true, false),
    CREATE_BRANCH("Create branch (if needed)", CreateBranch.class, true, false),
    CORE_RELEASE_PREPARE("Prepare the core release", CoreReleasePrepare.class, true, false),
    CORE_RELEASE_PERFORM("Perform the core release", CoreReleasePerform.class, true, false),
    SYNC_CORE_RELEASE_TO_MAVEN_CENTRAL("Sync core release to Maven Central", SyncCoreRelease.class, true, false),
    RELEASE_GRADLE_PLUGIN("Release Gradle plugin", ReleaseGradlePlugin.class, true, false),
    POST_CORE_RELEASE("Execute post-core-release operations", PostCoreRelease.class, true, false),
    //    TRIGGER_PERFORMANCE_LAB("Trigger performance lab", TriggerPerformanceLab.class, true, false),
    PREPARE_PLATFORM("Prepare the Quarkus Platform", PreparePlatform.class, true, false),
    RELEASE_PLATFORM("Release the Quarkus Platform", ReleasePlatform.class, true, false),
    SYNC_PLATFORM_RELEASE("Sync Platform release to Maven Central", SyncPlatformRelease.class, true, false),
    UPDATE_EXTENSION_CATALOG("Update extension catalog", UpdateExtensionCatalog.class, true, false),
    POST_PLATFORM_RELEASE("Execute post-Platform-release operations", PostPlatformRelease.class, true, false),
    UPDATE_JBANG_CATALOG("Update JBang catalog", UpdateJBangCatalog.class, true, true),
    PUBLISH_CLI("Publish CLI", PublishCLI.class, true, true),
    UPDATE_QUICKSTARTS("Update quickstarts", UpdateQuickstarts.class, false, true),
    UPDATE_QUICKSTARTS_ADDITIONAL_SYNC_LTS("Update quickstarts - Additional sync to version branch", UpdateQuickstartsAdditionalSyncVersionBranch.class,
            true, true),
    UPDATE_DOCUMENTATION("Update documentation", UpdateDocumentation.class, true, true),
    UPDATE_DOCUMENTATION_ADDITIONAL_SYNC_LTS("Update documentation - Additional sync for LTS",
            UpdateDocumentationAdditionalSyncLts.class, true, true),
    ANNOUNCE_RELEASE("Announce release", AnnounceRelease.class, true, false);

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

    public boolean isRecoverable() {
        return recoverable;
    }

    public boolean isForFinalReleasesOnly() {
        return forFinalReleasesOnly;
    }

    public boolean isLast() {
        return this.ordinal() == values().length -1;
    }

    public Step next() {
        if (isLast()) {
            throw new IllegalStateException("Called next() on the last step");
        }

        return Step.values()[this.ordinal() + 1];
    }

    public StepHandler getStepHandler() {
        InstanceHandle<? extends StepHandler> instanceHandle = Arc.container().instance(stepHandler);

        if (!instanceHandle.isAvailable()) {
            throw new IllegalStateException("Couldn't find an appropriate StepHandler for " + name());
        }

        return instanceHandle.get();
    }
}
