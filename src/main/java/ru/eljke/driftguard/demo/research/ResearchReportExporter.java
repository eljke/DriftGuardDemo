package ru.eljke.driftguard.demo.research;

import java.util.Locale;
import java.util.stream.Collectors;

public final class ResearchReportExporter {
    private ResearchReportExporter() {
    }

    public static String csv(ResearchExperimentReport report) {
        StringBuilder csv = new StringBuilder();
        csv.append("row_type,scope,strategy,baseline_profile,trials_or_pairs,precision,recall,f1,f1_ci_low,f1_ci_high,")
                .append("false_positives_per_1000,delay_samples,detection_rate,specificity,")
                .append("false_alarm_free_rate,mean_time_to_false_alarm_samples,mean_delta,delta_ci_low,")
                .append("delta_ci_high,relative_improvement_percent,wilcoxon_p,wins,losses,ties,selected_profiles\n");
        for (ResearchAggregate result : report.aggregates()) {
            csv.append("aggregate,")
                    .append(result.scenario()).append(',')
                    .append(result.strategy()).append(',')
                    .append(',')
                    .append(result.trials()).append(',')
                    .append(decimal(result.meanPrecision())).append(',')
                    .append(decimal(result.meanRecall())).append(',')
                    .append(decimal(result.meanF1())).append(',')
                    .append(decimal(result.f1ConfidenceLow())).append(',')
                    .append(decimal(result.f1ConfidenceHigh())).append(',')
                    .append(decimal(result.meanFalsePositiveEventsPerThousand())).append(',')
                    .append(decimal(result.meanDetectionDelaySamples())).append(',')
                    .append(decimal(result.detectionRate())).append(',')
                    .append(decimal(result.meanSpecificity())).append(',')
                    .append(decimal(result.falseAlarmFreeRate())).append(',')
                    .append(decimal(result.meanTimeToFirstFalseAlarmSamples())).append(',')
                    .append("N/A,N/A,N/A,N/A,N/A,N/A,N/A,")
                    .append('"').append(profileCounts(result)).append('"')
                    .append('\n');
        }
        for (ResearchComparison comparison : report.comparisons()) {
            csv.append("comparison,")
                    .append(comparison.scope()).append(',')
                    .append("ADAPTIVE,")
                    .append(comparison.baselineProfile()).append(',')
                    .append(comparison.pairs()).append(',')
                    .append("N/A,N/A,N/A,N/A,N/A,N/A,N/A,N/A,N/A,N/A,N/A,")
                    .append(decimal(comparison.meanDelta())).append(',')
                    .append(decimal(comparison.confidenceLow())).append(',')
                    .append(decimal(comparison.confidenceHigh())).append(',')
                    .append(decimal(comparison.relativeImprovementPercent())).append(',')
                    .append(decimal(comparison.wilcoxonPValue())).append(',')
                    .append(comparison.adaptiveWins()).append(',')
                    .append(comparison.adaptiveLosses()).append(',')
                    .append(comparison.ties()).append(',')
                    .append("\"\"")
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
                .append("- Calibration repetitions: ").append(report.calibration().calibrationRepetitions()).append('\n')
                .append("- Hold-out repetitions: ").append(report.calibration().holdoutRepetitions()).append('\n')
                .append("- Calibration trials: ").append(report.calibration().calibrationTrials()).append('\n')
                .append("- Training examples: ").append(report.calibration().trainingExamples()).append('\n')
                .append("- Calibration-selected global baseline: ").append(report.calibration().bestGlobalProfile()).append('\n')
                .append("- Samples per stream: ").append(report.request().samples()).append('\n')
                .append("- Base seed: ").append(report.request().baseSeed()).append("\n\n")
                .append("| Scenario | Strategy | F1 | 95% CI | Precision | Recall | FP/1000 | Delay | Detection rate | Specificity | Alarm-free | Mean time to false alarm |\n")
                .append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (ResearchAggregate result : report.aggregates()) {
            markdown.append("| ").append(result.scenario())
                    .append(" | ").append(result.strategy())
                    .append(" | ").append(percent(result.meanF1()))
                    .append(" | ").append(interval(result.f1ConfidenceLow(), result.f1ConfidenceHigh()))
                    .append(" | ").append(percent(result.meanPrecision()))
                    .append(" | ").append(percent(result.meanRecall()))
                    .append(" | ").append(decimal(result.meanFalsePositiveEventsPerThousand()))
                    .append(" | ").append(decimal(result.meanDetectionDelaySamples()))
                    .append(" | ").append(percent(result.detectionRate()))
                    .append(" | ").append(percent(result.meanSpecificity()))
                    .append(" | ").append(percent(result.falseAlarmFreeRate()))
                    .append(" | ").append(decimal(result.meanTimeToFirstFalseAlarmSamples()))
                    .append(" |\n");
        }
        markdown.append("\n## Paired adaptive comparison\n\n")
                .append("| Scope | Baseline | Pairs | Baseline utility | Adaptive utility | Mean delta | Relative improvement | Bootstrap 95% CI | Wilcoxon p | Wins | Losses | Ties |\n")
                .append("|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (ResearchComparison comparison : report.comparisons()) {
            markdown.append("| ").append(comparison.scope())
                    .append(" | ").append(comparison.baselineProfile())
                    .append(" | ").append(comparison.pairs())
                    .append(" | ").append(decimal(comparison.meanBaselineUtility()))
                    .append(" | ").append(decimal(comparison.meanAdaptiveUtility()))
                    .append(" | ").append(decimal(comparison.meanDelta()))
                    .append(" | ").append(decimal(comparison.relativeImprovementPercent())).append("%")
                    .append(" | ").append(decimal(comparison.confidenceLow()))
                    .append("-").append(decimal(comparison.confidenceHigh()))
                    .append(" | ").append(decimal(comparison.wilcoxonPValue()))
                    .append(" | ").append(comparison.adaptiveWins())
                    .append(" | ").append(comparison.adaptiveLosses())
                    .append(" | ").append(comparison.ties())
                    .append(" |\n");
        }
        return markdown.toString();
    }

    private static String profileCounts(ResearchAggregate result) {
        return result.selectedProfiles().entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(";"));
    }

    private static String decimal(Double value) {
        if (value == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private static String percent(Double value) {
        if (value == null) {
            return "N/A";
        }
        return String.format(Locale.ROOT, "%.1f%%", value * 100.0);
    }

    private static String interval(Double low, Double high) {
        return low == null ? "N/A" : percent(low) + "-" + percent(high);
    }
}
