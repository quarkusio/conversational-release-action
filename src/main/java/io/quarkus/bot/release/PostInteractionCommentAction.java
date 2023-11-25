package io.quarkus.bot.release;

import java.io.IOException;
import java.util.Optional;

import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHIssue;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Commands;
import io.quarkiverse.githubaction.Inputs;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Outputs;

public class PostInteractionCommentAction {

    @Action("post-interaction-comment")
    void postInteractionComment(Commands commands, Inputs inputs, @Issue.Opened GHEventPayload.Issue issuePayload)
            throws IOException {
        postInteractionComment(commands, inputs, issuePayload.getIssue());
    }

    @Action("post-interaction-comment")
    void postInteractionComment(Commands commands, Inputs inputs,
            @IssueComment.Created GHEventPayload.IssueComment issueCommentPayload)
            throws IOException {
        postInteractionComment(commands, inputs, issueCommentPayload.getIssue());
    }

    private void postInteractionComment(Commands commands, Inputs inputs, GHIssue issue) throws IOException {
        commands.notice("Posting interaction comment");

        Optional<String> interactionCommentInput = inputs.get(Outputs.INTERACTION_COMMENT);
        if (interactionCommentInput.isEmpty() || interactionCommentInput.get().isBlank()) {
            commands.warning("No " + Outputs.INTERACTION_COMMENT + " input, not posting interaction comment");
            return;
        }

        issue.comment(interactionCommentInput.get());
    }
}