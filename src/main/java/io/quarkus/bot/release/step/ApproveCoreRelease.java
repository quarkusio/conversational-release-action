package io.quarkus.bot.release.step;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

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
    public boolean shouldPause(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        StringBuilder comment = new StringBuilder();
        comment.append("We are going to release the following release:\n\n");
        comment.append("- Quarkus `").append(releaseInformation.getVersion()).append("`\n");
        comment.append("- On branch `").append(releaseInformation.getBranch()).append("`\n");
        comment.append("- With Java `").append(jdks.getJdkVersion(releaseInformation.getBranch())).append("`\n");
        if (Files.exists(Path.of("work", "maintenance"))) {
            comment.append("- This is a `maintenance` release.\n");
        }
        if (Files.exists(Path.of("work", "preview"))) {
            comment.append("- This is a `preview` release (e.g. `Alpha`, `Beta`, `CR`).\n");
        }

        if (releaseInformation.isFirstCR()) {
            comment.append("\n\n");
            comment.append(
                    "**IMPORTANT** This is the first Candidate Release. Make sure the following tasks have been done:\n\n");
            comment.append(
                    "- Create the `" + releaseInformation.getBranch() + "` branch and push it to the upstream repository\n");
            comment.append("- Rename the `" + releaseInformation.getBranch()
                    + " - main` milestone [here](https://github.com/quarkusio/quarkus/milestones)\n");
            comment.append(
                    "- Create a new milestone `X.Y - main` milestone [here](https://github.com/quarkusio/quarkus/milestones/new) with `X.Y` being the next major/minor version name. Make sure you follow the name convention, it is important.\n");
            comment.append("- Create a `triage/backport-" + releaseInformation.getBranch()
                    + "?` label and make sure [all the current pull requests with the `triage/backport?` label](https://github.com/quarkusio/quarkus/pulls?q=is%3Apr+label%3Atriage%2Fbackport%3F+) also have the `triage/backport-"
                    + releaseInformation.getBranch()
                    + "?` label (in the UI, you can select all the pull requests with the top checkbox then use the `Label` dropdown to apply the new `triage/backport-"
                    + releaseInformation.getBranch() + "?` label)\n");
            comment.append("- Send an email to `quarkus-dev` announcing that `" + releaseInformation.getBranch() + "` has been branched:\n\n");
            comment.append("Subject: `Quarkus " + releaseInformation.getBranch() + " branched`\n\n");
            comment.append("> Hi,\n"
                    + "> \n"
                    + "> We just branched " + releaseInformation.getBranch() + ". The main branch is now **X.Y**.\n"
                    + "> \n"
                    + "> Please make sure you add the appropriate backport labels from now on:\n"
                    + "- for anything required in " + releaseInformation.getBranch() + " (currently open pull requests included), please add the triage/backport? label\n"
                    + "- for fixes we also want in future 3.5, please add the triage/backport-3.5? label\n"
                    + "- for fixes we also want in future 3.2, please add the triage/backport-3.2? label\n"
                    + "- for fixes we also want in future 2.13, please add the triage/backport-2.13? label\n"
                    + "\n"
                    + "Thanks!\n"
                    + "\n"
                    + "--\n"
                    + "The Quarkus dev team");
            comment.append("\n\n");
        }

        comment.append(
                "\nPlease approve with a `" + Command.YES.getFullCommand() + "` comment if you want to continue with the release.\n");
        comment.append("\nIf not, simply close this issue.");
        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));
        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());
        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.YES.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus,
            GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Core release is approved, proceeding...");
        return 0;
    }

    @Override
    public String getErrorHelp() {
        return "Failure at this stage usually means that something is wrong with this version.\n"
                + "It might be a code issue or a javadoc issue.\n\n"
                + "If so, you don't have much choice as close this release, adjust the code and start a whole new release.";
    }
}
