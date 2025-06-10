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
import io.quarkus.bot.release.util.Admonitions;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Jdks;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class CoreReleaseApprove implements StepHandler {

    @Inject
    Jdks jdks;

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        StringBuilder comment = new StringBuilder();
        comment.append(":raised_hands: We are going to release the following release:\n\n");
        comment.append("- Quarkus `").append(releaseInformation.getVersion()).append("`\n");
        comment.append("- On branch `").append(releaseInformation.getBranch()).append("`");
        if (releaseInformation.isFirstCR()) {
            comment.append(" (it will get created a bit further in the process)");
        }
        comment.append("\n");
        comment.append("- With Java `").append(jdks.getJdkVersion(releaseInformation.getBranch())).append("`\n");
        if (releaseInformation.isFirstFinal() && !releaseInformation.isDot0()) {
            comment.append("- :bulb: We detected that this `" + releaseInformation.getVersion() + "` release will be the first final as `"
                    + Versions.getDot0(releaseInformation.getVersion()) + "` has not been fully released\n");
        }
        if (Branches.isLts(releaseInformation.getBranch())) {
            comment.append("- This is a `LTS` release.\n");
        }
        if (releaseInformation.isMaintenance()) {
            comment.append("- This is a `maintenance` release.\n");
        }
        if (releaseInformation.isEmergency()) {
            comment.append("- This is an `emergency` release.\n");
        }
        if (!releaseInformation.isFinal()) {
            comment.append("- This is a `preview` release (e.g. `Alpha`, `Beta`, `CR`).\n");
        }

        if (!releaseInformation.isOriginBranchMain()) {
            comment.append("\n");
            comment.append(Admonitions.warning("This release will be branched from " + releaseInformation.getOriginBranch() + ".\n" +
                    "You may release from an existing branch only when preparing a new LTS release.") + "\n");
        }

        String mergeInfo;
        if (releaseInformation.isFirstCR()) {
            mergeInfo = "**Make sure you have merged all the required [pull requests](https://github.com/quarkusio/quarkus/pulls) in the ["
                    + releaseInformation.getOriginBranch() + "](https://github.com/quarkusio/quarkus/commits/"
                    + releaseInformation.getOriginBranch() + "/) branch.**";
        } else {
            mergeInfo = "**Make sure you have merged all the [backport pull requests](https://github.com/quarkusio/quarkus/pulls?q=is%3Apr+base%3A"
                    + releaseInformation.getBranch() + "+is%3Aopen) in the ["
                    + releaseInformation.getBranch() + "](https://github.com/quarkusio/quarkus/commits/"
                    + releaseInformation.getBranch() + "/) branch.**";
        }

        comment.append("\n");
        comment.append(Admonitions.important(mergeInfo + "\n\n"
                + "Please approve with a `" + Command.YES.getFullCommand()
                + "` comment if you want to continue with the release.\n" +
                "\n" +
                "If not, simply close this issue.") + "\n\n");

        if (releaseInformation.isFirstCR()) {
            comment.append(Admonitions.tip("Don't go too far, we will need further input from you very soon.") + "\n\n");
        }

        comment.append("---\n\n<details><summary>Legend for the admonitions</summary>\n\n");
        comment.append(Admonitions.important("A manual confirmation/intervention is needed.") + "\n\n");
        comment.append(Admonitions.tip("Some useful information about the release process.") + "\n\n");
        comment.append(Admonitions.note("Some additional information.") + "\n\n");
        comment.append(Admonitions.warning("Something important to consider.") + "\n\n");
        comment.append(Admonitions.caution("An error occurred.") + "\n\n");
        comment.append("</details>\n\n");


        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));
        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());
        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.YES.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        StringBuilder comment = new StringBuilder(":white_check_mark: Core release is approved, proceeding...\n\n");

        if (releaseInformation.isFirstCR()) {
            // we will need input soon so let's make it clear
            comment.append(Admonitions.tip("Don't go too far, we will need some input from you very soon."));
        } else {
            comment.append(Admonitions
                    .tip("The Core release steps take approximately " + CoreReleasePrepare.DURATION + " so don't panic if it takes time.\n" +
                            "You will receive feedback in this very issue when further input is needed or if an error occurs."));
        }

        issue.comment(comment.toString());

        return 0;
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "Failure at this stage usually means that something is wrong with this version.\n"
                + "It might be a code issue or a javadoc issue.\n\n"
                + "If so, you don't have much choice as close this release, adjust the code and start a whole new release.";
    }
}
