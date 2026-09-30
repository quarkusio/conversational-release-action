package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.Optional;

import jakarta.inject.Singleton;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class CoreReleaseRenameMilestoneForPreview implements StepHandler {

    private static final Logger LOG = Logger.getLogger(CoreReleaseRenameMilestoneForPreview.class);

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return !isPreCR1(releaseInformation);
    }

    private static boolean isPreCR1(ReleaseInformation releaseInformation) {
        String qualifier = releaseInformation.getQualifier();
        if (qualifier == null || qualifier.isBlank()) {
            return false;
        }
        return new ComparableVersion(qualifier).compareTo(new ComparableVersion("CR1")) < 0;
    }

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        GHRepository repository = Repositories.getQuarkusRepository(quarkusBotGitHub);

        String minorVersion = Versions.getMinorVersion(releaseInformation.getVersion());
        String milestoneName = minorVersion + " - " + Branches.getDefaultOriginBranch(releaseInformation.getBranch());

        Optional<GHMilestone> versionedMilestone = getMilestone(repository, releaseInformation.getVersion());
        if (versionedMilestone.isPresent()) {
            LOG.infof("Milestone %s already exists, skipping rename", releaseInformation.getVersion());
            return StepResult.success();
        }

        Optional<GHMilestone> milestone = getMilestone(repository, milestoneName);
        if (milestone.isEmpty()) {
            throw new IllegalStateException(
                    "Milestone " + releaseInformation.getVersion() + " does not exist and we were unable to find milestone "
                            + milestoneName + " to rename it");
        }

        try {
            milestone.get().setTitle(releaseInformation.getVersion());
            repository.createMilestone(milestoneName, "");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to update the milestone or create the new milestone: " + e.getMessage(), e);
        }

        issue.comment(":white_check_mark: Milestone `" + milestoneName + "` has been renamed to `"
                + releaseInformation.getVersion() + "` and a new `" + milestoneName + "` milestone has been created.\n\n"
                + "Make sure to [adjust the name of the milestone](https://github.com/quarkusio/quarkus/milestones) if needed.");

        return StepResult.success();
    }

    private static Optional<GHMilestone> getMilestone(GHRepository repository, String name) {
        try {
            return repository.listMilestones(GHIssueState.OPEN).toList().stream()
                    .filter(m -> name.equals(m.getTitle()))
                    .findFirst();
        } catch (Exception e) {
            LOG.warnf(e, "Unable to find milestone %s", name);
            return Optional.empty();
        }
    }
}
