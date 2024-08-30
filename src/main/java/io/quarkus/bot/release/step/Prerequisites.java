package io.quarkus.bot.release.step;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
import io.quarkus.bot.release.error.StepExecutionException;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Processes;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class Prerequisites implements StepHandler {

    @Inject
    Issues issues;

    @Inject
    Processes processes;

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add("./prerequisites.java");
        command.add("--branch=" + releaseInformation.getBranch());
        command.add("--origin-branch=" + releaseInformation.getOriginBranch());
        if (releaseInformation.getQualifier() != null) {
            command.add("--qualifier=" + releaseInformation.getQualifier());
        }
        if (releaseInformation.isMajor()) {
            command.add("--major");
        }
        if (Branches.isLts(releaseInformation.getBranch())) {
            command.add("--lts");
        }

        int exitCode = processes.execute(command);
        if (exitCode != 0) {
            return exitCode;
        }

        String version = Files.readString(Path.of("work", "newVersion")).trim();

        boolean firstFinal = Versions.isDot0(version) ||
                (Versions.isFirstMicroMaintenanceRelease(version)
                        && Repositories.getQuarkusRepository(quarkusBotGitHub).getReleaseByTagName(Versions.getDot0(version)) == null);
        boolean maintenance = Files.exists(Path.of("work", "maintenance"));

        releaseInformation.setVersion(version, firstFinal, maintenance);

        issues.appendReleaseInformation(updatedIssueBody, releaseInformation);

        // for the first CR of an LTS branch, we need the origin branch to be not be main
        if (releaseInformation.isFirstCR() && Branches.isLts(releaseInformation.getBranch())) {
            if (releaseInformation.isOriginBranchMain()) {
                throw new StepExecutionException("Origin branch is set to `main` for the CR1 of a LTS release.", true,
                        "For the first CR of a LTS branch, we need the origin branch to be the branch of the previous minor as the LTS should be a direct continuation of the previous minor.");
            }
        } else if (!releaseInformation.isOriginBranchMain()) {
            throw new StepExecutionException("Origin branch may only be set when releasing the CR1 of a LTS release.", true);
        }

        return exitCode;
    }
}
