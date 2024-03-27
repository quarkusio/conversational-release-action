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
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.UpdatedIssueBody;

@Singleton
@Unremovable
public class UpdateQuickstarts implements StepHandler {

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        if (releaseInformation.isCR()) {
            if (releaseInformation.isFirstCR()) {
                // first CR is specific, we want to create the branch and get the new developments from the development branch
                return processes.execute(List.of("./update-quickstarts.sh", releaseInformation.getBranch(), "development"));
            } else {
                return processes.execute(
                        List.of("./update-quickstarts.sh", releaseInformation.getBranch(), releaseInformation.getBranch()));
            }
        } else if (releaseInformation.isFinal()) {
            if (releaseInformation.isFirstFinal()) {
                return processes.execute(List.of("./update-quickstarts.sh", Branches.MAIN, releaseInformation.getBranch()));
            } else {
                String branch = releaseInformation.isMaintenance() ? releaseInformation.getBranch() : Branches.MAIN;

                return processes.execute(List.of("./update-quickstarts.sh", branch, branch));
            }
        }

        throw new IllegalStateException(
                "UpdateQuickstarts may only be executed for CR and Final versions but got: " + releaseInformation.getVersion());
    }

    @Override
    public String getErrorHelp(ReleaseInformation releaseInformation) {
        if (releaseInformation.isFirstFinal()) {
            return "It might be due to the build failing with the current " + releaseInformation.getBranch()
                    + " branch of Quarkus.\n\n"
                    + "You need to check the content of the `development` branch of https://github.com/quarkusio/quarkus-quickstarts/.\n\n"
                    + "If you fix the issue there, you can safely retry.";
        } else {
            return "For micro versions, we shouldn't have code updates in the Quickstarts.\n\n"
                    + "So, if you got the build failing, either something got wrongly merged in the `main` branch of https://github.com/quarkusio/quarkus-quickstarts/ or something in Quarkus broke the quickstarts.";
        }
    }

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return !releaseInformation.isCR() && !releaseInformation.isFinal();
    }
}
