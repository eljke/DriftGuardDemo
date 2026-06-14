package ru.eljke.driftguard.demo.research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

final class PairedStatistics {
    private static final int BOOTSTRAP_SAMPLES = 5_000;
    private static final double EPSILON = 1.0e-12;

    private PairedStatistics() {
    }

    static Result analyze(double[] differences, long seed) {
        double mean = Arrays.stream(differences).average().orElseThrow();
        double[] bootstrapMeans = new double[BOOTSTRAP_SAMPLES];
        SplittableRandom random = new SplittableRandom(seed);
        for (int sample = 0; sample < BOOTSTRAP_SAMPLES; sample++) {
            double sum = 0.0;
            for (int index = 0; index < differences.length; index++) {
                sum += differences[random.nextInt(differences.length)];
            }
            bootstrapMeans[sample] = sum / differences.length;
        }
        Arrays.sort(bootstrapMeans);
        int wins = (int) Arrays.stream(differences).filter(value -> value > EPSILON).count();
        int losses = (int) Arrays.stream(differences).filter(value -> value < -EPSILON).count();
        return new Result(
                mean,
                percentile(bootstrapMeans, 0.025),
                percentile(bootstrapMeans, 0.975),
                wilcoxonPValue(differences),
                wins,
                losses,
                differences.length - wins - losses
        );
    }

    private static double wilcoxonPValue(double[] differences) {
        List<SignedDifference> ranked = Arrays.stream(differences)
                .filter(value -> Math.abs(value) > EPSILON)
                .mapToObj(value -> new SignedDifference(Math.abs(value), Math.signum(value), 0.0))
                .sorted(Comparator.comparingDouble(SignedDifference::absolute))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        int count = ranked.size();
        if (count == 0) {
            return 1.0;
        }
        int start = 0;
        while (start < count) {
            int end = start + 1;
            while (end < count && Math.abs(ranked.get(end).absolute() - ranked.get(start).absolute()) < EPSILON) {
                end++;
            }
            double averageRank = (start + 1 + end) / 2.0;
            for (int index = start; index < end; index++) {
                SignedDifference value = ranked.get(index);
                ranked.set(index, new SignedDifference(value.absolute(), value.sign(), averageRank));
            }
            start = end;
        }
        double positiveRank = ranked.stream()
                .filter(value -> value.sign() > 0)
                .mapToDouble(SignedDifference::rank)
                .sum();
        double expected = count * (count + 1) / 4.0;
        double variance = count * (count + 1) * (2.0 * count + 1) / 24.0;
        double z = (Math.abs(positiveRank - expected) - 0.5) / Math.sqrt(variance);
        return Math.min(1.0, 2.0 * (1.0 - normalCdf(Math.max(0.0, z))));
    }

    private static double normalCdf(double value) {
        double t = 1.0 / (1.0 + 0.2316419 * value);
        double density = 0.3989422804014327 * Math.exp(-value * value / 2.0);
        double tail = density * t * (
                0.319381530 + t * (-0.356563782 + t * (1.781477937 + t * (-1.821255978 + t * 1.330274429)))
        );
        return 1.0 - tail;
    }

    private static double percentile(double[] sorted, double probability) {
        double position = probability * (sorted.length - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sorted[lower];
        }
        double fraction = position - lower;
        return sorted[lower] * (1.0 - fraction) + sorted[upper] * fraction;
    }

    record Result(
            double mean,
            double confidenceLow,
            double confidenceHigh,
            double pValue,
            int wins,
            int losses,
            int ties
    ) {
    }

    private record SignedDifference(double absolute, double sign, double rank) {
    }
}
