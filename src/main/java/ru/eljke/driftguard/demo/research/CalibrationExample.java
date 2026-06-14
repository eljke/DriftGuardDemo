package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.algorithms.adaptive.BaselineCharacteristics;
import ru.eljke.driftguard.algorithms.adaptive.DetectorSensitivityProfile;

public record CalibrationExample(
        BaselineCharacteristics characteristics,
        DetectorSensitivityProfile bestProfile
) {
}
