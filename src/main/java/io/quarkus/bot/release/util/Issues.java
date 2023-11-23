package io.quarkus.bot.release.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.util.UtilsProducer.Yaml;

@Singleton
public final class Issues {

    private static final String BRANCH = "### Branch";
    private static final String QUALIFIER = "### Qualifier";
    private static final String MAJOR_VERSION = "### Major version";
    private static final String NO_RESPONSE = "_No response_";

    private static final String RELEASE_INFORMATION_MARKER = "<!-- quarkus-release/release-information:";
    private static final String RELEASE_STATUS_MARKER = "<!-- quarkus-release/release-status:";
    private static final String END_OF_MARKER = "-->";

    private static final Pattern RELEASE_INFORMATION_PATTERN = Pattern.compile(RELEASE_INFORMATION_MARKER + "\n(.*?)\n" + END_OF_MARKER, Pattern.DOTALL);
    private static final Pattern RELEASE_STATUS_PATTERN = Pattern.compile(RELEASE_STATUS_MARKER + "\n(.*?)\n" + END_OF_MARKER, Pattern.DOTALL);

    @Inject
    @Yaml
    ObjectMapper objectMapper;

    public ReleaseInformation extractReleaseInformationFromForm(String description) {
        String branch = null;
        String qualifier = null;
        boolean major = false;

        boolean inBranch = false;
        boolean inQualifier = false;
        boolean inMajor = false;

        for (String line : description.lines().map(String::trim).collect(Collectors.toList())) {
            if (line.isBlank()) {
                continue;
            }
            if (BRANCH.equals(line)) {
                inBranch = true;
                continue;
            }
            if (QUALIFIER.equals(line)) {
                inQualifier = true;
                continue;
            }
            if (MAJOR_VERSION.equals(line)) {
                inMajor = true;
                continue;
            }
            if (inBranch) {
                branch = line;
                inBranch = false;
                continue;
            }
            if (inQualifier) {
                qualifier = NO_RESPONSE.equals(line) ? null : line;
                inQualifier = false;
                continue;
            }
            if (inMajor) {
                major = line.contains("[X]");
                inMajor = false;
                break;
            }
        }

        if (branch == null) {
            throw new IllegalStateException("Unable to extract a branch from the description");
        }

        return new ReleaseInformation(null, branch, qualifier, major);
    }

    public ReleaseInformation extractReleaseInformation(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Unable to extract release information as body is empty");
        }

        Matcher releaseInformationMatcher = RELEASE_INFORMATION_PATTERN.matcher(body);
        if (!releaseInformationMatcher.find()) {
            throw new IllegalStateException("Invalid release information in body:\n" + body);
        }

        try {
            return objectMapper.readValue(releaseInformationMatcher.group(1), ReleaseInformation.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid release information in body:\n" + releaseInformationMatcher.group(1), e);
        }
    }

    public String appendReleaseInformation(String body, ReleaseInformation releaseInformation) {
        try {
            String descriptor = RELEASE_INFORMATION_MARKER + "\n" + objectMapper.writeValueAsString(releaseInformation) + END_OF_MARKER;

            if (body == null || !body.contains(RELEASE_INFORMATION_MARKER)) {
                return (body != null ? body + "\n\n" : "") + descriptor;
            }

            return RELEASE_INFORMATION_PATTERN.matcher(body).replaceFirst(descriptor);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to update the release information descriptor", e);
        }
    }

    public ReleaseStatus extractReleaseStatus(String body) {
        if (body == null || body.isBlank()) {
            throw new IllegalStateException("Invalid release status in body:\n" + body);
        }

        Matcher releaseStatusMatcher = RELEASE_STATUS_PATTERN.matcher(body);
        if (!releaseStatusMatcher.find()) {
            throw new IllegalStateException("Invalid release status in body:\n" + body);
        }

        try {
            return objectMapper.readValue(releaseStatusMatcher.group(1), ReleaseStatus.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid release information in body:\n" + releaseStatusMatcher.group(1), e);
        }
    }

    public String appendReleaseStatus(String body, ReleaseStatus releaseStatus) {
        try {
            String descriptor = RELEASE_STATUS_MARKER + "\n" + objectMapper.writeValueAsString(releaseStatus) + END_OF_MARKER;

            if (body == null || !body.contains(RELEASE_STATUS_MARKER)) {
                return (body != null ? body + "\n\n" : "") + descriptor;
            }

            return RELEASE_STATUS_PATTERN.matcher(body).replaceFirst(descriptor);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to update the release status descriptor", e);
        }
    }
}
