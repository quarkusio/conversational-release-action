package io.quarkus.bot.release;

import jakarta.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Jdks;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.UpdatedIssueBody;

public class GetReleaseInformationAction {

    @Inject
    Issues issues;

    @Inject
    Jdks jdks;

    @Action("get-release-information")
    void getReleaseInformation(Commands commands, @Issue.Opened GHEventPayload.Issue issuePayload) {
        commands.notice("Extracting release information...");

        ReleaseInformation releaseInformation = issues.extractReleaseInformationFromForm(issuePayload.getIssue().getBody());
        outputReleaseInformation(commands, releaseInformation);
    }

    @Action("get-release-information")
    void getReleaseInformation(Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) {
        commands.notice("Extracting release information...");

        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issueCommentPayload.getIssue().getBody());
        ReleaseInformation releaseInformation = issues.extractReleaseInformation(updatedIssueBody);

        outputReleaseInformation(commands, releaseInformation);
    }

    private void outputReleaseInformation(Commands commands, ReleaseInformation releaseInformation) {
        commands.setOutput(Outputs.BRANCH, releaseInformation.getBranch());
        if (releaseInformation.getQualifier() != null) {
            commands.setOutput(Outputs.QUALIFIER, releaseInformation.getQualifier());
        }
        commands.setOutput(Outputs.MAJOR, Boolean.toString(releaseInformation.isMajor()));
        if (releaseInformation.getVersion() != null) {
            commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
        }

        commands.setOutput(Outputs.JDK, jdks.getJdkVersion(releaseInformation.getBranch()));

        commands.setOutput(Outputs.ORIGIN_BRANCH, releaseInformation.getOriginBranch());
    }
}
