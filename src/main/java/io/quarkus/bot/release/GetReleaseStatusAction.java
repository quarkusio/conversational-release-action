package io.quarkus.bot.release;

import jakarta.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.UpdatedIssueBody;

public class GetReleaseStatusAction {

    @Inject
    Issues issues;

    @Action("get-release-status")
    void getReleaseStatus(Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) {
        commands.notice("Extracting release status information...");

        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issueCommentPayload.getIssue().getBody());

        ReleaseStatus releaseStatus = issues.extractReleaseStatus(updatedIssueBody);

        commands.setOutput(Outputs.STATUS, releaseStatus.getStatus().name());
        commands.setOutput(Outputs.CURRENT_STEP, releaseStatus.getCurrentStep().name());
        commands.setOutput(Outputs.CURRENT_STEP_STATUS, releaseStatus.getCurrentStepStatus().name());
        commands.setOutput(Outputs.WORKFLOW_RUN_ID, releaseStatus.getWorkflowRunId().toString());
        commands.setOutput(Outputs.DATE, releaseStatus.getDate().toString());
    }
}
