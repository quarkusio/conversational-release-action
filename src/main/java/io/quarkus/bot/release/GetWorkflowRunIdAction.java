package io.quarkus.bot.release;

import jakarta.inject.Inject;

import org.kohsuke.github.GHEventPayload;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Issues;
import io.quarkus.bot.release.util.Outputs;
import io.quarkus.bot.release.util.UpdatedIssueBody;

public class GetWorkflowRunIdAction {

    @Inject
    Issues issues;

    @Action("get-workflow-run-id")
    void getWorkflowRunId(Commands commands, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) {
        UpdatedIssueBody updatedIssueBody = new UpdatedIssueBody(issueCommentPayload.getIssue().getBody());

        commands.setOutput(Outputs.WORKFLOW_RUN_ID,
                issues.extractReleaseStatus(updatedIssueBody).getWorkflowRunId().toString());
    }
}
