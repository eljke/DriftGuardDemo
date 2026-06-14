package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

public record StreamCharacteristics(
        double mean,
        double standardDeviation,
        double coefficientOfVariation,
        double lagOneAutocorrelation,
        double median,
        double medianAbsoluteDeviationRatio,
        double normalizedTrendSlope,
        double outlierRate
) {
    public static StreamCharacteristics fromBaseline(List<MetricPoint> points) {
        int size = Math.max(8, Math.min(points.size(), points.size() / 4));
        List<Double> values = points.subList(0, size).stream()
                .map(MetricPoint::value)
                .toList();
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(value -> Math.pow(value - mean, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);
        double coefficientOfVariation = Math.abs(mean) < 1.0e-12
                ? standardDeviation
                : standardDeviation / Math.abs(mean);
        double median = median(values);
        double medianAbsoluteDeviation = median(values.stream()
                .map(value -> Math.abs(value - median))
                .toList());
        double scale = Math.max(Math.abs(median), 1.0e-12);
        return new StreamCharacteristics(
                mean,
                standardDeviation,
                coefficientOfVariation,
                lagOneAutocorrelation(values, mean, variance),
                median,
                medianAbsoluteDeviation / scale,
                trendSlope(values) / scale,
                outlierRate(values, median, medianAbsoluteDeviation)
        );
    }

    public double[] featureVector() {
        return new double[] {
                signedLogMagnitude(mean),
                Math.log1p(standardDeviation),
                coefficientOfVariation,
                lagOneAutocorrelation,
                medianAbsoluteDeviationRatio,
                normalizedTrendSlope,
                outlierRate
        };
    }

    private static double signedLogMagnitude(double value) {
        return Math.copySign(Math.log1p(Math.abs(value)), value);
    }

    private static double lagOneAutocorrelation(List<Double> values, double mean, double variance) {
        if (values.size() < 3 || variance < 1.0e-12) {
            return 0.0;
        }
        double covariance = 0.0;
        for (int index = 1; index < values.size(); index++) {
            covariance += (values.get(index - 1) - mean) * (values.get(index) - mean);
        }
        return covariance / ((values.size() - 1) * variance);
    }

    private static double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int middle = sorted.size() / 2;
        return sorted.size() % 2 == 0
                ? (sorted.get(middle - 1) + sorted.get(middle)) / 2.0
                : sorted.get(middle);
    }

    private static double trendSlope(List<Double> values) {
        double center = (values.size() - 1) / 2.0;
        double numerator = 0.0;
        double denominator = 0.0;
        for (int index = 0; index < values.size(); index++) {
            double centeredIndex = index - center;
            numerator += centeredIndex * values.get(index);
            denominator += centeredIndex * centeredIndex;
        }
        return numerator / denominator;
    }

    private static double outlierRate(List<Double> values, double median, double medianAbsoluteDeviation) {
        if (medianAbsoluteDeviation < 1.0e-12) {
            return 0.0;
        }
        double robustScale = 1.4826 * medianAbsoluteDeviation;
        return values.stream()
                .filter(value -> Math.abs(value - median) > 3.0 * robustScale)
                .count() / (double) values.size();
    }
}
