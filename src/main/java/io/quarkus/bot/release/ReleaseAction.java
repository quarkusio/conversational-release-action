package io.quarkus.bot.release;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
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
import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepHandler;
import io.quarkus.bot.release.step.StepStatus;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Processes;
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

        if (System.getenv("RELEASE_GITHUB_TOKEN") == null || System.getenv("RELEASE_GITHUB_TOKEN").isBlank()) {
            throw new IllegalStateException("No RELEASE_GITHUB_TOKEN around");
        }

        if (!issuePayload.getRepository().hasPermission(issuePayload.getSender(), GHPermissionType.WRITE)
                || Users.isIgnored(issuePayload.getSender().getLogin())) {
            react(commands, issue, ReactionContent.MINUS_ONE);
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
            handleSteps(context, commands, issuePayload.getIssue(), updatedIssueBody, null, releaseInformation,
                    new ReleaseStatus(Status.STARTED, Step.PREREQUISITES, StepStatus.STARTED, context.getGitHubRunId()));
        } finally {
            if (releaseInformation.getVersion() != null) {
                commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
            }
        }
    }

    @Action
    void onComment(Context context, Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload)
            throws Exception {
        commands.notice("Continuing release...");

        GHIssueComment issueComment = issueCommentPayload.getComment();
        GHIssue issue = issueCommentPayload.getIssue();
        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issue.getBody());

        if (Users.isIgnored(issueCommentPayload.getSender().getLogin())) {
            return;
        }

        if (!issueCommentPayload.getRepository().hasPermission(issueCommentPayload.getSender(), GHPermissionType.WRITE)
                || issue.getState() == GHIssueState.CLOSED) {
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
            handleSteps(context, commands, issue, updatedIssueBody, issueComment, releaseInformation, releaseStatus);
        } finally {
            if (releaseInformation.getVersion() != null) {
                commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
            }
        }
    }

    private void handleSteps(Context context, Commands commands, GHIssue issue, UpdatedIssueBody updatedIssueBody,
            GHIssueComment issueComment, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) throws Exception {
        ReleaseStatus currentReleaseStatus = releaseStatus;

        if (issueComment != null) {
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED) {
                commands.error("Current step status is completed, ignoring comment");
                react(commands, issueComment, ReactionContent.MINUS_ONE);
                return;
            }
            if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.FAILED ||
                    currentReleaseStatus.getCurrentStepStatus() == StepStatus.STARTED) {
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
                            "A previous step failed with unrecoverable error");
                    return;
                }
            } else if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.PAUSED) {
                // Handle paused, we will continue the process with the next step
                StepHandler stepHandler = getStepHandler(currentReleaseStatus.getCurrentStep());

                if (stepHandler.shouldContinue(context, commands, releaseInformation, currentReleaseStatus, issue, issueComment)) {
                    react(commands, issueComment, ReactionContent.PLUS_ONE);
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.COMPLETED);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                } else {
                    react(commands, issueComment, ReactionContent.CONFUSED);
                    return;
                }
            }
        }

        if (currentReleaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED) {
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

                currentReleaseStatus = currentReleaseStatus.progress(currentStep);
                updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);

                if (currentStepHandler.shouldPause(context, commands, releaseInformation, releaseStatus, issue, issueComment)) {
                    currentReleaseStatus = currentReleaseStatus.progress(StepStatus.PAUSED);
                    updateReleaseStatus(issue, updatedIssueBody, currentReleaseStatus);
                    return;
                }

                int exitCode = currentStepHandler.run(context, commands, releaseInformation, issue, updatedIssueBody);
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
                            e.getMessage(), currentStepHandler.getErrorHelp());
                    throw e;
                } else {
                    fatalError(context, commands, releaseInformation, currentReleaseStatus, issue, updatedIssueBody,
                            e.getMessage(), currentStepHandler.getErrorHelp());
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
            throw new StatusUpdateException("Unable to update the release status to: " + updatedReleaseStatus, e);
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
                    + ")." + youAreHere(releaseInformation, releaseStatus));
        } catch (IOException e) {
            commands.warning("Unable to add progress comment: " + progress);
        }
    }

    private void progressError(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error,
            String errorHelp) {
        try {
            ReleaseStatus currentReleaseStatus = releaseStatus.progress(StepStatus.FAILED);
            issue.setBody(issues.appendReleaseStatus(updatedIssueBody, currentReleaseStatus));
            commands.setOutput(Outputs.INTERACTION_COMMENT, ":rotating_light: " + error
                    + (errorHelp != null && !errorHelp.isBlank() ? "\n\n" + errorHelp : "")
                    + "\n\nYou can find more information about the failure [here](" + getWorkflowRunUrl(context) + ").\n\n"
                    + "This is not a fatal error, you can retry by adding a `" + Command.RETRY.getFullCommand() + "` comment."
                    + youAreHere(releaseInformation, currentReleaseStatus));
        } catch (IOException e) {
            throw new RuntimeException("Unable to add progress error comment or close the comment: " + error, e);
        }
    }

    private void fatalError(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error) {
        fatalError(context, commands, releaseInformation, releaseStatus, issue, updatedIssueBody, error, null);
    }

    private void fatalError(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody, String error, String errorHelp) {
        try {
            ReleaseStatus currentReleaseStatus = releaseStatus.progress(Status.FAILED, StepStatus.FAILED);
            issue.setBody(issues.appendReleaseStatus(updatedIssueBody, currentReleaseStatus));
            issue.comment(":rotating_light: " + error
                    + (errorHelp != null && !errorHelp.isBlank() ? "\n\n" + errorHelp : "")
                    + "\n\nYou can find more information about the failure [here]("
                    + getWorkflowRunUrl(context) + ").\n\n"
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
        return "\n\n<details><summary>Where am I?</summary>\n\n" +
                Arrays.stream(Step.values())
                        .filter(s -> releaseInformation.isFinal() || !s.isForFinalReleasesOnly())
                        .map(s -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            if (releaseStatus.getCurrentStep().ordinal() > s.ordinal() ||
                                    (releaseStatus.getCurrentStep() == s
                                            && releaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED)) {
                                sb.append("X");
                            } else {
                                sb.append(" ");
                            }
                            sb.append("] ").append(s.getDescription());

                            if (releaseStatus.getCurrentStep() == s) {
                                switch (releaseStatus.getCurrentStepStatus()) {
                                    case STARTED:
                                        sb.append(" :gear:");
                                        break;
                                    case FAILED:
                                        sb.append(" :rotating_light:");
                                        break;
                                    case PAUSED:
                                        sb.append(" :pause_button:");
                                        break;
                                    default:
                                        break;
                                }
                                sb.append(" ☚ You are here");
                            }
                            return sb.toString();
                        }).collect(Collectors.joining("\n- ", "- ", ""))
                + "</details>";
    }
}
