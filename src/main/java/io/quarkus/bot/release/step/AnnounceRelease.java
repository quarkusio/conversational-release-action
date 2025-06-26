package io.quarkus.bot.release.step;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import io.quarkus.bot.release.util.Admonitions;
import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Labels;
import io.quarkus.bot.release.util.Repositories;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class AnnounceRelease implements StepHandler {

    private static final Logger LOG = Logger.getLogger(AnnounceRelease.class);

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody)
            throws IOException, InterruptedException {
        StringBuilder comment = new StringBuilder();

        comment.append(":white_check_mark: " + releaseInformation.getVersion() + " was successfully released.\n\n");

        comment.append(":raised_hands: Some manual steps are required to finalize the release.\n\n");

        comment.append(Admonitions.important("You need to:\n\n"
                + "- Trigger the performance testing (we can't automate it for now for security reasons)\n"
                + "- Announce the release on `quarkus-dev@`" + (releaseInformation.isFinal() ? " and on social networks" : "")
                + "\n\n"
                + "You can find detailed instructions below.") + "\n\n");

        comment.append("## Trigger performance testing\n\n");

        comment.append("Connected to the Red Hat VPN, in a clone of https://github.com/quarkusio/main-release-scripts, run:\n");
        comment.append("```\n");
        comment.append("./trigger-performance-testing.sh " + releaseInformation.getVersion() + "\n");
        comment.append("```\n");
        comment.append("to trigger the performance evaluation testing for this release.\n\n");

        comment.append("## Announce release\n\n");

        if (releaseInformation.isFinal()) {
            String blogPostSlug = "quarkus-"
                    + (releaseInformation.isFirstFinal() ? Versions.getMinorVersion(releaseInformation.getVersion())
                            : releaseInformation.getVersion()).replace(".", "-").toLowerCase(Locale.ROOT)
                    + "-released";

            comment.append("Then it is time to announce the release:\n\n");
            if (!releaseInformation.isMaintenance()) {
                comment.append(
                        "* Update the versions of the website in [`_data/versions.yaml`](https://github.com/quarkusio/quarkusio.github.io/blob/develop/_data/versions.yaml):\n");
                comment.append("  * Update `version:` to `").append(releaseInformation.getVersion()).append("`\n");
                comment.append("  * Update `announce:` to `/blog/").append(blogPostSlug).append(
                        "/` - be careful, we use the minor version in the URL for the first final and the full version for follow-up micro releases\n");
                if (releaseInformation.isFirstFinal()) {
                    comment.append("  * Update requirements if needed\n");
                }
            }
            if (Branches.isLts(releaseInformation.getBranch())) {
                comment.append(
                        "* This is a LTS version so make sure the version is referenced in the `documentation:` section of [`_data/versions.yaml`](https://github.com/quarkusio/quarkusio.github.io/blob/develop/_data/versions.yaml)\n");
            }
            comment.append("* Write a blog post in `_posts/").append(DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now()))
                    .append("-").append(blogPostSlug).append(".adoc")
                    .append("` for [the website](https://github.com/quarkusio/quarkusio.github.io)\n");
            comment.append(
                    "  * Use a previous announcement as a template (be aware, announcements are very different for the first final of a major/minor and the follow-up micros)\n");
            if (releaseInformation.isFirstFinal()) {
                comment.append("  * If a Mandrel/GraalVM upgrade is necessary, make sure it is prominent in the announcement");
            }
            comment.append(
                    "* Push it and wait for it to be live on [quarkus.io](https://quarkus.io/blog/) - you can follow the progress of the deployment on [GitHub Actions](https://github.com/quarkusio/quarkusio.github.io/actions)\n");
            comment.append("* Send the announcement to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com)\n");
            comment.append("* Send the announcement to various social networks using https://buffer.com/\n");

            try {
                comment.append("\n\nYou can find below a template that can help you with writing the announcement email.\n\n");
                comment.append(generateAnnouncement(quarkusBotGitHub, releaseInformation, blogPostSlug));
            } catch (Exception e) {
                commands.warning("An error occurred while generating the email announcement template. Ignoring.");
                LOG.warn("An error occurred while generating the email announcement template. Ignoring.", e);
            }

            if (releaseInformation.isFirstFinal()) {
                try {
                    String previousMinorBranch = getPreviousMinorBranch(Repositories.getQuarkusRepository(quarkusBotGitHub),
                            releaseInformation.getBranch());

                    comment.append(
                            "\n\nFor new major/minor releases, we include the list of contributors in the announcement blog post.\n");
                    comment.append(
                            "The number of contributors can be found in the `Contributors` section of the [project home page](https://github.com/quarkusio/quarkus).\n");
                    comment.append(
                            "You can get a rough list of contributors (check for duplicates!) since the previous minor by executing the following commands in a Quarkus repository local clone:\n\n");
                    comment.append("```\n");
                    comment.append("git fetch upstream --tags\n");
                    comment.append("git shortlog -s '" + previousMinorBranch + ".0'..'" + releaseInformation.getVersion()
                            + "' | cut -d$'\\t' -f 2 | grep -v dependabot | grep -v quarkusbot | sort -d -f -i | paste -sd ',' - | sed 's/,/, /g'\n");
                    comment.append("```\n\n");
                } catch (Exception e) {
                    LOG.warn("An error occurred while trying to get the previous minor. Ignoring.", e);
                }
            }
        } else {
            comment.append(
                    "Then it is time to send an email to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com):\n\n");
            comment.append("Subject:\n");
            comment.append("```\n");
            comment.append("Quarkus " + releaseInformation.getVersion() + " released\n");
            comment.append("```\n");
            comment.append("Body:\n");
            comment.append("```\n");
            comment.append("Hi,\n"
                    + "\n"
                    + "We released Quarkus " + releaseInformation.getVersion() + ".\n"
                    + "\n"
                    + "Changelog is here:\n"
                    + "https://github.com/quarkusio/quarkus/releases/tag/" + releaseInformation.getVersion() + "\n"
                    + "\n"
                    + "Please try to upgrade your applications and report back:\n"
                    + "- if everything is going well, just post a reply to this thread\n"
                    + "- if you encounter issues, please open a GitHub issue in our tracker with a simple reproducer\n"
                    + "\n"
                    + "We will build the final core artifacts next Wednesday.\n"
                    + "\n"
                    + "Thanks!\n\n"
                    + "--\n"
                    + "The Quarkus dev team\n");
            comment.append("```\n");
        }

        issue.comment(comment.toString());
        issue.close();

        return 0;
    }

    private static String getPreviousMinorBranch(GHRepository repository, String currentBranch) throws IOException {
        TreeSet<ComparableVersion> tags = repository.listTags().toList().stream()
                .map(t -> Versions.getBranch(t.getName()))
                .collect(Collectors.toCollection(TreeSet::new));

        return Versions.getPreviousMinorBranch(tags, Versions.getBranch(currentBranch));
    }

    private static String generateAnnouncement(GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            String blogPostSlug) throws IOException {
        String version = releaseInformation.getVersion();
        String fullVersion = releaseInformation.getFullVersion();
        String minorVersion = Versions.getMinorVersion(releaseInformation.getVersion());

        GHRepository quarkusRepository = Repositories.getQuarkusRepository(quarkusBotGitHub);
        GHMilestone milestone = quarkusRepository.listMilestones(GHIssueState.CLOSED).toList().stream()
                .filter(m -> version.equals(m.getTitle()))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unable to find a closed milestone for " + version));
        List<GHIssue> issues = quarkusRepository.getIssues(GHIssueState.CLOSED, milestone);

        List<GHIssue> majorChanges = issues.stream()
                .filter(i -> i.getLabels().stream().anyMatch(l -> Labels.RELEASE_NOTEWORTHY_FEATURE_LABEL.equals(l.getName())))
                .sorted(Comparator.comparingInt(GHIssue::getNumber))
                .toList();
        List<GHIssue> breakingChanges = issues.stream()
                .filter(i -> i.getLabels().stream().anyMatch(l -> Labels.RELEASE_BREAKING_CHANGE_LABEL.equals(l.getName())))
                .filter(i -> majorChanges.stream().noneMatch(o -> o.getNumber() == i.getNumber()))
                .sorted(Comparator.comparingInt(GHIssue::getNumber))
                .toList();

        String announcement = "";

        if (!majorChanges.isEmpty()) {
            announcement += "### Newsworthy changes (in Asciidoc)\n\n";
            announcement += "```\n";
            for (GHIssue majorChange : majorChanges) {
                announcement += "* " + issueTitleInAsciidoc(majorChange) + "\n";
            }
            announcement += "```\n\n";
        }

        if (!breakingChanges.isEmpty()) {
            announcement += "### Other breaking changes (FYI, in Asciidoc)\n\n";
            announcement += "```\n";
            for (GHIssue breakingChange : breakingChanges) {
                announcement += "* " + issueTitleInAsciidoc(breakingChange) + "\n";
            }
            announcement += "```\n\n";
        }

        if (releaseInformation.isFirstFinal()) {
            announcement += "It might also be a good idea to have a look at the [migration guide for this version](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-"
                    + minorVersion + ").\n\n";
        }

        announcement += "### Announcement email template\n\n";

        announcement += "Subject:\n";
        announcement += "```\n";
        announcement += "[RELEASE] Quarkus " + fullVersion + "\n";
        announcement += "```\n";
        announcement += "Body:\n";
        announcement += "```\n";
        announcement += "Hello,\n" +
                "\n" +
                "Quarkus " + fullVersion
                + " has been released, and is now available from the Maven Central repository. The quickstarts and documentation have also been updated.\n"
                +
                "\n" +
                "More information in the announcement blog post: https://quarkus.io/blog/" + blogPostSlug + "/.\n" +
                "\n";
        if (!majorChanges.isEmpty()) {
            announcement += "* Major changes:\n" +
                    "\n" +
                    majorChanges.stream().map(mc -> "  * " + issueTitle(mc)).collect(Collectors.joining("\n")) +
                    "\n\n";
        }
        announcement += "* BOM dependency:\n" +
                "\n" +
                "  <dependency>\n" +
                "      <groupId>io.quarkus.platform</groupId>\n" +
                "      <artifactId>quarkus-bom</artifactId>\n" +
                "      <version>" + releaseInformation.getVersion() + "</version>\n" +
                "      <type>pom</type>\n" +
                "      <scope>import</scope>\n" +
                "  </dependency>\n" +
                "\n";

        if (releaseInformation.isFirstFinal()) {
            if (releaseInformation.isDot0()) {
                announcement += "* Changelogs are available from https://github.com/quarkusio/quarkus/releases/tag/" + version
                        + ".CR1 and https://github.com/quarkusio/quarkus/releases/tag/" + version + "\n";
            } else {
                announcement += "* Changelogs are available from https://github.com/quarkusio/quarkus/releases/tag/"
                        + minorVersion + ".0.CR1, https://github.com/quarkusio/quarkus/releases/tag/" + minorVersion
                        + ".0, and https://github.com/quarkusio/quarkus/releases/tag/" + version + "\n";
            }
            announcement += "* Download is available from https://github.com/quarkusio/quarkus/releases/tag/" + version + "\n";
        } else {
            announcement += "* Changelog and download are available from https://github.com/quarkusio/quarkus/releases/tag/"
                    + version + "\n";
        }

        announcement += "* Documentation: https://quarkus.io\n";
        announcement += "\n";
        announcement += "--\n";
        announcement += "The Quarkus dev team\n";
        announcement += "```\n\n";

        return announcement;
    }

    private static String issueTitle(GHIssue issue) {
        return "[#" + issue.getNumber() + "] " + issue.getTitle();
    }

    private static String issueTitleInAsciidoc(GHIssue issue) {
        return issue.getHtmlUrl() + "[#" + issue.getNumber() + "] - " + issue.getTitle();
    }
}
