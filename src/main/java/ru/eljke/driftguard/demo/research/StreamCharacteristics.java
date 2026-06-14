package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.core.domain.MetricPoint;

import java.util.List;

public record StreamCharacteristics(
        double mean,
        double standardDeviation,
        double coefficientOfVariation,
        double lagOneAutocorrelation
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
        return new StreamCharacteristics(
                mean,
                standardDeviation,
                coefficientOfVariation,
                lagOneAutocorrelation(values, mean, variance)
        );
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
}
