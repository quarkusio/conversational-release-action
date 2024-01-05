package io.quarkus.bot.release;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepStatus;

public class ReleaseStatus {

    private final Status status;
    private final Step currentStep;
    private final StepStatus currentStepStatus;
    private final Long workflowRunId;
    private final Instant date;

    @JsonCreator
    public ReleaseStatus(Status status, Step currentStep, StepStatus currentStepStatus, Long workflowRunId) {
        this.status = status;
        this.currentStep = currentStep;
        this.currentStepStatus = currentStepStatus;
        this.workflowRunId = workflowRunId;
        this.date = Instant.now();
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

    @JsonIgnore
    public ReleaseStatus progress(Long workflowRunId) {
        return new ReleaseStatus(this.status, this.currentStep, this.currentStepStatus, workflowRunId);
    }

    @JsonIgnore
    public ReleaseStatus progress(Step updatedStep) {
        return new ReleaseStatus(this.status, updatedStep, StepStatus.INIT, this.workflowRunId);
    }

    @JsonIgnore
    public ReleaseStatus progress(StepStatus updatedStepStatus) {
        return new ReleaseStatus(this.status, this.currentStep, updatedStepStatus, this.workflowRunId);
    }

    @JsonIgnore
    public ReleaseStatus progress(Status status, StepStatus updatedStepStatus) {
        return new ReleaseStatus(status, this.currentStep, updatedStepStatus, this.workflowRunId);
    }

    @JsonIgnore
    public ReleaseStatus progress(Status status) {
        return new ReleaseStatus(status, this.currentStep, this.currentStepStatus, this.workflowRunId);
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
                + workflowRunId + "]";
    }
}
