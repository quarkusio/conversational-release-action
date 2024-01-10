package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.MonitorArtifactPublicationInputKeys;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class SyncPlatformRelease implements StepHandler {

    @Override
    public boolean shouldPause(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        StringBuilder comment = new StringBuilder();

        comment.append("The Platform artifacts have been pushed to `s01.oss.sonatype.org`.\n\n");
        comment.append(
                "**IMPORTANT** You need to wait for them to be synced to Maven Central before continuing with the release:\n\n");

        try {
            Map<String, Object> inputs = new HashMap<>();
            inputs.put(MonitorArtifactPublicationInputKeys.GROUP_ID, "io.quarkus.platform");
            inputs.put(MonitorArtifactPublicationInputKeys.ARTIFACT_ID, "quarkus-bom");
            inputs.put(MonitorArtifactPublicationInputKeys.VERSION, releaseInformation.getVersion());
            inputs.put(MonitorArtifactPublicationInputKeys.ISSUE_NUMBER, String.valueOf(issue.getNumber()));
            inputs.put(MonitorArtifactPublicationInputKeys.MESSAGE_IF_PUBLISHED,
                    Command.CONTINUE.getFullCommand()
                    + "\n\nWe have detected that the Platform artifacts have been synced to Maven Central.");
            inputs.put(MonitorArtifactPublicationInputKeys.MESSAGE_IF_NOT_PUBLISHED,
                    "The Platform artifacts don't seem to have been synced to Maven Central.\n\n"
                    + "Please check the situation by yourself:\n\n"
                    + "* Check that https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/"
                    + releaseInformation.getVersion() + "/"
                    + " does not return a 404\n\n"
                    + "* Wait some more if it still returns a 404.\n"
                    + "Once the artifact is available, wait for an additional 10 minutes then you can continue with the release by adding a `"
                    + Command.CONTINUE.getFullCommand() + "` comment.");
            inputs.put(MonitorArtifactPublicationInputKeys.INITIAL_DELAY, "20");
            inputs.put(MonitorArtifactPublicationInputKeys.POLL_ITERATIONS, "3");
            inputs.put(MonitorArtifactPublicationInputKeys.POLL_DELAY, "10");
            inputs.put(MonitorArtifactPublicationInputKeys.POST_DELAY, "10");

            issue.getRepository().getWorkflow("monitor-artifact-publication.yml").dispatch(Branches.MAIN, inputs);
            comment.append("The publication of the Platform artifacts will take 20-40 minutes.\n\n"
                    + "**We started a separate workflow to monitor the situation for you. It will automatically continue the release process once it detects the artifacts have been synced to Maven Central.**\n\n");

            comment.append("---\n\n");
            comment.append("If things go south, you can monitor the situation manually:\n\n");
            comment.append("* Wait for 40 minutes (starting from the time of this comment)\n");
            comment.append("* Check that https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/"
                    + releaseInformation.getVersion() + "/"
                    + " does not return a 404\n\n");
            comment.append(
                    "Once these two conditions are met, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.");
        } catch (Exception e) {
            comment.append("We were unable to start the Platform artifacts monitoring workflow, so please monitor the situation manually:\n\n");
            comment.append("* Wait for 40 minutes (starting from the time of this comment)\n");
            comment.append("* Check that https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/"
                    + releaseInformation.getVersion() + "/"
                    + " does not return a 404\n\n");
            comment.append(
                    "Once these two conditions are met, you can continue with the release by adding a `"
                            + Command.CONTINUE.getFullCommand() + "` comment.");
        }

        comment.append("\n\n" + Progress.youAreHere(releaseInformation, releaseStatus));

        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());
        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.CONTINUE.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus,
            GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Platform artifacts have been synced to Maven Central, continuing...");
        return 0;
    }
}
