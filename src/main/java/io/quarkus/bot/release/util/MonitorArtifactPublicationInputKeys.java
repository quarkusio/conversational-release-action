package io.quarkus.bot.release.util;

public class MonitorArtifactPublicationInputKeys {

    public static final String GROUP_ID = "group-id";
    public static final String ARTIFACT_ID = "artifact-id";
    public static final String VERSION = "version";
    public static final String ISSUE_NUMBER = "issue-number";
    public static final String MESSAGE_IF_PUBLISHED = "message-if-published";
    public static final String MESSAGE_IF_NOT_PUBLISHED = "message-if-not-published";
    public static final String INITIAL_DELAY = "initial-delay";
    public static final String POLL_DELAY = "poll-delay";
    public static final String POLL_ITERATIONS = "poll-iterations";
    public static final String POST_DELAY = "post-delay";

    private MonitorArtifactPublicationInputKeys() {
    }
}
