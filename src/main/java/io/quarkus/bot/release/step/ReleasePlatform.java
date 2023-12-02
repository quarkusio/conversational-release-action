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

@Singleton
@Unremovable
public class ReleasePlatform implements StepHandler {

    @Override
    public boolean shouldPause(Context context, Commands commands, ReleaseInformation releaseInformation,
            ReleaseStatus releaseStatus) {
        StringBuilder comment = new StringBuilder();
        comment.append("Now is time to release the Quarkus Platform. This is a manual process:\n\n");
        comment.append("* Make sure you have merged all the pull requests that should be included in this version of the Platform");
        comment.append("* Then follow (roughly) this process:\n\n");
        comment.append("```\n");
        comment.append("cd <your quarkus-platform clone>\n");
        comment.append("git checkout " + releaseInformation.getBranch() + "\n");
        comment.append("git pull upstream " + releaseInformation.getBranch() + "\n");
        comment.append("git checkout -b quarkus-" + releaseInformation.getVersion() + "\n");
        comment.append("./update-quarkus-version.sh " + releaseInformation.getVersion() + "\n");
        comment.append("```\n\n");
        comment.append("* Check the diff with `git diff`\n\n");
        comment.append("```\n");
        comment.append("git commit -a -m 'Upgrade to Quarkus " + releaseInformation.getVersion() + "'\n");
        comment.append("git push origin quarkus-" + releaseInformation.getVersion() + "\n");
        comment.append("```\n\n");
        comment.append("* Create a pull request\n");
        comment.append("* Wait for CI to go green\n");
        comment.append("* Merge the pull request\n\n");
        comment.append("```\n");
        comment.append("git checkout " + releaseInformation.getBranch() + "\n");
        comment.append("git pull upstream " + releaseInformation.getBranch() + "\n");
        comment.append("```\n\n");
        comment.append("* Then actually release the branch with the following line:\n\n");
        comment.append("> TAG=" + releaseInformation.getVersion() + " && ./check-version.sh $TAG && ./mvnw release:prepare release:perform -DdevelopmentVersion=999-SNAPSHOT -DreleaseVersion=$TAG -Dtag=$TAG -DperformRelease -Prelease,releaseNexus -DskipTests -Darguments=-DskipTests\n\n");
        comment.append(
                ":warning: You need to wait for them to be synced to Maven Central before pursuing with the release:\n\n");
        comment.append("* Wait for 40 minutes\n");
        comment.append("* Check that https://repo1.maven.org/maven2/io/quarkus/platform/quarkus-bom/" + releaseInformation.getVersion() + "/"
                + " does not return a 404\n\n");
        comment.append(
                "Once these two conditions are true, you can pursue the release by adding a `"
                        + Command.CONTINUE.getFullCommand() + "` comment.");
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
        issue.comment(":white_check_mark: Platform artifacts have been synced to Maven Central, pursuing...");
        return 0;
    }
}
