package io.quarkus.bot.release;

import jakarta.inject.Inject;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.UpdatedIssueBody;

public class GetReleaseInformationAction {

    @Inject
    Issues issues;

    @Action("get-release-information")
    void getReleaseInformation(Commands commands, @Issue.Opened GHEventPayload.Issue issuePayload) {
        extractReleaseInformation(commands, issuePayload.getIssue());
    }

    @Action("get-release-information")
    void getReleaseInformation(Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) {
        extractReleaseInformation(commands, issueCommentPayload.getIssue());
    }

    private void extractReleaseInformation(Commands commands, GHIssue issue) {
        commands.notice("Extracting release information...");

        ReleaseInformation releaseInformation;

        try {
            UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issue.getBody());
            releaseInformation = issues.extractReleaseInformation(updatedIssueBody);
        } catch (Exception e) {
            releaseInformation = issues.extractReleaseInformationFromForm(issue.getBody());
        }

        commands.setOutput(Outputs.BRANCH, releaseInformation.getBranch());
        if (releaseInformation.getQualifier() != null) {
            commands.setOutput(Outputs.QUALIFIER, releaseInformation.getQualifier());
        }
        commands.setOutput(Outputs.MAJOR, Boolean.toString(releaseInformation.isMajor()));
        if (releaseInformation.getVersion() != null) {
            commands.setOutput(Outputs.VERSION, releaseInformation.getVersion());
        }
    }
}
