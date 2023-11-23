package io.quarkus.bot.release.step;

import java.io.IOException;

import org.kohsuke.github.GHIssue;

import io.quarkus.bot.release.ReleaseInformation;

public class CoreReleasePrepare implements StepHandler {

    @Override
    public int run(ReleaseInformation releaseInformation, GHIssue issue) throws IOException, InterruptedException {
        throw new IllegalStateException("Testing error handling...");
    }
}
