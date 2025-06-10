package io.quarkus.bot.release;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.bot.release.util.Branches;
import io.quarkus.bot.release.util.Versions;

public class ReleaseInformation {

    private final String branch;
    private final String originBranch;
    private final String qualifier;
    private final boolean emergency;
    private final boolean major;

    private String version;
    private boolean firstFinal;
    private boolean maintenance;

    @JsonCreator
    public ReleaseInformation(String version, String branch, String originBranch, String qualifier, boolean emergency,
            boolean major, boolean firstFinal, boolean maintenance) {
        checkConsistency(branch, qualifier, emergency, major);

        this.version = version;
        this.branch = branch;
        this.originBranch = originBranch;
        this.qualifier = qualifier;
        this.emergency = emergency;
        this.major = major;
        this.firstFinal = firstFinal;
        this.maintenance = maintenance;
    }

    public String getVersion() {
        return version;
    }

    /**
     * @return the version appended with " LTS" if this is an LTS release, the version otherwise
     */
    @JsonIgnore
    public String getFullVersion() {
        if (Branches.isLts(branch)) {
            return version + " LTS";
        }

        return version;
    }

    public String getBranch() {
        return branch;
    }

    /**
     * @return the branch appended with " LTS" if this is an LTS release, the branch otherwise
     */
    @JsonIgnore
    public String getFullBranch() {
        if (Branches.isLts(branch)) {
            return branch + " LTS";
        }

        return branch;
    }

    /**
     * @return the branch from which we branch the CR1 release - it is main except when preparing the CR1 of an LTS release, in
     *         which case it is the previous version branch as we want to branch CR1 from the previous version branch. For any
     *         other release than the CR1, this is not consumed.
     */
    public String getOriginBranch() {
        return originBranch;
    }

    /**
     * @return the qualifier (Alpha1, CR1, CR2...). Will be {@code null} for final releases. See {@link #isFinal()}.
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
     * @return whether it's a maintenance release, meaning this release is for a branch that is older than the latest
     *         branch for which we have a final release.
     *         For instance, if the latest branch for which we have a final release is 3.17 (we released 3.17.0 Platform), 3.16
     *         is considered a maintenance branch, together will all previous branches).
     */
    public boolean isMaintenance() {
        return maintenance;
    }

    public void setVersion(String version, boolean firstFinal, boolean maintenance) {
        this.version = version;
        this.firstFinal = firstFinal;
        this.maintenance = maintenance;

        if (firstFinal && emergency) {
            throw new IllegalStateException("An emergency release may not be the first final release of a branch");
        }
    }

    @JsonIgnore
    public boolean isComplete() {
        return version != null;
    }

    /**
     * @return whether this version is the first final of a given branch, it will be .0 in most cases but can be .1 if we didn't
     *         release the .0 Platform (which happens when we find a critical bug in the core release between the initial .0
     *         release and the full Platform release)
     */
    public boolean isFirstFinal() {
        return firstFinal;
    }

    /**
     * @return whether this version is a final release (e.g. not an alpha or a CR)
     */
    @JsonIgnore
    public boolean isFinal() {
        return qualifier == null || qualifier.isBlank() || qualifier.equals("Final");
    }

    /**
     * @return whether this version is a candidate release
     */
    @JsonIgnore
    public boolean isCR() {
        return qualifier != null && qualifier.startsWith("CR");
    }

    /**
     * @return whether this version is the .0. Be careful, it doesn't mean it's the first final (see {@link #isFirstFinal()}).
     */
    @JsonIgnore
    public boolean isDot0() {
        if (version == null) {
            throw new IllegalStateException("Unable to know if the version is the .0 at this stage");
        }

        return Versions.isDot0(version);
    }

    /**
     * @return whether this version is the CR1
     */
    @JsonIgnore
    public boolean isFirstCR() {
        return "CR1".equalsIgnoreCase(qualifier);
    }

    /**
     * @return whether this is a major new release (e.g. 4.0.0.CR1)
     */
    public boolean isMajor() {
        return major;
    }

    /**
     * @return whether this is an emergency release (e.g. 3.17.7.1). Emergency releases are only used for LTS branches with
     *         regular release cadence.
     */
    public boolean isEmergency() {
        return emergency;
    }

    /**
     * @return whether the origin branch for creating the branch is the main branch (see {@link #getOriginBranch()} for more
     *         details)
     */
    @JsonIgnore
    public boolean isOriginBranchMain() {
        return Branches.MAIN.equals(originBranch);
    }

    /**
     * @return whether this version is an LTS maintenance release with the regular release cadence (so a release from a LTS
     *         branch, not a first final, and post 3.15 included)
     */
    @JsonIgnore
    public boolean isLtsMaintenanceReleaseWithRegularReleaseCadence() {
        if (version == null) {
            throw new IllegalStateException(
                    "Unable to know if the version is a LTS maintenance release with regular release cadence at this stage");
        }

        return Branches.isLtsBranchWithRegularReleaseCadence(branch) && isFinal() && !isFirstFinal();
    }

    private static void checkConsistency(String branch, String qualifier, boolean emergency, boolean major) {
        if (emergency) {
            if (!Branches.isLtsBranchWithRegularReleaseCadence(branch)) {
                throw new IllegalStateException(
                        "Emergency releases are only supported for LTS branches with regular release cadence.");
            }
            if (major) {
                throw new IllegalStateException("A release may not be both an emergency and a major release");
            }
            if (qualifier != null) {
                throw new IllegalStateException("An emergency release may not have a qualifier");
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(branch, qualifier, emergency, major);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReleaseInformation other = (ReleaseInformation) obj;
        return Objects.equals(version, this.version)
                && Objects.equals(branch, other.branch)
                && Objects.equals(originBranch, other.originBranch)
                && emergency == other.emergency
                && major == other.major
                && Objects.equals(qualifier, other.qualifier)
                && firstFinal == other.firstFinal
                && maintenance == other.maintenance;
    }

    @Override
    public String toString() {
        return "ReleaseInformation [version=" + version + ", branch=" + branch + ", originBranch=" + originBranch
                + ", qualifier=" + qualifier + ", emergency=" + emergency + ", major=" + major
                + ", firstFinal=" + firstFinal + ", maintenance=" + maintenance
                + "]";
    }
}
