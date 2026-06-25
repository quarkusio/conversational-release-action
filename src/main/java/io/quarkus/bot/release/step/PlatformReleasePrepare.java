package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHPullRequest;
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
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class PlatformReleasePrepare implements StepHandler {

    private static final String PLATFORM_MODE = "platformMode";
    private static final String PLATFORM_PR_NUMBER = "platformPrNumber";
    private static final String AUTO = "auto";

    private String createdPrNumber;

    @Inject
    Processes processes;

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        String platformPreparationBranch = Branches.getPlatformPreparationBranch(releaseInformation);

        StringBuilder comment = new StringBuilder();

        comment.append(":raised_hands: Now is time to update Quarkus in the Quarkus Platform.\n\n");

        if (releaseInformation.isDot0()) {
            comment.append(Admonitions.warning(
                    "**This is the `.0` release so we update the Platform first then wait one week for the Platform members to contribute their updates then we release. Make sure you follow the instructions closely.**")
                    + "\n\n");
        }

        if (releaseInformation.isLtsMaintenanceReleaseWithRegularReleaseCadence() && !releaseInformation.isEmergency()) {
            comment.append(Admonitions.warning(
                    "**This is a maintenance release for a LTS version with regular release cadence so we update the Platform first then wait one week for the Platform members to potentially contribute compatibility fixes then we release. Make sure you follow the instructions closely.**")
                    + "\n\n");
        }

        if (releaseInformation.isEmergency() && releaseInformation.getEmergencyReleasePlatformBranch() != null) {
            comment.append(Admonitions.warning(
                    "The Platform will be built from the emergency branch `"
                            + releaseInformation.getEmergencyReleasePlatformBranch() + "`.")
                    + "\n\n");
        }

        if (!releaseInformation.isFinal() && releaseInformation.isOriginBranchMain()) {
            comment.append(Admonitions.tip(
                    "In the case of `preview releases` (e.g. `Alpha1`, `CR1`...), the release will be built from the `main` branch")
                    + "\n\n");
        }

        comment.append(":raised_hands: You have two options:\n\n");
        comment.append("- Let the release process handle things automatically: it will create a pull request on the Platform "
                + "targeting the `" + platformPreparationBranch + "` branch, monitor CI, and merge it\n");
        comment.append("- Perform all the operations manually\n\n");

        comment.append(Admonitions.important("**To let the release process handle things automatically for you, simply add a `"
                + Command.AUTO.getFullCommand() + "` comment**."));
        comment.append("\n\n");

        comment.append("---\n\n");
        comment.append("<details><summary>How to perform the operations manually?</summary>\n\n");
        comment.append("If you choose to do things manually, add a `" + Command.MANUAL.getFullCommand()
                + "` comment and follow the instructions that will be provided.\n\n");
        comment.append("</details>\n\n");

        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));

        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());

        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return Command.AUTO.matches(issueComment.getBody());
    }

    @Override
    public boolean shouldSkipAfterPause(Context context, Commands commands,
            GitHub quarkusBotGitHub, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue,
            GHIssueComment issueComment) {
        return Command.MANUAL.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {

        String platformPreparationBranch = Branches.getPlatformPreparationBranch(releaseInformation);

        List<String> scriptCommand = new ArrayList<>();
        scriptCommand.add("./release-platform-prepare.sh");
        scriptCommand.add(platformPreparationBranch);
        if (!releaseInformation.isOriginBranchMain()) {
            scriptCommand.add(releaseInformation.getOriginBranch());
        }

        int exitCode = processes.execute(scriptCommand);
        if (exitCode != 0) {
            return exitCode;
        }

        GHPullRequest pr = Repositories.getQuarkusPlatformRepository(quarkusBotGitHub)
                .createPullRequest(
                        "Upgrade to Quarkus " + releaseInformation.getVersion(),
                        "quarkus-" + releaseInformation.getVersion(),
                        platformPreparationBranch,
                        "Upgrade to Quarkus " + releaseInformation.getVersion());

        createdPrNumber = String.valueOf(pr.getNumber());

        issue.comment(":white_check_mark: Pull request [#" + pr.getNumber()
                + "](https://github.com/quarkusio/quarkus-platform/pull/" + pr.getNumber()
                + ") has been created to upgrade the Platform to Quarkus " + releaseInformation.getVersion() + ".\n\n"
                + Progress.youAreHere(releaseInformation, releaseStatus));

        return 0;
    }

    @Override
    public Map<String, String> getUpdatedProperties(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        if (createdPrNumber != null) {
            return Map.of(PLATFORM_MODE, AUTO, PLATFORM_PR_NUMBER, createdPrNumber);
        }
        return Map.of();
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "There was an issue creating the Platform pull request.\n"
                + "Please check the workflow run logs for details.\n"
                + "You can retry or switch to manual mode.";
    }
}
