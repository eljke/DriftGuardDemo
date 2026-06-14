package ru.eljke.driftguard.demo.research;

import ru.eljke.driftguard.demo.detection.DemoDetectorProfile;

public record CalibrationExample(
        StreamCharacteristics characteristics,
        DemoDetectorProfile bestProfile
) {
}
