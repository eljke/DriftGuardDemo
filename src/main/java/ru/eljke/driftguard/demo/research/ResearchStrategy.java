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

    public DemoDetectorProfile profileFor(StreamCharacteristics characteristics) {
        if (fixedProfile != null) {
            return fixedProfile;
        }
        if (Math.abs(characteristics.lagOneAutocorrelation()) >= 0.65
                || characteristics.coefficientOfVariation() >= 0.12) {
            return DemoDetectorProfile.CONSERVATIVE;
        }
        if (characteristics.coefficientOfVariation() <= 0.04) {
            return DemoDetectorProfile.AGGRESSIVE;
        }
        return DemoDetectorProfile.BALANCED;
    }
}
