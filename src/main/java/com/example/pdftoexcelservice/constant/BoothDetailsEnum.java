package com.example.pdftoexcelservice.constant;

import java.text.Normalizer;


public enum BoothDetailsEnum {
    STATE("निर्वाचक नामावली"),
    BOOTH_NUMBER("भाग संख्या"),
    BOOTH_NAME("मतदान केंद्र की संख्या व नाम"),
    BOOTH_ADDRESS("मतदान केंद्र का पता"),
    CONSTITUENCY("जिला");

    private final String label;

    BoothDetailsEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    /**
     * Attempt to match a line to an enum heading.
     * If the line starts with the label (after normalization), return that enum.
     * Otherwise return null.
     */
    public static BoothDetailsEnum matchHeading(String line) {
        if (line == null) {
            return null;
        }
        String trimmed = line.trim();
        // Normalize to NFC (composed form) to reduce unicode variant issues
        trimmed = Normalizer.normalize(trimmed, Normalizer.Form.NFC);
        for (BoothDetailsEnum e : BoothDetailsEnum.values()) {
            String lbl = Normalizer.normalize(e.label, Normalizer.Form.NFC);
            if (trimmed.startsWith(lbl)) {
                return e;
            }
        }
        return null;
    }
}