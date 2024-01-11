package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class CoreReleasePerform implements StepHandler {

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, ReleaseStatus releaseStatus,
            GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {

        return processes.execute(List.of("./release-perform.sh"));
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "Please check the workflow run logs but there is a good chance "
                + "that the issue was due to a problem with accessing `s01.oss.sonatype.org` "
                + "either when authenticating or when uploading the artifacts.\n"
                + "If so, please retry.\n\n"
                + "Status page for `s01.oss.sonatype.org`: https://status.maven.org/.";
    }
}
