package io.quarkus.bot.release;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPermissionType;
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
import io.quarkus.bot.release.step.ApproveCoreRelease;
import io.quarkus.bot.release.step.CoreReleasePrepare;
import io.quarkus.bot.release.step.Prerequisites;
import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepHandler;
import io.quarkus.bot.release.step.StepStatus;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Processes;

public class ReleaseAction {

    private static final Logger LOG = Logger.getLogger(ReleaseAction.class);

    @Inject
    Issues issues;

    @Inject
    Processes processes;

    // ugly workaround...
    @Inject
    Prerequisites prerequisites;
    @Inject
    ApproveCoreRelease approveCoreRelease;
    @Inject
    CoreReleasePrepare coreReleasePrepare;

    @Action
    void startRelease(Context context, Commands commands, @Issue.Opened GHEventPayload.Issue issuePayload) throws Exception {
        GHIssue issue = issuePayload.getIssue();

        if (System.getenv("RELEASE_GITHUB_TOKEN") == null || System.getenv("RELEASE_GITHUB_TOKEN").isBlank()) {
            throw new IllegalStateException("No RELEASE_GITHUB_TOKEN around");
        }

        if (!issuePayload.getRepository().hasPermission(issuePayload.getSender(), GHPermissionType.WRITE)) {
            react(commands, issue, ReactionContent.MINUS_ONE);
            issue.comment(":rotating_light: You don't have the permission to start a release.");
            issue.close();
            return;
        }

        ReleaseInformation releaseInformation;

        try {
            releaseInformation= issues
                    .extractReleaseInformationFromForm(issuePayload.getIssue().getBody());
        } catch (Exception e) {
            LOG.error("Unable to extract release information from the body of the issue for issue: #"
                    + issue.getNumber() + " " + issue.getTitle());
            issue.comment(":rotating_light: Unable to extract release information from the issue description.\nWe can't release\nClosing the issue.");
            issue.close();
            throw e;
        }

        react(commands, issue, ReactionContent.PLUS_ONE);

        handleSteps(context, commands, issuePayload.getIssue(), null, releaseInformation, new ReleaseStatus(Status.STARTED, Step.PREREQUISITES, StepStatus.STARTED, context.getGitHubRunId()));
    }

    @Action
    void onComment(Context context, Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) throws Exception {
        GHIssueComment issueComment = issueCommentPayload.getComment();
        GHIssue issue = issueCommentPayload.getIssue();

        if (issueCommentPayload.getSender().getLogin().endsWith("-bot")
                || issueCommentPayload.getSender().getLogin().endsWith("[bot]")) {
            return;
        }

        if (!issueCommentPayload.getRepository().hasPermission(issueCommentPayload.getSender(), GHPermissionType.WRITE)) {
            react(commands, issueComment, ReactionContent.MINUS_ONE);
            return;
        }

        ReleaseInformation releaseInformation;
        ReleaseStatus releaseStatus;
        try {
            releaseInformation = issues.extractReleaseInformation(issue.getBody());
            releaseStatus = issues.extractReleaseStatus(issue.getBody());
        } catch (Exception e) {
            issue.comment(":rotating_light: Unable to extract release information and/or release status from the issue description.\nWe can't release\nClosing the issue.");
            issue.close();
            throw e;
        }

        handleSteps(context, commands, issue, issueComment, releaseInformation, releaseStatus);
    }

