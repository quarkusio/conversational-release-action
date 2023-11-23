package io.quarkus.bot.release.step;

import java.io.IOException;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Command;

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
    public int run(ReleaseInformation releaseInformation, GHIssue issue) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Core release is approved, proceeding...");
        return 0;
    }
}
