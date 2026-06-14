package ru.eljke.driftguard.demo.detection;

import java.util.Locale;

/**
 * Predefined detector sensitivity profiles exposed by the demo UI.
 */
public enum DemoDetectorProfile {
    AGGRESSIVE,
    BALANCED,
    CONSERVATIVE,
    ADAPTIVE;

    public static DemoDetectorProfile parse(String value) {
        if (value == null || value.isBlank()) {
            return BALANCED;
        }
        return DemoDetectorProfile.valueOf(value.trim().replace('-', '_').toUpperCase(Locale.ROOT));
    }
}