    private void handleSteps(Context context, Commands commands, GHIssue issue, GHIssueComment issueComment, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) throws Exception {
        int initialStepOrdinal = releaseStatus.getCurrentStep().ordinal();
        if (releaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED) {
            initialStepOrdinal++;
        }
        if (initialStepOrdinal >= Step.values().length) {
            return;
        }

        ReleaseStatus currentReleaseStatus = releaseStatus;

        if (issueComment != null) {
            // Handle retries
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.FAILED ||
                    currentReleaseStatus.getCurrentStepStatus() == StepStatus.STARTED) {
                if (currentReleaseStatus.getCurrentStep().isRecoverable()) {
                    if (Command.RETRY.matches(issueComment.getBody())) {
                        react(commands, issueComment, ReactionContent.PLUS_ONE);
                        currentReleaseStatus = currentReleaseStatus.progress(StepStatus.STARTED);
                        updateReleaseStatus(issue, currentReleaseStatus);
                    } else {
                        react(commands, issueComment, ReactionContent.CONFUSED);
                        return;
                    }
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    fatalError(context, commands, releaseInformation, currentReleaseStatus, issue,
                            "A previous step failed with unrecoverable error");
                    return;
                }
            }

            // Handle paused, we will continue the process with the next step
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.PAUSED) {
                StepHandler stepHandler = getStepHandler(currentReleaseStatus.getCurrentStep());

                if (stepHandler.shouldContinue(releaseInformation, currentReleaseStatus, issueComment)) {
                    react(commands, issueComment, ReactionContent.PLUS_ONE);
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.COMPLETED);
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    return;
                }
            }
        }

        progressInformation(context, commands, releaseInformation, currentReleaseStatus, issue,
                "Proceeding to step " + Step.values()[initialStepOrdinal].getDescription());

        for (Step currentStep : Step.values()) {
            if (currentStep.ordinal() < initialStepOrdinal) {
                // we already handled this step, skipping to next one
                continue;
            }
            if (currentStep.isForFinalReleasesOnly() && !releaseInformation.isFinal()) {
                // we skip steps restricted to final releases if the release is not final
                continue;
            }

            commands.notice("Running step " + currentStep.getDescription());

            try {
                StepHandler stepHandler = getStepHandler(currentStep);

                currentReleaseStatus = currentReleaseStatus.progress(currentStep);
                updateReleaseStatus(issue, currentReleaseStatus);

                if (stepHandler.shouldPause(releaseInformation, releaseStatus)) {
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.PAUSED);
                    updateReleaseStatus(issue, currentReleaseStatus);
                    return;
                }

                int exitCode = stepHandler.run(releaseInformation, issue);
                handleExitCode(exitCode, currentStep);

                currentReleaseStatus = currentReleaseStatus.progress(StepStatus.COMPLETED);
                updateReleaseStatus(issue, currentReleaseStatus);
            } catch (StatusUpdateException e) {
                fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, e.getMessage());
                throw e;
            } catch (Exception e) {
                if (currentStep.isRecoverable()) {
                    progressError(context, commands, releaseInformation, currentReleaseStatus, issue, e.getMessage());
                    throw e;
                } else {
                    fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, e.getMessage());
                    throw e;
                }
            }
        }

        currentReleaseStatus = currentReleaseStatus.progress(Status.COMPLETED);
        updateReleaseStatus(issue, currentReleaseStatus);

        try {
            issue.comment(":white_check_mark: " + releaseInformation.getVersion() + " was successfully released.\n\nTime to write the announcement.");
            issue.close();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to mark the release as successful", e);
        }
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

    private void updateReleaseStatus(GHIssue issue, ReleaseStatus updatedReleaseStatus) {
        try {
            issue.setBody(issues.appendReleaseStatus(issue.getBody(), updatedReleaseStatus));
        } catch (Exception e) {
            throw new StatusUpdateException("Unable to update the release status to: " + updatedReleaseStatus, e);
        }
    }

    private static void react(Commands commands, Reactable reactable, ReactionContent reactionContent) {
        try {
            reactable.createReaction(reactionContent);
        } catch (IOException e) {
            commands.error("Unable to react with: " + reactionContent);
        }
    }

    private static void progressInformation(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, String progress) {
        try {
            issue.comment(":gear: " + progress + "\n\nYou can follow the progress of the workflow [here](" + getWorkflowRunUrl(context)
                    + ")." + youAreHere(releaseInformation, releaseStatus));
        } catch (IOException e) {
            commands.warning("Unable to add progress comment: " + progress);
        }
    }

    private void progressError(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, String error) {
        try {
            ReleaseStatus currentReleaseStatus = releaseStatus.progress(StepStatus.FAILED);
            issue.setBody(issues.appendReleaseStatus(issue.getBody(), currentReleaseStatus));
            issue.comment(":rotating_light: " + error + "\n\nYou can find more information about the failure [here](" + getWorkflowRunUrl(context) + ").\n\n"
                    + "This is not a fatal error, you can retry by adding a `retry` comment."
                    + youAreHere(releaseInformation, currentReleaseStatus));
        } catch (IOException e) {
            throw new RuntimeException("Unable to add progress error comment or close the comment: " + error, e);
        }
    }

    private void fatalError(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, String error) {
        try {
            ReleaseStatus currentReleaseStatus = releaseStatus.progress(Status.FAILED, StepStatus.FAILED);
            issue.setBody(issues.appendReleaseStatus(issue.getBody(), currentReleaseStatus));
            issue.comment(":rotating_light: " + error + "\n\nYou can find more information about the failure [here](" + getWorkflowRunUrl(context) + ").\n\n"
                    + "This is a fatal error, the issue will be closed."
                    + youAreHere(releaseInformation, currentReleaseStatus));
            issue.close();
        } catch (IOException e) {
            throw new RuntimeException("Unable to add fatal error comment or close the comment: " + error, e);
        }
    }

    private static String getWorkflowRunUrl(Context context) {
        return context.getGitHubServerUrl() + "/" + context.getGitHubRepository() + "/actions/runs/" + context.getGitHubRunId();
    }

    private static String youAreHere(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return "\n\n<details><summary>You are here</summary>\n\n" +
                Arrays.stream(Step.values())
                    .filter(s -> releaseInformation.isFinal() || !s.isForFinalReleasesOnly())
                    .map(s -> {
                        StringBuilder sb = new StringBuilder();
                        sb.append("[");
                        if (releaseStatus.getCurrentStep().ordinal() > s.ordinal() ||
                                (releaseStatus.getCurrentStep() == s && releaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED)) {
                            sb.append("X");
                        } else {
                            sb.append(" ");
                        }
                        sb.append("] ").append(s.getDescription());

                        if (releaseStatus.getCurrentStep() == s) {
                            if (releaseStatus.getCurrentStepStatus() == StepStatus.STARTED) {
                                sb.append(" :gear:");
                            }
                            if (releaseStatus.getCurrentStepStatus() == StepStatus.FAILED) {
                                sb.append(" :rotating_light:");
                            }
                            sb.append(" ☚ You are here");
                        }
                        return sb.toString();
                    }).collect(Collectors.joining("\n- ", "- ", "")) + "</details>";
    }
}
