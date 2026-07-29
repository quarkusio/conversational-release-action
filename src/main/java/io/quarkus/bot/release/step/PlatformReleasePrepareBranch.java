package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class PlatformReleasePrepareBranch implements StepHandler {

    private static final String PLATFORM_MODE = "platformMode";
    private static final String AUTO = "auto";

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        if (!AUTO.equals(releaseStatus.getProperty(PLATFORM_MODE))) {
            return true;
        }
        return !releaseInformation.isFirstFinal();
    }

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {

        String platformReleaseBranch = Branches.getPlatformReleaseBranch(releaseInformation);

        if ((releaseInformation.isFirstFinal() || releaseInformation.isDot0())
                && !platformBranchExists(quarkusBotGitHub, platformReleaseBranch)) {
            GHRepository platformRepo = Repositories.getQuarkusPlatformRepository(quarkusBotGitHub);
            String sha = platformRepo.getBranch(Branches.MAIN).getSHA1();
            platformRepo.createRef("refs/heads/" + platformReleaseBranch, sha);
        }

        issue.comment(":white_check_mark: The Platform branch `" + platformReleaseBranch
                + "` is ready to be released, continuing...\n\n" + Progress.youAreHere(releaseInformation, releaseStatus));

        return StepResult.success();
    }

    private static boolean platformBranchExists(GitHub quarkusBotGitHub, String branch) {
        try {
            Repositories.getQuarkusPlatformRepository(quarkusBotGitHub).getBranch(branch);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
