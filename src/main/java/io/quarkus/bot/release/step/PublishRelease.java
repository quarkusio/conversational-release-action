package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class PublishRelease implements StepHandler {

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        GHRepository repository = Repositories.getQuarkusRepository(quarkusBotGitHub);
        String version = releaseInformation.getVersion();

        publishRelease(commands, repository, version);

        if (releaseInformation.isFirstFinal() && !releaseInformation.isDot0()) {
            String dot0Version = Versions.getMinorVersion(version) + ".0";
            GHRelease dot0Release = repository.getReleaseByTagName(dot0Version);
            if (dot0Release != null && dot0Release.isDraft()) {
                publishRelease(commands, repository, dot0Version);
            }
        }

        return StepResult.success();
    }

    private static void publishRelease(Commands commands, GHRepository repository, String version) throws IOException {
        GHRelease release = repository.getReleaseByTagName(version);

        if (release == null) {
            throw new IllegalStateException(
                    "Release " + version + " not found, it should have been created by the post-Platform-release step");
        }

        if (!release.isDraft()) {
            commands.warning("Release " + version + " is already published, skipping");
            return;
        }

        release.update().draft(false).update();
    }
}
