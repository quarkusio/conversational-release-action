package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Jdks;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class ApproveCoreRelease implements StepHandler {

    @Inject
    Jdks jdks;

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub gitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        StringBuilder comment = new StringBuilder();
        comment.append("We are going to release the following release:\n\n");
        comment.append("- Quarkus `").append(releaseInformation.getVersion()).append("`\n");
        comment.append("- On branch `").append(releaseInformation.getBranch()).append("`\n");
        comment.append("- With Java `").append(jdks.getJdkVersion(releaseInformation.getBranch())).append("`\n");
        if (releaseInformation.isMaintenance()) {
            comment.append("- This is a `maintenance` release.\n");
        }
        if (!releaseInformation.isFinal()) {
            comment.append("- This is a `preview` release (e.g. `Alpha`, `Beta`, `CR`).\n");
        }

        comment.append(
                "\nPlease approve with a `" + Command.YES.getFullCommand() + "` comment if you want to continue with the release.\n");
        comment.append("\nIf not, simply close this issue.\n\n");
        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));
        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());
        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            GitHub gitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.YES.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, GitHub gitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Core release is approved, proceeding...");
        return 0;
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "Failure at this stage usually means that something is wrong with this version.\n"
                + "It might be a code issue or a javadoc issue.\n\n"
                + "If so, you don't have much choice as close this release, adjust the code and start a whole new release.";
    }
}
