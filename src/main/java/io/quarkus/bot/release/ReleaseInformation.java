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
    private final boolean major;

    private String version;
    private boolean firstFinal;
    private boolean maintenance;

    @JsonCreator
    public ReleaseInformation(String version, String branch, String originBranch, String qualifier, boolean major, boolean firstFinal, boolean maintenance) {
        this.version = version;
        this.branch = branch;
        this.originBranch = originBranch;
        this.qualifier = qualifier;
        this.major = major;
        this.firstFinal = firstFinal;
        this.maintenance = maintenance;
    }

    public String getVersion() {
        return version;
    }

    public String getBranch() {
        return branch;
    }

    public String getOriginBranch() {
        return originBranch;
    }

    public String getQualifier() {
        return qualifier;
    }

    public boolean isMaintenance() {
        return maintenance;
    }

    public void setVersion(String version, boolean firstFinal, boolean maintenance) {
        this.version = version;
        this.firstFinal = firstFinal;
        this.maintenance = maintenance;
    }

    @JsonIgnore
    public boolean isComplete() {
        return version != null;
    }

    public boolean isFirstFinal() {
        return firstFinal;
    }

    @JsonIgnore
    public boolean isFinal() {
        return qualifier == null || qualifier.isBlank() || qualifier.equals("Final");
    }

    @JsonIgnore
    public boolean isCR() {
        return qualifier != null && qualifier.startsWith("CR");
    }

    @JsonIgnore
    public boolean isDot0() {
        if (version == null) {
            throw new IllegalStateException("Unable to know if the version is the .0 at this stage");
        }

        return Versions.isDot0(version);
    }

    @JsonIgnore
    public boolean isFirstMicroMaintenanceRelease() {
        if (version == null) {
            throw new IllegalStateException("Unable to know if the version is the first micro maintenance at this stage");
        }

        return Versions.isFirstMicroMaintenanceRelease(version);
    }

    @JsonIgnore
    public boolean isFirstCR() {
        return "CR1".equalsIgnoreCase(qualifier);
    }

    public boolean isMajor() {
        return major;
    }

    @JsonIgnore
    public boolean isOriginBranchMain() {
        return Branches.MAIN.equals(originBranch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branch, major, qualifier);
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
                && major == other.major
                && Objects.equals(qualifier, other.qualifier)
                && firstFinal == other.firstFinal
                && maintenance == other.maintenance;
    }

    @Override
    public String toString() {
        return "ReleaseInformation [version=" + version + ", branch=" + branch + ", originBranch=" + originBranch
                + ", qualifier=" + qualifier + ", major=" + major
                + ",firstFinal=" + firstFinal + ",maintenance=" + maintenance
                + "]";
    }
}
