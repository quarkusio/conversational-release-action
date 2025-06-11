package io.quarkus.bot.release.step;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;

public enum Step {

    PREREQUISITES("Prerequisites", Prerequisites.class, false, false),
    CORE_RELEASE_APPROVE("Approve the Core release", CoreReleaseApprove.class, true, false),
    CORE_RELEASE_CREATE_BRANCH("Create branch", CoreReleaseCreateBranch.class, true, false),
    CORE_RELEASE_PREPARE("Prepare the Core release", CoreReleasePrepare.class, true, false),
    CORE_RELEASE_DEPLOY_LOCALLY("Deploy the Core release locally", CoreReleaseDeployLocally.class, true, false),
    CORE_RELEASE_VALIDATE("Validate the Core release artifacts", CoreReleaseValidate.class, true, false),
    CORE_RELEASE_PUBLISH("Publish the Core release to Central Portal", CoreReleasePublish.class, true, false),
    CORE_RELEASE_WAIT_FOR_SYNC("Wait for Core release sync to Central Portal", CoreReleaseWaitForSync.class, true, false),
    CORE_RELEASE_GRADLE_PLUGIN("Release Gradle plugin", CoreReleaseGradlePlugin.class, true, false),
    POST_CORE_RELEASE("Execute post-Core-release operations", PostCoreRelease.class, true, false),
    //    TRIGGER_PERFORMANCE_LAB("Trigger performance lab", TriggerPerformanceLab.class, true, false),
    PLATFORM_RELEASE_PREPARE("Prepare the Quarkus Platform", PlatformReleasePrepare.class, true, false),
    PLATFORM_RELEASE_PUBLISH("Publish the Quarkus Platform", PlatformReleasePublish.class, true, false),
    PLATFORM_RELEASE_WAIT_FOR_SYNC("Sync Platform release to Maven Central", PlatformReleaseWaitForSync.class, true, false),
    UPDATE_EXTENSION_CATALOG("Update extension catalog", UpdateExtensionCatalog.class, true, false),
    POST_PLATFORM_RELEASE("Execute post-Platform-release operations", PostPlatformRelease.class, true, false),
    UPDATE_JBANG_CATALOG("Update JBang catalog", UpdateJBangCatalog.class, true, true),
    PUBLISH_CLI("Publish CLI", PublishCLI.class, true, true),
    UPDATE_QUICKSTARTS("Update quickstarts", UpdateQuickstarts.class, true, false),
    UPDATE_QUICKSTARTS_ADDITIONAL_SYNC_LTS("Update quickstarts - Additional sync to version branch",
            UpdateQuickstartsAdditionalSyncVersionBranch.class,
            true, true),
    UPDATE_DOCUMENTATION("Update documentation", UpdateDocumentation.class, true, true),
    UPDATE_DOCUMENTATION_ADDITIONAL_SYNC_LTS("Update documentation - Additional sync for LTS",
            UpdateDocumentationAdditionalSyncLts.class, true, true),
    ANNOUNCE_RELEASE("Announce release", AnnounceRelease.class, true, false);

    private final String description;

    private final Class<? extends StepHandler> stepHandler;

    private final boolean recoverable;

    private final boolean forFinalReleasesOnly;

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
        return this.ordinal() == values().length - 1;
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
