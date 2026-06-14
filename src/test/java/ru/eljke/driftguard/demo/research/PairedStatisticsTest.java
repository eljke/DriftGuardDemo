package ru.eljke.driftguard.demo.research;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PairedStatisticsTest {
    @Test
    void reportsConsistentPairedAdvantage() {
        PairedStatistics.Result result = PairedStatistics.analyze(
                new double[] {0.10, 0.15, 0.12, 0.08, 0.11, 0.14, 0.09, 0.13},
                42L
        );

        assertEquals(8, result.wins());
        assertEquals(0, result.losses());
        assertTrue(result.confidenceLow() > 0.0);
        assertTrue(result.pValue() < 0.05);
    }

    @Test
    void treatsEqualPairsAsNoEvidence() {
        PairedStatistics.Result result = PairedStatistics.analyze(new double[] {0.0, 0.0, 0.0}, 42L);

        assertEquals(3, result.ties());
        assertEquals(1.0, result.pValue());
    }
}
