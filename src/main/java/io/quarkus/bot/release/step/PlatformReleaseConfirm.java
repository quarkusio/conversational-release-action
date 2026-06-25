package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHRepository;
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
public class PlatformReleaseConfirm implements StepHandler {

    private static final String PLATFORM_MODE = "platformMode";
    private static final String AUTO = "auto";

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return !AUTO.equals(releaseStatus.getProperty(PLATFORM_MODE));
    }

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        String platformReleaseBranch = Branches.getPlatformReleaseBranch(releaseInformation);

        StringBuilder comment = new StringBuilder();

        comment.append(
                "* Make sure you have merged [all the pull requests](https://github.com/quarkusio/quarkus-platform/pulls) that should be included in this version of the Platform\n\n");

        if ((releaseInformation.isFirstFinal() || releaseInformation.isDot0())
                && !platformBranchExists(quarkusBotGitHub, platformReleaseBranch)) {
            comment.append(
                    Admonitions.important("**Once everything has been pushed to branch `" + platformReleaseBranch
                            + "`, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.**")
                            + "\n\n");
        } else {
            comment.append(
                    Admonitions.important("**Once everything has been merged to branch `" + platformReleaseBranch
                            + "`, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.**")
                            + "\n\n");
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
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return Command.CONTINUE.matches(issueComment.getBody());
    }

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {

        String platformReleaseBranch = Branches.getPlatformReleaseBranch(releaseInformation);

        if ((releaseInformation.isFirstFinal() || releaseInformation.isDot0())
                && AUTO.equals(releaseStatus.getProperty(PLATFORM_MODE))
                && !platformBranchExists(quarkusBotGitHub, platformReleaseBranch)) {
            GHRepository platformRepo = Repositories.getQuarkusPlatformRepository(quarkusBotGitHub);
            String sha = platformRepo.getBranch(Branches.MAIN).getSHA1();
            platformRepo.createRef("refs/heads/" + platformReleaseBranch, sha);
        }

        issue.comment(":white_check_mark: The Platform branch `" + platformReleaseBranch
                + "` is ready to be released, continuing...\n\n" + Progress.youAreHere(releaseInformation, releaseStatus));

        return StepResult.success();
    }
}
