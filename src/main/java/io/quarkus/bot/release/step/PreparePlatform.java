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
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class PreparePlatform implements StepHandler {

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        String platformPreparationBranch = Branches.getPlatformPreparationBranch(releaseInformation);
        String platformReleaseBranch = Branches.getPlatformReleaseBranch(releaseInformation);

        StringBuilder comment = new StringBuilder();

        comment.append(":raised_hands: Now is time to update Quarkus in the Quarkus Platform. This is a manual process.\n\n");
        if (releaseInformation.isDot0()) {
            comment.append(Admonitions.warning("**This is the `.0` release so we update the Platform first then wait one week for the Platform members to contribute their updates then we release. Make sure you follow the instructions closely.**") + "\n\n");
        }

        if (releaseInformation.isLtsMaintenanceReleaseWithRegularReleaseCadence()) {
            comment.append(Admonitions.warning(
                    "**This is a maintenance release for a LTS version with regular release cadence so we update the Platform first then wait one week for the Platform members to potentially contribute compatibility fixes then we release. Make sure you follow the instructions closely.**")
                    + "\n\n");
        }

        if (!releaseInformation.isFinal() && releaseInformation.isOriginBranchMain()) {
            comment.append(Admonitions.tip("In the case of `preview releases` (e.g. `Alpha1`, `CR1`...), the release will be built from the `main` branch") + "\n\n");
        }

        comment.append(Admonitions.important("First, you need to update the Platform locally, create a pull request, wait for CI and merge it.\n\n" +
                "You can find detailed instructions below.") + "\n\n");

        comment.append("* Go into your Quarkus Platform clone directory:\n\n");
        comment.append("```\n");
        comment.append("cd <your quarkus-platform clone>\n");
        comment.append("```\n");
        comment.append("* Follow (roughly) these steps (`upstream` is the upstream repository, `origin` is your fork):\n\n");
        comment.append("```\n");
        if (releaseInformation.isOriginBranchMain()) {
            comment.append("git checkout " + platformPreparationBranch + "\n");
            comment.append("git pull upstream " + platformPreparationBranch + "\n");
        } else {
            comment.append("git checkout " + releaseInformation.getOriginBranch() + "\n");
            comment.append("git pull upstream " + releaseInformation.getOriginBranch() + "\n");
            comment.append("git checkout -b " + releaseInformation.getBranch()+ "\n");
            comment.append("git push upstream " + releaseInformation.getBranch()+ "\n");
        }
        comment.append("git checkout -b quarkus-" + releaseInformation.getVersion() + "\n");
        comment.append("./update-quarkus-version.sh " + releaseInformation.getVersion() + "\n");
        comment.append("```\n\n");
        comment.append("* Check the diff with `git diff`\n");
        comment.append("* Then:\n\n");
        comment.append("```\n");
        comment.append("git add .\n");
        comment.append("git commit -m 'Upgrade to Quarkus " + releaseInformation.getVersion() + "'\n");
        comment.append("git push origin quarkus-" + releaseInformation.getVersion() + "\n");
        comment.append("```\n\n");
        try {
            comment.append("* [Create a pull request](https://github.com/quarkusio/quarkus-platform/compare/" + platformPreparationBranch + "..." + issue.getUser().getLogin() + ":quarkus-" + releaseInformation.getVersion() + "?expand=1) targeting branch `" + platformPreparationBranch + "`"
                    + " (or [generic link if targeted link doesn't work](https://github.com/quarkusio/quarkus-platform/pulls))\n");
        } catch (IOException e) {
            comment.append("* [Create a pull request](https://github.com/quarkusio/quarkus-platform/pulls) targeting branch `" + platformPreparationBranch + "`\n");
        }
        comment.append("* Wait for CI to go green\n");
        comment.append("* Merge the pull request\n");
        if (releaseInformation.isDot0()) {
            comment.append("* Send an email to the Platform coordination mailing list: [quarkus-platform-coordination@googlegroups.com](mailto:quarkus-platform-coordination@googlegroups.com) :\n\n");
            comment.append("Subject:\n");
            comment.append("```\n");
            comment.append("Quarkus " + releaseInformation.getVersion() + " core artifacts are available\n");
            comment.append("```\n");
            comment.append("Body:\n");
            comment.append("```\n");
            comment.append("Hi,\n"
                    + "\n"
                    + "The Quarkus " + releaseInformation.getVersion() + " core artifacts are available on Maven Central.\n"
                    + "\n"
                    + "The pull request updating the Platform to Quarkus " + releaseInformation.getVersion() + " has been merged in the main branch.\n"
                    + "We pinged in the pull request the teams maintaining components not passing the tests.\n"
                    + "\n"
                    + "If you want to update your components, please create your pull requests targeting the main branch and make sure they are merged before next Tuesday.\n");
            if (!releaseInformation.isOriginBranchMain()) {
                comment.append("\nMake sure you mention in the description that your pull request should be be backported to the "
                        + releaseInformation.getBranch() + " branch as " + releaseInformation.getBranch()
                        + " has already been branched, given it is a LTS.\n");
            }
            comment.append("\n"
                    + "Thanks.\n"
                    + "\n"
                    + "--\n"
                    + "The Quarkus dev team\n"
                    );
            comment.append("```\n\n");
            comment.append("* If CI failed for some Platform members, please contact them so that they are aware of the issues\n\n");
            comment.append(Admonitions.warning("**IMPORTANT - STOP HERE**\n**IMPORTANT - Wait a week before continuing with the Platform release**") + "\n\n");
        }
        if (releaseInformation.isLtsMaintenanceReleaseWithRegularReleaseCadence()) {
            comment.append("* Send an email to the Platform coordination mailing list: [quarkus-platform-coordination@googlegroups.com](mailto:quarkus-platform-coordination@googlegroups.com) :\n\n");
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
                    + "The pull request updating the Platform to Quarkus " + releaseInformation.getFullVersion() + " has been merged in the main branch.\n"
                    + "We pinged in the pull request the teams maintaining components not passing the tests.\n"
                    + "\n"
                    + "If you need to update your components to fix compatibility issues with this new micro (and only for this reason!), please create your pull requests targeting the " + releaseInformation.getBranch() + " branch and make sure they are merged before next Monday.\n");
            comment.append("\n"
                    + "Thanks.\n"
                    + "\n"
                    + "--\n"
                    + "The Quarkus dev team\n"
                    );
            comment.append("```\n\n");
            comment.append("* If CI failed for some Platform members, please contact them so that they are aware of the issues\n\n");
            comment.append(Admonitions.warning("**IMPORTANT - STOP HERE**\n**IMPORTANT - Wait a week before continuing with the Platform release**") + "\n\n");
        }
        if ((releaseInformation.isDot0() || releaseInformation.isFirstFinal()) && !platformBranchExists(quarkusBotGitHub, platformReleaseBranch)) {
            comment.append("* Make sure you have merged [all the pull requests](https://github.com/quarkusio/quarkus-platform/pulls) that should be included in this version of the Platform\n");
            comment.append("* Once all the pull requests are merged, create the branch:\n\n");
            comment.append("```\n");
            comment.append("git checkout main\n");
            comment.append("git pull upstream main\n");
            comment.append("git checkout -b " + platformReleaseBranch + "\n");
            comment.append("git push upstream " + platformReleaseBranch + "\n");
            comment.append("```\n\n");
            comment.append(
                    Admonitions.important("**Once everything has been pushed to branch `" + platformReleaseBranch + "`, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.**") + "\n\n");
        } else {
            comment.append("* Make sure you have merged [all the pull requests](https://github.com/quarkusio/quarkus-platform/pulls) that should be included in this version of the Platform\n\n");
            comment.append(
                    Admonitions.important("**Once everything has been merged to branch `" + platformReleaseBranch + "`, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.**") + "\n\n");
        }

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

    private static boolean platformBranchExists(GitHub quarkusBotGitHub, String branch) {
        try {
            Repositories.getQuarkusPlatformRepository(quarkusBotGitHub).getBranch(branch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.CONTINUE.matches(issueComment.getBody());
    }


    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: The Platform branch `" + Branches.getPlatformReleaseBranch(releaseInformation)
                + "` is ready to be released, continuing...");
        return 0;
    }
}
