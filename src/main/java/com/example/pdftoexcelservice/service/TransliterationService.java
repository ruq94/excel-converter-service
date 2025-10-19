package com.example.pdftoexcelservice.service;


import com.ibm.icu.text.Transliterator;
import org.springframework.stereotype.Service;

@Service
public class TransliterationService {

    private final Transliterator transliterator;

    public TransliterationService() {
        this.transliterator = Transliterator.getInstance("Devanagari-Latin");
    }

    /**
     * Transliterate Hindi text to English (phonetic)
     */
    public String transliterate(String hindiText) {
        if (hindiText == null || hindiText.isBlank()) return "";
        String result = transliterator.transliterate(hindiText);
        return clean(result);
    }

    /**
     * Optional: Clean output to plain ASCII (remove ā, ī, etc.)
     */
    private String clean(String text) {
        String cleanedtext= text
                .replaceAll("ā", "a")
                .replaceAll("ī", "i")
                .replaceAll("ū", "u")
                .replaceAll("ṛ", "r")
                .replaceAll("ḍ", "d")
                .replaceAll("ṭ", "t")
                .replaceAll("ś", "sh")
                .replaceAll("ṣ", "sh")
                .replaceAll("ñ", "n")
                .replaceAll("ṇ", "n")
                .replaceAll("ḥ", "h")
                .replaceAll("ṃ", "m")
                .replaceAll("[^\\x00-\\x7F]", ""); // strip diacritics

        cleanedtext= cleanedtext.replaceAll("([b-df-hj-np-tv-z])a(\\s|$)", "$1$2");
return cleanedtext.trim();
    }
}

