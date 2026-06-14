package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CalibratedProfileSelector {
    private static final int NEIGHBORS = 5;

    private final List<CalibrationExample> examples;
    private final double[] means;
    private final double[] scales;

    public CalibratedProfileSelector(List<CalibrationExample> examples) {
        this.examples = List.copyOf(examples);
        int features = examples.getFirst().characteristics().featureVector().length;
        means = new double[features];
        scales = new double[features];
        for (CalibrationExample example : examples) {
            double[] vector = example.characteristics().featureVector();
            for (int index = 0; index < features; index++) {
                means[index] += vector[index] / examples.size();
            }
        }
        for (CalibrationExample example : examples) {
            double[] vector = example.characteristics().featureVector();
            for (int index = 0; index < features; index++) {
                scales[index] += Math.pow(vector[index] - means[index], 2);
            }
        }
        for (int index = 0; index < features; index++) {
            scales[index] = Math.sqrt(scales[index] / examples.size());
            if (scales[index] < 1.0e-12) {
                scales[index] = 1.0;
            }
        }
    }

    public DemoDetectorProfile select(StreamCharacteristics characteristics) {
        double[] target = characteristics.featureVector();
        List<Neighbor> neighbors = examples.stream()
                .map(example -> new Neighbor(
                        example.bestProfile(),
                        distance(target, example.characteristics().featureVector())
                ))
                .sorted(Comparator.comparingDouble(Neighbor::distance))
                .limit(Math.min(NEIGHBORS, examples.size()))
                .toList();
        Map<DemoDetectorProfile, Double> votes = new EnumMap<>(DemoDetectorProfile.class);
        for (Neighbor neighbor : neighbors) {
            votes.merge(neighbor.profile(), 1.0 / (neighbor.distance() + 1.0e-9), Double::sum);
        }
        return Arrays.stream(DemoDetectorProfile.values())
                .max(Comparator.comparingDouble(profile -> votes.getOrDefault(profile, 0.0)))
                .orElseThrow();
    }

    private double distance(double[] left, double[] right) {
        double squared = 0.0;
        for (int index = 0; index < left.length; index++) {
            squared += Math.pow((left[index] - right[index]) / scales[index], 2);
        }
        return Math.sqrt(squared);
    }

    private record Neighbor(DemoDetectorProfile profile, double distance) {
    }
}
