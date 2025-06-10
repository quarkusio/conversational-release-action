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
public class CoreReleasePrepare implements StepHandler {

    static String DURATION = "2 hours";

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        return processes.execute(List.of("./release-core-prepare.sh"));
    }

    @Override
    public String getContinueFromStepHelp(ReleaseInformation releaseInformation) {
        StringBuilder help = new StringBuilder();
        help.append("The Core release steps take approximately ").append(DURATION).append(" so don't panic if it takes time.\n");
        help.append("You will receive feedback in this very issue if an error occurs or when further input is needed.");
        return help.toString();
    }
}
