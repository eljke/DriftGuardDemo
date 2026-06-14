package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

public enum ResearchStrategy {
    AGGRESSIVE(DemoDetectorProfile.AGGRESSIVE),
    BALANCED(DemoDetectorProfile.BALANCED),
    CONSERVATIVE(DemoDetectorProfile.CONSERVATIVE),
    ADAPTIVE(null);

    private final DemoDetectorProfile fixedProfile;

    ResearchStrategy(DemoDetectorProfile fixedProfile) {
        this.fixedProfile = fixedProfile;
    }

    public DemoDetectorProfile fixedProfile() {
        if (fixedProfile == null) {
            throw new IllegalStateException("Adaptive strategy requires a calibrated selector");
        }
        return fixedProfile;
    }

    public static java.util.List<ResearchStrategy> fixed() {
        return java.util.List.of(AGGRESSIVE, BALANCED, CONSERVATIVE);
    }
}
