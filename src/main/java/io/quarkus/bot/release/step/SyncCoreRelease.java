package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;
import org.kohsuke.github.GHIssueComment;

import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Context;
import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.Command;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.UpdatedIssueBody;
import io.quarkus.bot.release.util.Versions;

@Singleton
@Unremovable
public class SyncCoreRelease implements StepHandler {

    @Override
    public boolean shouldPause(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus) {
        StringBuilder comment = new StringBuilder();
        if (Versions.getVersion(releaseInformation.getBranch()).compareTo(Versions.VERSION_3_6) < 0) {
            comment.append("The core artifacts have been pushed to a staging repository on `s01.oss.sonatype.org`.\n\n");
            comment.append(":warning: You need to release the staging repository **manually** at https://s01.oss.sonatype.org/#stagingRepositories.\n\n");
        } else {
            comment.append("The core artifacts have been pushed to `s01.oss.sonatype.org`.\n\n");
        }
        comment.append(
                "**IMPORTANT** You need to wait for them to be synced to Maven Central before continuing with the release:\n\n");
        comment.append("* Wait for one hour\n");
        comment.append("* Check that https://repo1.maven.org/maven2/io/quarkus/quarkus-core/" + releaseInformation.getVersion() + "/"
                + " does not return a 404\n\n");
        comment.append(
                "Once these two conditions are met, you can continue with the release by adding a `"
                        + Command.CONTINUE.getFullCommand() + "` comment.");
        comment.append("\n\n(Note that we plan to automate this step soonish)");
        commands.setOutput(Outputs.INTERACTION_COMMENT, comment.toString());
        return true;
    }

    @Override
    public boolean shouldContinue(Context context, Commands commands,
            ReleaseInformation releaseInformation, ReleaseStatus releaseStatus, GHIssueComment issueComment) {
        return Command.CONTINUE.matches(issueComment.getBody());
    }

    @Override
    public int run(Context context, Commands commands, ReleaseInformation releaseInformation, GHIssue issue,
            UpdatedIssueBody updatedIssueBody) throws IOException, InterruptedException {
        issue.comment(":white_check_mark: Core artifacts have been synced to Maven Central, continuing...");
        return 0;
    }
}
