package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import org.jboss.logging.Logger;
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
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class PlatformReleaseMonitorCI implements StepHandler {

    private static final Logger LOG = Logger.getLogger(PlatformReleaseMonitorCI.class);

    private static final String PLATFORM_MODE = "platformMode";
    private static final String PLATFORM_PR_NUMBER = "platformPrNumber";
    private static final String AUTO = "auto";

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return !AUTO.equals(releaseStatus.getProperty(PLATFORM_MODE));
    }

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        String prNumber = releaseStatus.getProperty(PLATFORM_PR_NUMBER);

        StringBuilder comment = new StringBuilder();

        comment.append(":hourglass: Monitoring CI for the Platform pull request.\n\n");

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put("pr-number", prNumber);
            inputs.put("issue-number", String.valueOf(issue.getNumber()));
            inputs.put("message-if-success",
                    Command.CONTINUE.getFullCommand()
                            + "\n\nWe have detected that the Platform CI has passed. Merging the pull request and continuing.");
            inputs.put("message-if-failure",
                    "The Platform CI has failed.\n\n"
                            + "Please check [the pull request](https://github.com/quarkusio/quarkus-platform/pull/"
                            + prNumber + ") for details.\n\n"
                            + "Once the issue is fixed and CI passes, add a `"
                            + Command.CONTINUE.getFullCommand() + "` comment to continue.");
            inputs.put("initial-delay", "120");
            inputs.put("poll-delay", "15");
            inputs.put("poll-iterations", "8");

            issue.getRepository().getWorkflow("monitor-platform-ci.yml").dispatch(Branches.MAIN, inputs);

            comment.append("**We started a separate workflow to monitor the CI status of "
                    + "[pull request #" + prNumber + "](https://github.com/quarkusio/quarkus-platform/pull/" + prNumber
                    + "). "
                    + "It will automatically continue the release process once CI passes.**\n\n");

            comment.append("---\n\n");
            comment.append("If things go south, you can check the pull request status manually and add a `"
                    + Command.CONTINUE.getFullCommand() + "` comment to continue.\n\n");
        } catch (Exception e) {
            LOG.error("Unable to start the CI monitoring workflow", e);
            comment.append(Admonitions.warning(
                    "We were unable to start the CI monitoring workflow: " + e.getMessage() + ". Please monitor CI manually.")
                    + "\n\n");
            comment.append("* Check [the pull request](https://github.com/quarkusio/quarkus-platform/pull/"
                    + prNumber + ") for CI status\n");
            comment.append("* Once CI passes and the pull request is merged, add a `"
                    + Command.CONTINUE.getFullCommand() + "` comment to continue.\n\n");
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
        String prNumber = releaseStatus.getProperty(PLATFORM_PR_NUMBER);

        if (prNumber != null) {
            GHPullRequest pr = Repositories.getQuarkusPlatformRepository(quarkusBotGitHub)
                    .getPullRequest(Integer.parseInt(prNumber));

            if (!pr.isMerged()) {
                pr.merge("Upgrade to Quarkus " + releaseInformation.getVersion(), null, GHPullRequest.MergeMethod.MERGE);
            }

            issue.comment(":white_check_mark: Platform pull request #" + prNumber + " has been merged.\n\n"
                    + "Waiting 15 minutes before continuing to let artifacts propagate...\n\n"
                    + Progress.youAreHere(releaseInformation, releaseStatus));

            Thread.sleep(15 * 60 * 1000L);
        }

        return StepResult.success();
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "There was an issue merging the Platform pull request or during the post-merge wait.\n"
                + "Please check the pull request status manually and retry.";
    }
}
