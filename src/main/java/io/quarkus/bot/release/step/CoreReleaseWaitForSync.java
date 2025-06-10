package io.quarkus.bot.release.step;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.List;

@Singleton
@Unremovable
public class CoreReleaseWaitForSync implements StepHandler {

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {

        return processes.execute(List.of("./release-core-wait-for-sync.sh"));
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "The sync to [Central Portal](https://central.sonatype.com/publishing/deployments) "
                + "took longer than expected.\n\n"
                + "Please have a look to the workflow logs to better understand what is going on.\n\n"
                + "You can retry to give it another chance.\n\n"
                + "Status page for Central Portal: https://status.maven.org/.";
    }
}
