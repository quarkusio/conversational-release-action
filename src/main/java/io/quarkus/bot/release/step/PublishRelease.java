package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder.MakeLatest;
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

        if (releaseInformation.isFirstFinal() && !releaseInformation.isDot0()) {
            String dot0Version = Versions.getMinorVersion(version) + ".0";
            GHRelease dot0Release = findRelease(repository, dot0Version);
            if (dot0Release != null && dot0Release.isDraft()) {
                publishRelease(commands, dot0Release, MakeLatest.FALSE);
            }
        }

        GHRelease release = findRelease(repository, version);
        if (release == null) {
            throw new IllegalStateException(
                    "Release " + version + " not found, it should have been created by the post-Platform-release step");
        }

        // GitHub doesn't keep the makeLatest value defined when creating a draft release
        // and makes the release the latest one by default when publishing it so we have to be explicit
        MakeLatest makeLatest = !releaseInformation.isFinal() || releaseInformation.isMaintenance() ? MakeLatest.FALSE
                : MakeLatest.TRUE;
        publishRelease(commands, release, makeLatest);

        return StepResult.success();
    }

    private static void publishRelease(Commands commands, GHRelease release, MakeLatest makeLatest) throws IOException {
        if (!release.isDraft()) {
            commands.warning("Release " + release.getTagName() + " is already published, skipping");
            return;
        }

        release.update().draft(false).makeLatest(makeLatest).update();
    }

    /**
     * {@link GHRepository#getReleaseByTagName(String)} only returns published releases so we also look for drafts
     * in the most recent releases: a draft is created during the release process so it is always one of the latest.
     */
    private static GHRelease findRelease(GHRepository repository, String version) throws IOException {
        GHRelease release = repository.getReleaseByTagName(version);
        if (release != null) {
            return release;
        }

        return repository.listReleases().withPageSize(20).iterator().nextPage().stream()
                .filter(r -> version.equals(r.getTagName()))
                .findFirst()
                .orElse(null);
    }
}
