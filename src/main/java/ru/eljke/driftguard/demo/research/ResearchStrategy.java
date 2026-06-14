package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;

public enum ResearchStrategy {
    AGGRESSIVE(DetectorSensitivityProfile.AGGRESSIVE),
    BALANCED(DetectorSensitivityProfile.BALANCED),
    CONSERVATIVE(DetectorSensitivityProfile.CONSERVATIVE),
    ADAPTIVE(null);

    private final DetectorSensitivityProfile fixedProfile;

    ResearchStrategy(DetectorSensitivityProfile fixedProfile) {
        this.fixedProfile = fixedProfile;
    }

    public DetectorSensitivityProfile fixedProfile() {
        if (fixedProfile == null) {
            throw new IllegalStateException("Adaptive strategy requires a calibrated selector");
        }
        return fixedProfile;
    }

    public static java.util.List<ResearchStrategy> fixed() {
        return java.util.List.of(AGGRESSIVE, BALANCED, CONSERVATIVE);
    }
}
