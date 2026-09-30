package io.quarkus.bot.release.step;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHFileNotFoundException;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.error.StepExecutionException;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class Prerequisites implements StepHandler {

    private static final Logger LOG = Logger.getLogger(Prerequisites.class);

    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+$");
    private static final Pattern FINAL_VERSION_PATTERN = Pattern.compile("^[0-9]+\\.[0-9]+\\.[0-9]+(\\.[0-9]+)?$");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("\\d+");

    @Inject
    Issues issues;

    @Override
    public StepResult run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        GHRepository repository = Repositories.getQuarkusRepository(quarkusBotGitHub);

        String branch = releaseInformation.getBranch();
        String qualifier = releaseInformation.getQualifier() != null ? releaseInformation.getQualifier() : "";
        boolean emergency = releaseInformation.isEmergency();
        String emergencyReleaseCoreBranch = releaseInformation.getEmergencyReleaseCoreBranch() != null
                ? releaseInformation.getEmergencyReleaseCoreBranch()
                : "";
        String originBranch = releaseInformation.getOriginBranch();
        boolean lts = Branches.isLts(branch);

        boolean micro = !Branches.isMain(branch);
        boolean firstCR = releaseInformation.isFirstCR();
        boolean preCR1 = releaseInformation.isPreCR1();

        // Validate branch format
        if (!VERSION_PATTERN.matcher(branch).matches() && !Branches.isMain(branch)) {
            throw new StepExecutionException("Branch " + branch + " is not a valid version (X.y)", true);
        }

        // Check branch exists (skip for CR1 and pre-CR1 qualifiers)
        if (!firstCR && !preCR1) {
            try {
                repository.getBranch(branch);
            } catch (GHFileNotFoundException e) {
                throw new StepExecutionException("Branch " + branch + " does not exist in the repository", true);
            }
        }

        // Check emergency release core branch exists
        if (!emergencyReleaseCoreBranch.isBlank()) {
            try {
                repository.getBranch(emergencyReleaseCoreBranch);
            } catch (GHFileNotFoundException e) {
                throw new StepExecutionException(
                        "Emergency release Core branch " + emergencyReleaseCoreBranch
                                + " does not exist in the Core repository",
                        true);
            }
            LOG.infof("Working on branch for Core: %s", emergencyReleaseCoreBranch);
            LOG.infof("Working on branch for the rest: %s", branch);
        } else {
            LOG.infof("Working on branch: %s", branch);
        }

        // List tags
        LOG.infof("Listing tags of %s", repository.getName());
        NavigableSet<ComparableVersion> tags = new TreeSet<>();
        tags.addAll(repository.listTags().toList().stream()
                .map(t -> t.getName())
                .map(n -> new ComparableVersion(n))
                .collect(Collectors.toList()));

        tags = tags.descendingSet();

        if (tags.isEmpty()) {
            throw new StepExecutionException("No tags in repository " + repository.getName(), true);
        }

        // Find the last relevant tag
        ComparableVersion tag = null;
        if (Branches.isMain(branch)) {
            tag = tags.iterator().next();
        } else {
            for (ComparableVersion currentTag : tags) {
                if (currentTag.toString().startsWith(branch + ".")) {
                    tag = currentTag;
                    break;
                }
            }
        }

        // Compute new version
        String newVersion;
        if (tag != null) {
            LOG.infof("Last tag is: %s", tag);

            GHRelease release = repository.getReleaseByTagName(tag.toString());
            if (release == null) {
                LOG.warnf("No release associated with tag %s", tag);
            }

            newVersion = computeNewVersion(tag.toString(), micro, emergency, qualifier);
        } else {
            if (!qualifier.isBlank()) {
                newVersion = branch + ".0." + qualifier;
            } else {
                newVersion = branch + ".0";
            }
        }

        // Check there are no tags with this version
        boolean tagAlreadyExists = tags.stream()
                .anyMatch(t -> newVersion.equals(t.toString()));

        if (tagAlreadyExists) {
            throw new StepExecutionException("There is a tag with name " + newVersion + ", invalid increment", true);
        }

        // Check there is a milestone with the right name (skip for CR1 and pre-CR1)
        if (!firstCR && !preCR1) {
            checkIfMilestoneExists(repository, newVersion);
        }

        // List releases for maintenance detection
        LOG.infof("Listing releases of %s", repository.getName());
        NavigableSet<ComparableVersion> releases = new TreeSet<>();
        releases.addAll(repository.listReleases().toList().stream()
                .map(t -> t.getName())
                .map(n -> new ComparableVersion(n))
                .collect(Collectors.toList()));

        releases = releases.descendingSet();

        boolean maintenance = isMaintenance(branch, releases);

        // Write work files
        new File("work/").mkdirs();

        LOG.infof("Writing %s into the 'work/newVersion' file", newVersion);
        Files.writeString(Path.of("work", "newVersion"), newVersion, StandardCharsets.UTF_8);

        String coreReleaseBranch = Branches.getCoreReleaseBranch(releaseInformation);
        LOG.infof("Writing %s into the 'work/branch' file", coreReleaseBranch);
        Files.writeString(Path.of("work", "branch"), coreReleaseBranch, StandardCharsets.UTF_8);

        Files.writeString(Path.of("work", "originBranch"), originBranch, StandardCharsets.UTF_8);

        if (!emergencyReleaseCoreBranch.isBlank()) {
            LOG.infof("Writing %s into the 'work/emergency-release-core-branch' file", emergencyReleaseCoreBranch);
            Files.writeString(Path.of("work", "emergency-release-core-branch"), emergencyReleaseCoreBranch,
                    StandardCharsets.UTF_8);
        }
        if (emergency) {
            LOG.info("Releasing an emergency release");
            new File("work/emergency").createNewFile();
        }

        if (micro) {
            LOG.info("Releasing a micro release");
            new File("work/micro").createNewFile();
        }

        if (maintenance) {
            LOG.info("Releasing a maintenance release");
            new File("work/maintenance").createNewFile();
        }

        if (lts) {
            LOG.info("Releasing a LTS release");
            new File("work/lts").createNewFile();
        }

        if (!newVersion.endsWith(".Final") && !FINAL_VERSION_PATTERN.matcher(newVersion).matches()) {
            new File("work/preview").createNewFile();
        }

        // Set version on release information
        boolean firstFinal = Versions.isDot0(newVersion) ||
                (Versions.isFirstMicroMaintenanceRelease(newVersion)
                        && repository.getReleaseByTagName(Versions.getDot0(newVersion)) == null);

        releaseInformation.setVersion(newVersion, firstFinal, maintenance);

        issues.appendReleaseInformation(updatedIssueBody, releaseInformation);

        // Origin branch validation
        if (releaseInformation.isFirstCR() && Branches.isLts(releaseInformation.getBranch())) {
            if (releaseInformation.isOriginBranchDefault()) {
                throw new StepExecutionException(
                        "Origin branch is set to `" + Branches.getDefaultOriginBranch(releaseInformation.getBranch())
                                + "` for the CR1 of a LTS release.",
                        true,
                        "For the first CR of a LTS branch, we need the origin branch to be the branch of the previous minor as the LTS should be a direct continuation of the previous minor.");
            }
        } else if (!releaseInformation.isOriginBranchDefault()) {
            throw new StepExecutionException("Origin branch may only be set when releasing the CR1 of a LTS release.", true);
        }

        return StepResult.success();
    }

    static String computeNewVersion(String previousVersion, boolean micro, boolean emergency,
            String qualifier) {
        String[] segments = previousVersion.split("\\.");
        if (segments.length < 3) {
            throw new StepExecutionException(
                    "Invalid version " + previousVersion + ", number of segments must be at least 3, found: " + segments.length,
                    true);
        }

        String newVersion;
        if (emergency) {
            if (segments.length >= 4) {
                newVersion = segments[0] + "." + segments[1] + "." + segments[2] + "."
                        + (Integer.parseInt(segments[3]) + 1);
            } else {
                newVersion = segments[0] + "." + segments[1] + "." + segments[2] + ".1";
            }
        } else if (micro) {
            if (segments.length == 3) {
                newVersion = segments[0] + "." + segments[1] + "." + (Integer.parseInt(segments[2]) + 1);
            } else {
                String previousQualifier = segments[3];
                if (DIGITS_PATTERN.matcher(previousQualifier).matches()) {
                    newVersion = segments[0] + "." + segments[1] + "." + (Integer.parseInt(segments[2]) + 1);
                } else if ("Final".equals(previousQualifier)) {
                    newVersion = segments[0] + "." + segments[1] + "." + (Integer.parseInt(segments[2]) + 1);
                    qualifier = "Final";
                } else {
                    newVersion = segments[0] + "." + segments[1] + "." + segments[2];
                }
            }
        } else {
            newVersion = segments[0] + "." + (Integer.parseInt(segments[1]) + 1) + ".0";
        }
        if (!qualifier.isBlank()) {
            newVersion = newVersion + "." + qualifier;
        }

        return newVersion;
    }

    private static void checkIfMilestoneExists(GHRepository repository, String version) throws IOException {
        Optional<GHMilestone> milestoneOptional = repository.listMilestones(GHIssueState.OPEN).toList().stream()
                .filter(m -> version.equals(m.getTitle()))
                .findFirst();

        if (milestoneOptional.isEmpty()) {
            throw new StepExecutionException("No milestone found with version " + version, true);
        } else {
            GHMilestone milestone = milestoneOptional.get();
            LOG.infof("Found milestone %s", milestone.getTitle());
            if (milestone.getOpenIssues() != 0) {
                LOG.warnf("Milestone %s found, but %d issue(s) is/are still opened, check %s",
                        version, milestone.getOpenIssues(), milestone.getHtmlUrl());
            }
        }
    }

    private static boolean isMaintenance(String branch, NavigableSet<ComparableVersion> releases) {
        if (Branches.isMain(branch)) {
            return false;
        }

        return new ComparableVersion(getCurrentStableBranch(releases)).compareTo(new ComparableVersion(branch)) > 0;
    }

    private static String getCurrentStableBranch(NavigableSet<ComparableVersion> tags) {
        for (ComparableVersion candidate : tags) {
            String[] segments = candidate.toString().split("\\.");
            if (segments.length == 3) {
                return segments[0] + "." + segments[1];
            }
        }

        throw new StepExecutionException("Unable to find the current stable branch", true);
    }

}
