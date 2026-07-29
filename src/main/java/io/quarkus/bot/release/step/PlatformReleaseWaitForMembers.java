package io.quarkus.bot.release.step;

import java.io.IOException;

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
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class PlatformReleaseWaitForMembers implements StepHandler {

    private static final String PLATFORM_MODE = "platformMode";
    private static final String AUTO = "auto";

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        if (!AUTO.equals(releaseStatus.getProperty(PLATFORM_MODE))) {
            return true;
        }
        return !releaseInformation.requiresPlatformMemberWaitingPeriod();
    }

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        String platformPreparationBranch = Branches.getPlatformPreparationBranch(releaseInformation);

        StringBuilder comment = new StringBuilder();

        if (releaseInformation.isDot0()) {
            comment.append(
                    "* Send an email to the Platform coordination mailing list: [quarkus-platform-coordination@googlegroups.com](mailto:quarkus-platform-coordination@googlegroups.com) :\n\n");
            comment.append("Subject:\n");
            comment.append("```\n");
            comment.append("Quarkus " + releaseInformation.getFullVersion() + " core artifacts are available\n");
            comment.append("```\n");
            comment.append("Body:\n");
            comment.append("```\n");
            comment.append("Hi,\n"
                    + "\n"
                    + "The Quarkus " + releaseInformation.getFullVersion() + " core artifacts are available on Maven Central.\n"
                    + "\n"
                    + "The pull request updating the Platform to Quarkus " + releaseInformation.getFullVersion()
                    + " has been merged in the " + platformPreparationBranch + " branch.\n"
                    + "We pinged in the pull request the teams maintaining components not passing the tests.\n"
                    + "\n"
                    + "If you want to update your components, please create your pull requests targeting the "
                    + platformPreparationBranch + " branch and make sure they are merged before next Tuesday.\n");
            if (!releaseInformation.isOriginBranchDefault()) {
                comment.append(
                        "\nMake sure you mention in the description that your pull request should be be backported to the "
                                + releaseInformation.getBranch() + " branch as " + releaseInformation.getBranch()
                                + " has already been branched, given it is a LTS.\n");
            }
            comment.append("\n"
                    + "Thanks.\n"
                    + "\n"
                    + "--\n"
                    + "The Quarkus dev team\n");
            comment.append("```\n\n");
            comment.append(
                    "* If CI failed for some Platform members, please contact them so that they are aware of the issues\n\n");
        } else {
            comment.append(
                    "* Send an email to the Platform coordination mailing list: [quarkus-platform-coordination@googlegroups.com](mailto:quarkus-platform-coordination@googlegroups.com) :\n\n");
            comment.append("Subject:\n");
            comment.append("```\n");
            comment.append("Quarkus " + releaseInformation.getFullVersion() + " core artifacts are available\n");
            comment.append("```\n");
            comment.append("Body:\n");
            comment.append("```\n");
            comment.append("Hi,\n"
                    + "\n"
                    + "The Quarkus " + releaseInformation.getFullVersion() + " core artifacts are available on Maven Central.\n"
                    + "\n"
                    + "The pull request updating the Platform to Quarkus " + releaseInformation.getFullVersion()
                    + " has been merged in the " + platformPreparationBranch + " branch.\n"
                    + "We pinged in the pull request the teams maintaining components not passing the tests.\n"
                    + "\n"
                    + "If you need to update your components to fix compatibility issues with this new micro (and only for this reason!), please create your pull requests targeting the "
                    + platformPreparationBranch + " branch and make sure they are merged before next Monday.\n");
            comment.append("\n"
                    + "Thanks.\n"
                    + "\n"
                    + "--\n"
                    + "The Quarkus dev team\n");
            comment.append("```\n\n");
            comment.append(
                    "* If CI failed for some Platform members, please contact them so that they are aware of the issues\n\n");
        }

        comment.append(Admonitions.warning(
                "**IMPORTANT - STOP HERE**\n**IMPORTANT - Wait a week before continuing with the Platform release**")
                + "\n\n");

        comment.append(
                "* Make sure you have merged [all the pull requests](https://github.com/quarkusio/quarkus-platform/pulls) that should be included in this version of the Platform before continuing\n\n");

        comment.append(Admonitions.important("**Once the week has passed and you are ready to continue, add a `"
                + Command.CONTINUE.getFullCommand() + "` comment.**") + "\n\n");

        if (releaseInformation.isDot0()) {
            comment.append("---\n\n");
            comment.append(
                    "<details><summary>If you have to release " + Versions.getDot1(releaseInformation.getVersion())
                            + " right away as the first Platform release</summary>\n\n");
            comment.append("This can happen if, for instance, an important regression is detected just after the `"
                    + releaseInformation.getVersion() + "` core release and before the Platform release.\n");
            comment.append("It might also happen if you want to fix a CVE before releasing the Platform.\n\n");
            comment.append("In this case, just close this release issue and start a new release for the `"
                    + Versions.getDot1(releaseInformation.getVersion()) + "` release as usual.\n");
            comment.append("The instructions will be automatically adapted.");
            comment.append("</details>\n\n");
        }

        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));

        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());

        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return Command.CONTINUE.matches(issueComment.getBody());
    }

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        return StepResult.success();
    }
}
