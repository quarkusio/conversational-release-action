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
        if (releaseInformation.getQualifier() != null) {
            command.add("--qualifier=" + releaseInformation.getQualifier());
        }
        if (releaseInformation.isMajor()) {
            command.add("--major");
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

        return exitCode;
    }
}
