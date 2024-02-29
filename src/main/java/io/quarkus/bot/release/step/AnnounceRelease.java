package io.quarkus.bot.release.step;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHIssue;
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
public class AnnounceRelease implements StepHandler {

    private static final Logger LOG = Logger.getLogger(AnnounceRelease.class);

    @Override
    public int run(Context context, Commands commands, GitHub quarkusBotGitHub, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus, GHIssue issue, UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        StringBuilder comment = new StringBuilder();

        comment.append(":white_check_mark: " + releaseInformation.getVersion() + " was successfully released.\n\n");

        comment.append("Connected to the Red Hat VPN, in a clone of https://github.com/quarkusio/quarkus-release, run:\n");
        comment.append("```\n");
        comment.append("./trigger-performance-testing.sh " + releaseInformation.getVersion() + "\n");
        comment.append("```\n");
        comment.append("to trigger the performance evaluation testing for this release.\n\n");

        if (releaseInformation.isFinal()) {
            comment.append("Then it is time to write the announcement:\n\n");
            if (!releaseInformation.isMaintenance()) {
                comment.append("* Update the versions in `_data/versions.yaml`\n");
            }
            comment.append("* Write a blog post for [the website](https://github.com/quarkusio/quarkusio.github.io)\n");
            comment.append(
                    "  * Use a previous announcement as a template (be aware, annoucements are very different for the first final of a major/minor and the follow-up micros)\n");
            if (releaseInformation.isFirstFinal()) {
                comment.append("  * If a Mandrel/GraalVM upgrade is necessary, make sure it is prominent in the announcement");
            }
            comment.append("* Push it and wait for it to be live on [quarkus.io](https://quarkus.io/blog/) - you can follow the progress of the deployment on [GitHub Actions](https://github.com/quarkusio/quarkusio.github.io/actions)\n");
            comment.append("* Send the announcement to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com)\n");
            comment.append("* Send the announcement to various social networks using https://buffer.com/\n");

            Path announcePath = Path.of("work", "announce-" + releaseInformation.getVersion() + ".txt");
            try {
                String announceText = Files.readString(announcePath);

                comment.append("\n\nHere is some content that could help in writing the announcement:\n\n");
                comment.append(announceText);
            } catch (Exception e) {
                commands.warning("An error occurred while reading " + announcePath + ": " + e.getMessage() + ". Ignoring.");
                LOG.warn("An error occurred while reading " + announcePath + ". Ignoring.", e);
            }

            if (releaseInformation.isFirstFinal()) {
                try {
                    String previousMinorBranch = getPreviousMinorBranch(Repositories.getQuarkusRepository(quarkusBotGitHub), releaseInformation.getBranch());

                    comment.append(
                            "\n\nFor new major/minor releases, we include the list of contributors in the announcement blog post.\n");
                    comment.append("The number of contributors can be found in the `Contributors` section of the [project home page](https://github.com/quarkusio/quarkus).\n");
                    comment.append(
                            "You can get a rough list of contributors (check for duplicates!) since the previous minor by executing the following commands in a Quarkus repository local clone:\n\n");
                    comment.append("> git fetch upstream --tags\n");
                    comment.append("> git shortlog -s '" + previousMinorBranch + ".0'..'" + releaseInformation.getVersion()
                            + "' | cut -d$'\\t' -f 2 | grep -v dependabot | grep -v quarkusbot | sort -d -f -i | paste -sd ',' - | sed 's/,/, /g'\n\n");
                } catch (Exception e) {
                    LOG.warn("An error occurred while trying to get the previous minor. Ignoring.", e);
                }
            }
        } else {
            comment.append("Then it is time to send an email to [quarkus-dev@googlegroups.com](mailto:quarkus-dev@googlegroups.com):\n\n");
            comment.append("Subject: `Quarkus " + releaseInformation.getVersion() + " released`\n\n");
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
}
