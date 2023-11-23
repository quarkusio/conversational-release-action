package io.quarkus.bot.release.step;

import java.io.IOException;

import jakarta.inject.Singleton;

import org.kohsuke.github.GHIssue;

import io.quarkus.arc.Unremovable;
import io.quarkus.bot.release.ReleaseInformation;

@Singleton
@Unremovable
public class CoreReleasePrepare implements StepHandler {

    @Override
    public int run(ReleaseInformation releaseInformation, GHIssue issue) throws IOException, InterruptedException {
        throw new IllegalStateException("Testing error handling...");
    }
}
