package io.quarkus.bot.release;

import java.io.IOException;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.Reactable;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.bot.release.error.StatusUpdateException;
import io.quarkus.bot.release.error.StepExecutionException;
import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepHandler;
import io.quarkus.bot.release.step.StepStatus;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Users;

public class ReleaseAction {

    private static final Logger LOG = Logger.getLogger(ReleaseAction.class);

    @Inject
    Issues issues;

    @Inject
    Processes processes;

    @Action
    void startRelease(Context context, Commands commands, @Issue.Opened GHEventPayload.Issue issuePayload) throws Exception {
        commands.notice("Starting release...");

        GHIssue issue = issuePayload.getIssue();
        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issue.getBody());

        checkReleaseGitHubTokenIsPresent();

        if (!issuePayload.getRepository().hasPermission(issuePayload.getSender(), GHPermissionType.WRITE)
                || Users.isIgnored(issuePayload.getSender().getLogin())) {
            react(commands, issue, ReactionContent.MINUS_ONE);
            commands.error("User " + issuePayload.getSender() + " doesn't have the appropriate permission");
            issue.comment(":rotating_light: You don't have the permission to start a release.");
            issue.close();
            return;
        }

        ReleaseInformation releaseInformation;

        try {
            releaseInformation = issues.extractReleaseInformationFromForm(updatedIssueBody.getBody());
            issue.setBody(issues.appendReleaseInformation(updatedIssueBody, releaseInformation));
        } catch (Exception e) {
            LOG.error("Unable to extract release information from the body of the issue for issue: #"
                    + issue.getNumber() + " " + issue.getTitle());
            issue.comment(
                    ":rotating_light: Unable to extract release information from the issue description.\nWe can't release\nClosing the issue.");
            issue.close();
            throw e;
        }

        react(commands, issue, ReactionContent.PLUS_ONE);

