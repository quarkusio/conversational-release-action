package io.quarkus.bot.release;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Outputs;

public class PostInteractionCommentAction {

    @Action("post-interaction-comment")
    void postInteractionComment(Inputs inputs, @Issue.Opened GHEventPayload.Issue issuePayload) throws IOException {
        postInteractionComment(inputs, issuePayload.getIssue());
    }

    @Action("post-interaction-comment")
    void postInteractionComment(Inputs inputs, @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload)
            throws IOException {
        postInteractionComment(inputs, issueCommentPayload.getIssue());
    }

    private void postInteractionComment(Inputs inputs, GHIssue issue) throws IOException {
        Optional<String> interactionCommentInput = inputs.get(Outputs.INTERACTION_COMMENT);
        if (interactionCommentInput.isEmpty() || interactionCommentInput.get().isBlank()) {
            return;
        }

        issue.comment(interactionCommentInput.get());
    }
}