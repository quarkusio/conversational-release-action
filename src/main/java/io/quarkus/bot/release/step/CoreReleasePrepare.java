package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class CoreReleasePrepare implements StepHandler {

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, GHIssue issue,
            UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        throw new IllegalStateException("Testing error handling...");
    }
}
