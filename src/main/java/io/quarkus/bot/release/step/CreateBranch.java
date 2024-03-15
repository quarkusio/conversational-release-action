package io.quarkus.bot.release.step;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHMilestone;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.Progress;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class CreateBranch implements StepHandler {

    private static final Logger LOG = Logger.getLogger(CreateBranch.class);

    private static final String BACKPORT_LABEL = "triage/backport";
    private static final String BACKPORT_LABEL_COLOR = "7fe8cd";
    private static final String MAIN_MILESTONE_SUFFIX = " - main";

    @Override
    public boolean shouldSkip(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        return !releaseInformation.isFirstCR();
    }

    @Override
    public boolean shouldPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {

        StringBuilder comment = new StringBuilder();
        comment.append(":raised_hands: **IMPORTANT** This is the first Candidate Release and this release requires special care.\n\n");
        comment.append("You have two options:\n\n");
        comment.append("- Let the release process handle things automatically: it will create the branch automatically and handle additional housekeeping operations\n");
        comment.append("- Perform all the operations manually\n\n");

        comment.append(":bulb: **To let the release process handle things automatically for you, simply add a `" + Command.AUTO.getFullCommand() + "` comment**\n\n");

        comment.append("---\n\n");
        comment.append("<details><summary>Handle things manually</summary>\n\n");
        comment.append("If you choose to do things manually, make sure you perform all the following tasks:\n\n");
        comment.append(
                "- Create the `" + releaseInformation.getBranch() + "` branch and push it to the upstream repository\n");
        comment.append("- Rename the `" + releaseInformation.getBranch()
                + " - main` milestone [here](https://github.com/quarkusio/quarkus/milestones) to " + releaseInformation.getVersion() + "\n");
        comment.append(
                "- Create a new milestone `X.Y - main` milestone [here](https://github.com/quarkusio/quarkus/milestones/new) with `X.Y` being the next major/minor version name. Make sure you follow the naming convention, it is important.\n");
        comment.append("- Rename the `triage/backport?` label to `triage/backport-" + releaseInformation.getBranch() + "?`\n");
        comment.append("- Create a new `triage/backport?` label\n");
        comment.append("- Make sure [all the current opened pull requests with the `triage/backport-"
                + releaseInformation.getBranch()
                + "?` label](https://github.com/quarkusio/quarkus/pulls?q=is%3Apr+is%3Aopen+label%3Atriage%2Fbackport-"
                + releaseInformation.getBranch()
                + "%3F+) also have the new `triage/backport?` label (in the UI, you can select all the pull requests with the top checkbox then use the `Label` dropdown to apply the `triage/backport?` label)\n");
        comment.append("- Send an email to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com) announcing that `" + releaseInformation.getBranch() + "` has been branched and post on [Zulip #dev stream](https://quarkusio.zulipchat.com/#narrow/stream/187038-dev/):\n\n");

        String previousMinorBranch;
        try {
            previousMinorBranch = getPreviousMinorBranch(Repositories.getQuarkusRepository(quarkusBotGitHub), releaseInformation.getBranch());
        } catch (IOException e) {
            previousMinorBranch = "previous minor";
        }

        comment.append(getBranchEmail(releaseInformation, previousMinorBranch, null) + "\n\n");
        comment.append("Once you are done with all this, add a `" + Command.MANUAL.getFullCommand() + "` comment to let the release process know you have handled everything manually.\n\n");
        comment.append("</details>\n\n");

        comment.append(Progress.youAreHere(releaseInformation, releaseStatus));

        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());

        return true;
    }

    @Override
    public boolean shouldContinueAfterPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.AUTO.matches(issueComment.getBody());
    }

    @Override
    public boolean shouldSkipAfterPause(Context context, Commands commands, GitHub quarkusBotGitHub,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssue issue, GHIssueComment issueComment) {
        return Command.MANUAL.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws InterruptedException, IOException {
        GHRepository repository = Repositories.getQuarkusRepository(quarkusBotGitHub);

        try {
            repository.getBranch(releaseInformation.getBranch());
        } catch (Exception e) {
            // the branch does not exist, let's create it
            try {
                String sha = repository.getBranch(Branches.MAIN).getSHA1();
                repository.createRef("refs/heads/" + releaseInformation.getBranch(), sha);
            } catch (Exception e2) {
                throw new IllegalStateException(
                        "Unable to create branch " + releaseInformation.getBranch() + ": " + e2.getMessage(), e2);
            }
        }

        Optional<GHMilestone> versionedMilestone = getMilestone(repository, releaseInformation.getVersion());
        String nextMinor = getNextMinor(releaseInformation.getBranch());

        if (versionedMilestone.isEmpty()) {
            // the version milestone does not exist, we try to rename the "X.Y - main" milestone to the version

            Optional<GHMilestone> milestone = getMilestone(repository, releaseInformation.getBranch() + MAIN_MILESTONE_SUFFIX);
            if (milestone.isPresent()) {
                try {
                    milestone.get().setTitle(releaseInformation.getVersion());

                    repository.createMilestone(nextMinor + MAIN_MILESTONE_SUFFIX, "");
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to update the milestone or create the new milestone: " + e.getMessage(), e);
                }
            } else {
                throw new IllegalStateException(
                        "Milestone " + releaseInformation.getVersion() + " does not exist and we were unable to find milestone "
                                + releaseInformation.getBranch() + MAIN_MILESTONE_SUFFIX + " to rename it");
            }
        }

        String previousMinorBranch;
        try {
            previousMinorBranch = getPreviousMinorBranch(repository, releaseInformation.getBranch());
        } catch (Exception e) {
            throw new IllegalStateException("Unable to resolve previous minor branch: " + e.getMessage(), e);
        }
        String previousMinorBackportLabel = "triage/backport-" + previousMinorBranch;

        try {
            repository.getLabel(previousMinorBackportLabel);
        } catch (Exception e) {
            try {
                repository.getLabel(BACKPORT_LABEL).update().name(previousMinorBackportLabel).done();
            } catch (Exception e2) {
                throw new IllegalStateException("Unable to rename backport label: " + e2.getMessage(), e2);
            }
        }

        try {
            repository.getLabel(BACKPORT_LABEL);
        } catch (Exception e) {
            try {
                repository.createLabel(BACKPORT_LABEL, BACKPORT_LABEL_COLOR);
            } catch (Exception e2) {
                throw new IllegalStateException("Unable to create new backport label: " + e2.getMessage(), e2);
            }
        }

        try {
            List<GHPullRequest> openedPullRequestsToBackport = repository.searchPullRequests().label(previousMinorBackportLabel).isOpen().list().toList();

            for (GHPullRequest openedPullRequestToBackport : openedPullRequestsToBackport) {
                openedPullRequestToBackport.addLabels(BACKPORT_LABEL);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to affect pull requests to the new backport label: " + e.getMessage(), e);
        }

        String comment = ":white_check_mark: Branch " + releaseInformation.getBranch()
                + " has been created and the milestone and backport labels adjusted.\n\n";
        comment += "We created a new " + nextMinor
                + " milestone for future developments, make sure to [adjust the name of the milestone](https://github.com/quarkusio/quarkus/milestones) if needed as the name has simply been inferred from the current release\n\n";
        comment += "Please announce that we branched " + releaseInformation.getBranch()
                + " by sending an email to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com) and posting on [Zulip #dev stream](https://quarkusio.zulipchat.com/#narrow/stream/187038-dev/):\n\n";
        comment += "**(Make sure to adjust the version in the email if you renamed the milestone)**\n\n";
        comment += getBranchEmail(releaseInformation, previousMinorBranch, nextMinor) + "\n\n";
        comment += ":bulb: **Apart from sending the email and posting on Zulip, no intervention from you is needed, the release process is in progress.**\n\n";
        comment += Progress.youAreHere(releaseInformation, releaseStatus);

        issue.comment(comment);

        return 0;
    }

    private static String getPreviousMinorBranch(GHRepository repository, String currentBranch) throws IOException {
        TreeSet<ComparableVersion> tags = repository.listTags().toList().stream()
                .map(t -> Versions.getBranch(t.getName()))
                .collect(Collectors.toCollection(TreeSet::new));

        return Versions.getPreviousMinorBranch(tags, Versions.getBranch(currentBranch));
    }

    private static String getNextMinor(String currentBranch) {
        String[] segments = currentBranch.toString().split("\\.");

        if (segments.length < 2) {
            throw new IllegalStateException("CR1 releases should be made from a versioned branch and not from main");
        }

        return segments[0] + "." + (Integer.parseInt(segments[1]) + 1);
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

    private static String getBranchEmail(ReleaseInformation releaseInformation, String previousMinorBranch, String nextMinor) {
        String email = "Subject:\n"
                + "```\n"
                + "Quarkus " + releaseInformation.getBranch() + " branched\n"
                + "```\n"
                + "Body:\n"
                + "```\n"
                + "Hi,\n"
                + "\n"
                + "We just branched " + releaseInformation.getBranch() + ". The main branch is now " + (nextMinor != null ? nextMinor : "**X.Y**") + ".\n"
                + "\n"
                + "Please make sure you add the appropriate backport labels from now on:\n"
                + "\n"
                + "- for anything required in " + releaseInformation.getBranch() + " (currently open pull requests included), please add the triage/backport? label\n";

        if (!Branches.LTS_BRANCHES.contains(previousMinorBranch)) {
            email += "- for fixes we also want in future " + previousMinorBranch + ", please add the triage/backport-" + previousMinorBranch + "? label\n";
        }

        for (String ltsBranch : Branches.LTS_BRANCHES) {
            if (ltsBranch.equals(releaseInformation.getBranch())) {
                continue;
            }

            // 2.13 is not an official LTS so we have to special case it
            email += "- for fixes we also want in future " + ltsBranch
                    + (Branches.BRANCH_2_13.equals(ltsBranch) ? "" : " LTS") + ", please add the triage/backport-" + ltsBranch
                    + "? label\n";
        }

        email += "\n"
                + "Thanks!\n"
                + "\n"
                + "--\n"
                + "The Quarkus dev team\n"
                + "```";

        return email;
    }
}
