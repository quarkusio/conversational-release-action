package io.quarkus.bot.release.util;

import java.util.Locale;
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
    private static final String EMERGENCY_RELEASE = "### Emergency release";
    private static final String MAJOR_VERSION = "### Major version";
    private static final String ORIGIN_BRANCH = "### Origin branch";
    private static final String NO_RESPONSE = "_No response_";

    private static final String RELEASE_INFORMATION_MARKER = "<!-- quarkus-release/release-information:";
    private static final String RELEASE_STATUS_MARKER = "<!-- quarkus-release/release-status:";
    private static final String END_OF_MARKER = "-->";

    private static final Pattern RELEASE_INFORMATION_PATTERN = Pattern
            .compile(RELEASE_INFORMATION_MARKER + "(.*?)" + END_OF_MARKER, Pattern.DOTALL);
    private static final Pattern RELEASE_STATUS_PATTERN = Pattern.compile(RELEASE_STATUS_MARKER + "(.*?)" + END_OF_MARKER,
            Pattern.DOTALL);

    @Inject
    @Yaml
    ObjectMapper objectMapper;

    public ReleaseInformation extractReleaseInformationFromForm(String description) {
        String branch = null;
        String qualifier = null;
        boolean major = false;
        boolean emergency = false;
        String originBranch = Branches.MAIN;

        boolean inBranch = false;
        boolean inQualifier = false;
        boolean inEmergencyRelease = false;
        boolean inMajor = false;
        boolean inOriginBranch = false;

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
            if (EMERGENCY_RELEASE.equals(line)) {
                inEmergencyRelease = true;
                continue;
            }
            if (MAJOR_VERSION.equals(line)) {
                inMajor = true;
                continue;
            }
            if (ORIGIN_BRANCH.equals(line)) {
                inOriginBranch = true;
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
            if (inEmergencyRelease) {
                emergency = line.toLowerCase(Locale.ROOT).contains("[x]");
                inEmergencyRelease = false;
                continue;
            }
            if (inOriginBranch) {
                originBranch = NO_RESPONSE.equals(line) ? Branches.MAIN : line;
                inOriginBranch = false;
                continue;
            }
            if (inMajor) {
                major = line.toLowerCase(Locale.ROOT).contains("[x]");
                inMajor = false;
                break;
            }
        }

        if (branch == null) {
            throw new IllegalStateException("Unable to extract a branch from the description");
        }

        return new ReleaseInformation(null, branch, originBranch, qualifier, emergency, major, false, false);
    }

    public ReleaseInformation extractReleaseInformation(UpdatedIssueBody updatedIssueBody) {
        if (updatedIssueBody.isBlank()) {
            throw new IllegalStateException("Unable to extract release information as body is empty");
        }

        Matcher releaseInformationMatcher = RELEASE_INFORMATION_PATTERN.matcher(updatedIssueBody.getBody());
        if (!releaseInformationMatcher.find()) {
            throw new IllegalStateException("Invalid release information in body:\n" + updatedIssueBody);
        }

        try {
            return objectMapper.readValue(releaseInformationMatcher.group(1), ReleaseInformation.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid release information in body:\n" + releaseInformationMatcher.group(1), e);
        }
    }

    public String appendReleaseInformation(UpdatedIssueBody updatedIssueBody, ReleaseInformation releaseInformation) {
        try {
            String descriptor = RELEASE_INFORMATION_MARKER + "\n" + objectMapper.writeValueAsString(releaseInformation)
                    + END_OF_MARKER;

            if (!updatedIssueBody.contains(RELEASE_INFORMATION_MARKER)) {
                return updatedIssueBody.append(descriptor);
            }

            return updatedIssueBody.replace(RELEASE_INFORMATION_PATTERN, descriptor);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to update the release information descriptor", e);
        }
    }

    public ReleaseStatus extractReleaseStatus(UpdatedIssueBody updatedIssueBody) {
        if (updatedIssueBody.isBlank()) {
            throw new IllegalStateException("Unable to extract release status as body is empty");
        }

        Matcher releaseStatusMatcher = RELEASE_STATUS_PATTERN.matcher(updatedIssueBody.getBody());
        if (!releaseStatusMatcher.find()) {
            throw new IllegalStateException("Invalid release status in body:\n" + updatedIssueBody);
        }

        try {
            return objectMapper.readValue(releaseStatusMatcher.group(1), ReleaseStatus.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid release information in body:\n" + releaseStatusMatcher.group(1), e);
        }
    }

    public String appendReleaseStatus(UpdatedIssueBody updatedIssueBody, ReleaseStatus releaseStatus) {
        try {
            String descriptor = RELEASE_STATUS_MARKER + "\n" + objectMapper.writeValueAsString(releaseStatus) + END_OF_MARKER;

            if (!updatedIssueBody.contains(RELEASE_STATUS_MARKER)) {
                return updatedIssueBody.append(descriptor);
            }

            return updatedIssueBody.replace(RELEASE_STATUS_PATTERN, descriptor);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to update the release status descriptor", e);
        }
    }
}
