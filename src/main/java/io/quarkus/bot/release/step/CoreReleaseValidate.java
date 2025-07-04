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
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class CoreReleaseValidate implements StepHandler {

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {

        return processes.execute(List.of("./release-core-validate.sh"));
    }

    @Override
    public void afterSuccess(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue) throws IOException, InterruptedException {
        issue.comment("""
                :white_check_mark: The Core artifacts have been deployed locally and validated.

                We will now publish them to Central Portal.


                """ + Progress.youAreHere(releaseInformation, releaseStatus));
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        return "Some artifacts are not compatible with Maven Central publication. " +
                "Possible causes are that some are missing a sources or javadoc jar, or that some are missing an artifact name. "
                +
                "Please refer to the workflow log to figure out what the problem is exactly.\n\n" +
                "Unfortunately, this is not recoverable and you will need to manually drop the `"
                + releaseInformation.getVersion()
                + "` tag from [the main repository](https://github.com/quarkusio/quarkus/tags) once you are completely sure this is a validation error.\n\n"
                +
                "Once the tag is dropped, fix the issues, backport them to the `" + releaseInformation.getBranch()
                + "` branch and create a new issue to restart the release process entirely.";
    }
}
