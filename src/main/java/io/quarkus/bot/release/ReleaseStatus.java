package io.quarkus.bot.release;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepStatus;

public class ReleaseStatus {

    private final Status status;
    private final Step currentStep;
    private final StepStatus currentStepStatus;
    private final Long workflowRunId;
    private final Instant date;
    private final Map<String, String> properties;

    public ReleaseStatus(Status status, Step currentStep, StepStatus currentStepStatus, Long workflowRunId) {
        this(status, currentStep, currentStepStatus, workflowRunId, null);
    }

    @JsonCreator
    public ReleaseStatus(
            @JsonProperty("status") Status status,
            @JsonProperty("currentStep") Step currentStep,
            @JsonProperty("currentStepStatus") StepStatus currentStepStatus,
            @JsonProperty("workflowRunId") Long workflowRunId,
            @JsonProperty("properties") Map<String, String> properties) {
        this.status = status;
        this.currentStep = currentStep;
        this.currentStepStatus = currentStepStatus;
        this.workflowRunId = workflowRunId;
        this.date = Instant.now();
        this.properties = properties != null ? Map.copyOf(properties) : Map.of();
    }

    public Status getStatus() {
        return status;
    }

    public Step getCurrentStep() {
        return currentStep;
    }

    public StepStatus getCurrentStepStatus() {
        return currentStepStatus;
    }

    public Long getWorkflowRunId() {
        return workflowRunId;
    }

    public Instant getDate() {
        return date;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    @JsonIgnore
    public String getProperty(String key) {
        return properties.get(key);
    }

    @JsonIgnore
    public ReleaseStatus withProperty(String key, String value) {
        Map<String, String> updatedProperties = new HashMap<>(this.properties);
        updatedProperties.put(key, value);
        return new ReleaseStatus(this.status, this.currentStep, this.currentStepStatus, this.workflowRunId,
                updatedProperties);
    }

    @JsonIgnore
    public ReleaseStatus progress(Long workflowRunId) {
        return new ReleaseStatus(this.status, this.currentStep, this.currentStepStatus, workflowRunId, this.properties);
    }

    @JsonIgnore
    public ReleaseStatus progress(Step updatedStep) {
        return new ReleaseStatus(this.status, updatedStep, StepStatus.INIT, this.workflowRunId, this.properties);
    }

    @JsonIgnore
    public ReleaseStatus progress(StepStatus updatedStepStatus) {
        return new ReleaseStatus(this.status, this.currentStep, updatedStepStatus, this.workflowRunId, this.properties);
    }

    @JsonIgnore
    public ReleaseStatus progress(Status status, StepStatus updatedStepStatus) {
        return new ReleaseStatus(status, this.currentStep, updatedStepStatus, this.workflowRunId, this.properties);
    }

    @JsonIgnore
    public ReleaseStatus progress(Status status) {
        return new ReleaseStatus(status, this.currentStep, this.currentStepStatus, this.workflowRunId, this.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentStep, currentStepStatus, workflowRunId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReleaseStatus other = (ReleaseStatus) obj;
        return currentStep == other.currentStep && currentStepStatus == other.currentStepStatus
                && Objects.equals(workflowRunId, other.workflowRunId);
    }

    @Override
    public String toString() {
        return "ReleaseStatus [currentStep=" + currentStep + ", currentStepStatus=" + currentStepStatus + ", workflowRunId="
                + workflowRunId + ", properties=" + properties + "]";
    }
}
