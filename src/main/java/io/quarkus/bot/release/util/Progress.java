package io.quarkus.bot.release.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import io.quarkus.bot.release.ReleaseInformation;
import io.quarkus.bot.release.ReleaseStatus;
import io.quarkus.bot.release.step.Step;
import io.quarkus.bot.release.step.StepStatus;

public final class Progress {

    private Progress() {
    }

    public static String youAreHere(ReleaseInformation releaseInformation, ReleaseStatus releaseStatus) {
        if (!releaseInformation.isComplete()) {
            return "";
        }

        return "---\n\n<details><summary>Where am I?</summary>\n\n" +
                Arrays.stream(Step.values())
                        .filter(s -> releaseInformation.isFinal() || !s.isForFinalReleasesOnly())
                        .filter(s -> !s.getStepHandler().shouldSkip(releaseInformation, releaseStatus))
                        .map(s -> {
                            StringBuilder sb = new StringBuilder();
                            sb.append("[");
                            if (releaseStatus.getCurrentStep().ordinal() > s.ordinal() ||
                                    (releaseStatus.getCurrentStep() == s
                                            && (releaseStatus.getCurrentStepStatus() == StepStatus.COMPLETED
                                                    || releaseStatus.getCurrentStepStatus() == StepStatus.SKIPPED))) {
                                sb.append("X");
                            } else {
                                sb.append(" ");
                            }
                            sb.append("] ").append(s.getDescription());

                            if (releaseStatus.getCurrentStep() == s) {
                                sb.append(" ");
                                switch (releaseStatus.getCurrentStepStatus()) {
                                    case INIT:
                                    case STARTED:
                                        sb.append(":gear:");
                                        break;
                                    case COMPLETED:
                                        // should never happen
                                        sb.append(":white_check_mark:");
                                        break;
                                    case SKIPPED:
                                        // should never happen
                                        sb.append(":zzz:");
                                        break;
                                    case INIT_FAILED:
                                    case FAILED:
                                        sb.append(":rotating_light:");
                                        break;
                                    case PAUSED:
                                        sb.append(":pause_button:");
                                        break;
                                }
                                sb.append(" ☚ You are here");
                            } else if (releaseStatus.getCurrentStepStatus() == StepStatus.SKIPPED) {
                                sb.append(" :zzz:");
                            }
                            return sb.toString();
                        }).collect(Collectors.joining("\n- ", "- ", ""))
                + "</details>";
    }
}
