package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class CoreReleasePublish implements StepHandler {

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {

        return processes.execute(List.of("./release-core-publish.sh"));
    }

    @Override
    public void afterSuccess(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus currentReleaseStatus, GHIssue issue) throws IOException, InterruptedException {
        issue.comment("""
                :white_check_mark: The Core artifacts have been published to Central Portal.

                We will now wait for them to get synced to Maven Central.

                The operations will continue automatically once we have detected all the artifacts have been synced.
                """);
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "Please check the workflow run logs but there is a good chance "
                + "that the issue was due to a problem accessing [Central Portal](https://central.sonatype.com/publishing/deployments) "
                + "when uploading the artifacts.\n"
                + "If so, please retry.\n\n"
                + "Status page for Central Portal: https://status.maven.org/.";
    }
}