        try {
            handleSteps(context, commands, getQuarkusBotGitHub(), issuePayload.getIssue(), updatedIssueBody, null, releaseInformation,
                    new ReleaseStatus(Status.STARTED, Step.PREREQUISITES, StepStatus.STARTED, context.getGitHubRunId()));
        } finally {
            if (releaseInformation.getVersion() != null) {
                commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
            }
        }
    }

    @Action
    void continueRelease(Context context, Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload)
            throws Exception {
        commands.notice("Continuing release...");

        GHIssueComment issueComment = issueCommentPayload.getComment();
        GHIssue issue = issueCommentPayload.getIssue();
        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issue.getBody());

        checkReleaseGitHubTokenIsPresent();

        if (Users.isIgnored(issueCommentPayload.getSender().getLogin())) {
            return;
        }

        if (issue.getState() == GHIssueState.CLOSED) {
            commands.error("The issue is closed. No further action is allowed.");
            react(commands, issueComment, ReactionContent.MINUS_ONE);
            return;
        }

        if (!issueCommentPayload.getRepository().hasPermission(issueCommentPayload.getSender(), GHPermissionType.WRITE)) {
            commands.error("User " + issueCommentPayload.getSender() + " doesn't have the appropriate permission");
            react(commands, issueComment, ReactionContent.MINUS_ONE);
            return;
        }

        ReleaseInformation releaseInformation;
        ReleaseStatus releaseStatus;
        try {
            releaseInformation = issues.extractReleaseInformation(updatedIssueBody);
            releaseStatus = issues.extractReleaseStatus(updatedIssueBody);
        } catch (Exception e) {
            issue.comment(
                    ":rotating_light: Unable to extract release information and/or release status from the issue description.\nWe can't release\nClosing the issue.");
            issue.close();
            throw e;
        }

        try {
            releaseStatus = releaseStatus.progress(context.getGitHubRunId());
            updateReleaseStatus(issue, updatedIssueBody, releaseStatus);
            handleSteps(context, commands, getQuarkusBotGitHub(), issue, updatedIssueBody, issueComment, releaseInformation, releaseStatus);
        } finally {
            if (releaseInformation.getVersion() != null) {
                commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
            }
        }
    }

    private static GitHub getQuarkusBotGitHub() throws IOException {
        checkReleaseGitHubTokenIsPresent();

        return GitHub.connectUsingOAuth(System.getenv("RELEASE_GITHUB_TOKEN"));
    }

    private static void checkReleaseGitHubTokenIsPresent() {
        if (System.getenv("RELEASE_GITHUB_TOKEN") == null || System.getenv("RELEASE_GITHUB_TOKEN").isBlank()) {
            throw new IllegalStateException("No RELEASE_GITHUB_TOKEN around");
        }
    }

    private void handleSteps(Context context, Commands commands, GitHub quarkusBotGitHub, GHIssue issue, UpdatedIssueBody updatedIssueBody,
            GHIssueComment issueComment, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) throws Exception {
        ReleaseStatus currentReleaseStatus = releaseStatus;

        if (issueComment != null) {
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.INIT ||
                    currentReleaseStatus.getCurrentStepStatus() == StepStatus.STARTED) {
                commands.error("Current step is running, ignoring comment");
                react(commands, issueComment, ReactionContent.MINUS_ONE);
                return;
            }
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.INIT_FAILED) {
                // Handle retries
                // we can always retry in case of failure at the init stage
                if (Command.RETRY.matches(issueComment.getBody())) {
                    react(commands, issueComment, ReactionContent.PLUS_ONE);
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.INIT);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    return;
                }
            } else if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.FAILED) {
                // Handle retries
                if (currentReleaseStatus.getCurrentStep().isRecoverable()) {
                    if (Command.RETRY.matches(issueComment.getBody())) {
                        react(commands, issueComment, ReactionContent.PLUS_ONE);
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.STARTED);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                    } else {
                        react(commands, issueComment, ReactionContent.CONFUSED);
                        return;
                    }
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, updatedIssueBody,
                            "A previous step failed with an unrecoverable error");
                    return;
                }
            } else if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.PAUSED) {
                // Handle paused, we will continue the process with the next step
                StepHandler stepHandler = getStepHandler(currentReleaseStatus.getCurrentStep());

                if (stepHandler.shouldContinueAfterPause(context, commands, quarkusBotGitHub, releaseInformation, currentReleaseStatus, issue, issueComment)) {
                    react(commands, issueComment, ReactionContent.PLUS_ONE);
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.STARTED);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                } else if (stepHandler.shouldSkipAfterPause(context, commands, quarkusBotGitHub, releaseInformation, currentReleaseStatus, issue, issueComment)) {
                    react(commands, issueComment, ReactionContent.PLUS_ONE);
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.SKIPPED);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    return;
                }
            }
        }

        if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED ||
                currentReleaseStatus.getCurrentStepStatus() == StepStatus.SKIPPED) {
            if (currentReleaseStatus.getCurrentStep().isLast()) {
                markAsReleased(issue, updatedIssueBody, releaseInformation, currentReleaseStatus);
                return;
            } else {
                currentReleaseStatus = currentReleaseStatus.progress(currentReleaseStatus.getCurrentStep().next());
                updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
            }
        }

        progressInformation(context, commands, releaseInformation, currentReleaseStatus, issue,
                "Proceeding to step " + currentReleaseStatus.getCurrentStep().getDescription());

        StepHandler currentStepHandler;

        for (Step currentStep : Step.values()) {
            if (currentStep.ordinal() < currentReleaseStatus.getCurrentStep().ordinal()) {
                // we already handled this step, skipping to next one
                continue;
            }
            if (currentStep.isForFinalReleasesOnly() && !releaseInformation.isFinal()) {
                // we skip steps restricted to final releases if the release is not final
                continue;
            }

            commands.notice("Running step " + currentStep.getDescription());

            currentStepHandler = getStepHandler(currentStep);

            try {
                if (currentReleaseStatus.getCurrentStep() != currentStep) {
                    currentReleaseStatus = currentReleaseStatus.progress(currentStep);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                } else {
                    if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.INIT_FAILED) {
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.INIT);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                    } else if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.FAILED) {
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.STARTED);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                    }
                }

                if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.INIT) {
                    if (currentStepHandler.shouldSkip(context, commands, quarkusBotGitHub, releaseInformation, currentReleaseStatus, issue, issueComment)) {
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.SKIPPED);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                        continue;
                    } else if (currentStepHandler.shouldPause(context, commands, quarkusBotGitHub, releaseInformation, releaseStatus, issue, issueComment)) {
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.PAUSED);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                        return;
                    } else {
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.STARTED);
                        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                    }
                }

                int exitCode = currentStepHandler.run(context, commands, quarkusBotGitHub, releaseInformation, releaseStatus, issue, updatedIssueBody);
                handleExitCode(exitCode, currentStep);

                currentReleaseStatus = currentReleaseStatus.progress(StepStatus.COMPLETED);
                updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
            } catch (StatusUpdateException e) {
                fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, updatedIssueBody,
                        e.getMessage());
                throw e;
            } catch (Exception e) {
                if (currentStep.isRecoverable()) {
                    progressError(context, commands, releaseInformation, currentReleaseStatus, issue, updatedIssueBody,
                            e.getMessage(), currentStepHandler.getErrorHelp(releaseInformation));
                    throw e;
                } else {
                    fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, updatedIssueBody,
                            e.getMessage(), currentStepHandler.getErrorHelp(releaseInformation));
                    throw e;
                }
            }
        }

        markAsReleased(issue, updatedIssueBody, releaseInformation, currentReleaseStatus);
    }

    private void markAsReleased(GHIssue issue, UpdatedIssueBody updatedIssueBody, ReleaseInformation releaseInformation,
            ReleaseStatus currentReleaseStatus) {
        currentReleaseStatus = currentReleaseStatus.progress(Status.COMPLETED);
        updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
    }

    private static StepHandler getStepHandler(Step step) {
        InstanceHandle<? extends StepHandler> instanceHandle = Arc.container().instance(step.getStepHandler());

        if (!instanceHandle.isAvailable()) {
            throw new IllegalStateException("Couldn't find an appropriate StepHandler for " + step.name());
        }

        return instanceHandle.get();
    }

    private static void handleExitCode(int exitCode, Step step) {
        if (exitCode != 0) {
            throw new StepExecutionException("An error occurred while executing step `" + step.getDescription() + "`.");
        }
    }

    private void updateReleaseStatus(GHIssue issue, UpdatedIssueBody updatedIssueBody, ReleaseStatus updatedReleaseStatus) {
        try {
            issue.setBody(issues.appendReleaseStatus(updatedIssueBody, updatedReleaseStatus));
        } catch (Exception e) {
            throw new StatusUpdateException(
                    "Unable to update the release status to " + updatedReleaseStatus + ": " + e.getMessage(), e);
        }
    }

    private static void react(Commands commands, final Reactable reactable, ReactionContent reactionContent) {
        try {
            reactable.listReactions().toList().stream()
                    .filter(r -> Users.isIgnored(r.getUser().getLogin()))
                    .filter(r -> r.getContent() == ReactionContent.EYES)
                    .forEach(r -> {
                        try {
                            reactable.deleteReaction(r);
                        } catch (Exception e) {
                            // ignore
                        }
                    });

            reactable.createReaction(reactionContent);
        } catch (IOException e) {
            commands.error("Unable to react with: " + reactionContent);
        }
    }

    private static void progressInformation(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, String progress) {
        try {
            issue.comment(":gear: " + progress + "\n\nYou can follow the progress of the workflow [here]("
                    + getWorkflowRunUrl(context)
                    + ").\n\n" + Progress.youAreHere(releaseInformation, releaseStatus));
        } catch (IOException e) {
            commands.warning("Unable to add progress comment: " + progress);
        }
    }

    private void progressError(Context context, Commands commands, ReleaseInformation releaseInformation,
            final ReleaseStatus currentReleaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error,
            String errorHelp) {
        ReleaseStatus updatedReleaseStatus;
        switch(currentReleaseStatus.getCurrentStepStatus()) {
            case INIT:
                updatedReleaseStatus = currentReleaseStatus.progress(StepStatus.INIT_FAILED);
                break;
            case SKIPPED:
                updatedReleaseStatus = currentReleaseStatus.progress(StepStatus.SKIPPED);
                break;
            default:
                updatedReleaseStatus = currentReleaseStatus.progress(StepStatus.FAILED);
                break;
        }

        try {
            retry(3, () -> issue.setBody(issues.appendReleaseStatus(updatedIssueBody, updatedReleaseStatus)));

            commands.setOutput(Outputs.INTERACTION_COMMENT, ":rotating_light: " + error
                    + (errorHelp != null && !errorHelp.isBlank() ? "\n\n" + errorHelp : "")
                    + "\n\nYou can find more information about the failure [here](" + getWorkflowRunUrl(context) + ").\n\n"
                    + "This is not a fatal error, you can retry by adding a `" + Command.RETRY.getFullCommand() + "` comment.\n\n"
                    + Progress.youAreHere(releaseInformation, currentReleaseStatus));
        } catch (Exception e) {
            commands.setOutput(Outputs.INTERACTION_COMMENT, ":rotating_light: We were unable to report the error to the issue status.\n\n"
                    + "The issue is in an inconsistent state, better ping @gsmet to figure out how to continue with the process.\n\n"
                    + Progress.youAreHere(releaseInformation, updatedReleaseStatus));

            throw new IllegalStateException("Unable to update the status or add progress error comment: " + error, e);
        }
    }

    private void fatalError(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error) {
        fatalError(context, commands, releaseInformation, releaseStatus, issue, updatedIssueBody, error, null);
    }

    private void fatalError(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error, String errorHelp) {
        final ReleaseStatus updatedReleaseStatus = releaseStatus.progress(Status.FAILED, StepStatus.FAILED);

        try {
            retry(3, () -> issue.setBody(issues.appendReleaseStatus(updatedIssueBody, updatedReleaseStatus)));
            retry(3, () -> issue.comment(":rotating_light: " + error
                    + (errorHelp != null && !errorHelp.isBlank() ? "\n\n" + errorHelp : "")
                    + "\n\nYou can find more information about the failure [here]("
                    + getWorkflowRunUrl(context) + ").\n\n"
                    + "This is a fatal error, the issue will be closed.\n\n"
                    + Progress.youAreHere(releaseInformation, releaseStatus)));
            retry(3, () -> issue.close());
        } catch (Exception e) {
            commands.setOutput(Outputs.INTERACTION_COMMENT,
                    ":rotating_light: We were unable to report the fatal error to the issue status.\n\n"
                            + "The issue should be closed and no further interaction should be made with this issue.\n\n"
                            + Progress.youAreHere(releaseInformation, updatedReleaseStatus));

            throw new RuntimeException(
                    "Unable to add fatal error comment or close the issue: " + error + " (because of " + e.getMessage() + ")",
                    e);
        }
    }

    private static String getWorkflowRunUrl(Context context) {
        return context.getGitHubServerUrl() + "/" + context.getGitHubRepository() + "/actions/runs/" + context.getGitHubRunId();
    }

    /**
     * This should be used with a lot of precautions, only to make sure critical API calls are successful.
     */
    private static void retry(int iterations, ThrowingRunnable runnable) throws Exception {
        Exception originalException = null;
        for (int i = 1; i <= iterations; i++) {
            try {
                runnable.run();
            } catch (Exception e) {
                if (originalException == null) {
                    originalException = e;
                } else {
                    originalException.addSuppressed(e);
                }
                if (i == iterations) {
                    throw originalException;
                } else {
                    // otherwise wait a bit and iterate
                    Thread.sleep(i * 3000);
                }
            }
        }
    }

    @FunctionalInterface
    public interface ThrowingRunnable {

        void run() throws Exception;
    }
}
