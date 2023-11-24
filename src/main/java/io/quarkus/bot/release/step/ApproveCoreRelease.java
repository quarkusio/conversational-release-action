package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Command;

@Singleton
@Unremovable
public class ApproveCoreRelease implements StepHandler {

    @Override
    public boolean shouldPause(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return true;
    }

    @Override
    public boolean shouldContinue(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssueComment issueComment) {
        return Command.YES.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, GHIssue issue) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Core release is approved, proceeding...");
        return 0;
    }
}
