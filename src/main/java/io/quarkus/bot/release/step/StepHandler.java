package io.quarkus.bot.release.step;

import java.io.IOException;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.UpdatedIssueBody;

public interface StepHandler {

    int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException;

    /**
     * This method is executed if run() has been successfully executed and is executed after the step status has been marked to COMPLETED.
     * <p>
     * It may be used to add a comment indicating the progress of step.
     */
    default void afterSuccess(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus currentReleaseStatus, GHIssue issue) throws IOException, InterruptedException {
    }

    default boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return false;
    }

    default boolean shouldContinueAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return false;
    }

    default boolean shouldSkipAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return false;
    }

    default boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return false;
    }

    /**
     * Provide some contextual help when an error occurs.
     * <p>
     * The help message is included into the comment reporting the error.
     * It will be prepended by a :bulb: emoji.
     */
    default String getErrorHelp(ReleaseInformation releaseInformation) {
        return null;
    }

    /**
     * Provide some contextual help when we restart from a specific step.
     * <p>
     * The help message is included into the comment indicating we are pursuing the release.
     * It will be prepended by a :bulb: emoji.
     */
    default String getContinueFromStepHelp(ReleaseInformation releaseInformation) {
        return null;
    }
}
