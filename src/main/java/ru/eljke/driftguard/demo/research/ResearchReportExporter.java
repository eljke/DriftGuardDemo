package ru.eljke.driftguard.demo.research;

import java.util.Locale;
import java.util.stream.Collectors;

public final class ResearchReportExporter {
    private ResearchReportExporter() {
    }

    public static String csv(ResearchExperimentReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append("scenario,strategy,trials,precision,recall,f1,f1_ci_low,f1_ci_high,")
                .append("false_positives_per_1000,delay_samples,detection_rate,selected_profiles\n");
        for (ResearchAggregate result : report.aggregates()) {
            csv.append(result.scenario()).append(',')
                    .append(result.strategy()).append(',')
                    .append(result.trials()).append(',')
                    .append(decimal(result.meanPrecision())).append(',')
                    .append(decimal(result.meanRecall())).append(',')
                    .append(decimal(result.meanF1())).append(',')
                    .append(decimal(result.f1ConfidenceLow())).append(',')
                    .append(decimal(result.f1ConfidenceHigh())).append(',')
                    .append(decimal(result.meanFalsePositiveEventsPerThousand())).append(',')
                    .append(decimal(result.meanDetectionDelaySamples())).append(',')
                    .append(decimal(result.detectionRate())).append(',')
                    .append('"').append(profileCounts(result)).append('"')
                    .append('\n');
        }
        return csv.toString();
    }

    public static String markdown(ResearchExperimentReport report) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# DriftGuard Research Experiment\n\n")
                .append("## Hypothesis\n\n")
                .append(report.hypothesis()).append("\n\n")
                .append("## Method\n\n")
                .append(report.method()).append("\n\n")
                .append("- Trials: ").append(report.totalTrials()).append('\n')
                .append("- Repetitions: ").append(report.request().repetitions()).append('\n')
                .append("- Samples per stream: ").append(report.request().samples()).append('\n')
                .append("- Base seed: ").append(report.request().baseSeed()).append("\n\n")
                .append("| Scenario | Strategy | F1 | 95% CI | Precision | Recall | FP/1000 | Delay | Detection rate |\n")
                .append("|---|---|---:|---:|---:|---:|---:|---:|---:|\n");
        for (ResearchAggregate result : report.aggregates()) {
            markdown.append("| ").append(result.scenario())
                    .append(" | ").append(result.strategy())
                    .append(" | ").append(percent(result.meanF1()))
                    .append(" | ").append(percent(result.f1ConfidenceLow()))
                    .append("-").append(percent(result.f1ConfidenceHigh()))
                    .append(" | ").append(percent(result.meanPrecision()))
                    .append(" | ").append(percent(result.meanRecall()))
                    .append(" | ").append(decimal(result.meanFalsePositiveEventsPerThousand()))
                    .append(" | ").append(decimal(result.meanDetectionDelaySamples()))
                    .append(" | ").append(percent(result.detectionRate()))
                    .append(" |\n");
        }
        return markdown.toString();
    }

    private static String profileCounts(ResearchAggregate result) {
        return result.selectedProfiles().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String percent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }
}
