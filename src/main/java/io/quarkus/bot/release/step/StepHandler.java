package io.quarkus.bot.release.step;

import java.io.IOException;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;

public interface StepHandler {

    int run(Context context, Commands commands, ReleaseInformation releaseInformation, GHIssue issue) throws IOException, InterruptedException;

    default boolean shouldPause(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return false;
    }

    default boolean shouldContinue(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssueComment issueComment) {
        return false;
    }
}
