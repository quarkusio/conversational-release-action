package io.quarkus.bot.release;

import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.Reactable;
import org.kohsuke.github.ReactionContent;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubapp.event.Issue;
import io.quarkiverse.githubapp.event.IssueComment;
import io.quarkus.bot.release.util.Users;

public class LookingAction {

    private static final Logger LOG = Logger.getLogger(LookingAction.class);

    @Action("looking")
    void looking(@Issue.Opened GHEventPayload.Issue issuePayload) {
        looking(issuePayload.getSender(), issuePayload.getIssue());
    }

    @Action("looking")
    void looking(@IssueComment.Created GHEventPayload.IssueComment issueCommentPayload) {
        looking(issueCommentPayload.getSender(), issueCommentPayload.getComment());
    }

    private void looking(GHUser sender, Reactable reactable) {
        if (Users.isIgnored(sender.getLogin())) {
            return;
        }

        try {
            reactable.createReaction(ReactionContent.EYES);
        } catch (Exception e) {
            // we can ignore it
            LOG.warn("Unable to add EYES reaction, ignoring", e);
        }
    }
}
