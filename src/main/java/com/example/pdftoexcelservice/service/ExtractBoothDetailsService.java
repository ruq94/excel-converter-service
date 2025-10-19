package com.example.pdftoexcelservice.service;

import com.example.pdftoexcelservice.constant.BoothDetailsEnum;
import com.example.pdftoexcelservice.dtos.BoothDetailsDto;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
@Slf4j
@RequiredArgsConstructor
public class ExtractBoothDetailsService {
private final TransliterationService transliterationService;
    public BoothDetailsDto parseBoothDetails(String text) {
        BoothDetailsDto dto = null;

        String boothNumber = extractBoothNumber(text);

        String boothName = extractBoothName(text);
        String boothAddress = extractBoothAddress(text);


        String constituency = extractConstituency(text);
        return BoothDetailsDto.builder()
                .boothAddress(transliterationService.transliterate(boothAddress))
                .boothName(transliterationService.transliterate(boothName))
                .boothNumber(transliterationService.transliterate(boothNumber))
                .constituency(transliterationService.transliterate(constituency)).build();

    }

    /**
     * Extract Booth Number: भाग संख्या 27
     */
    private String extractBoothNumber(String text) {
        // Pattern 1: भाग संख्या followed by number
        Pattern pattern1 = Pattern.compile("भाग\\s*संख्या\\s*[:\\s]*(\\d+)", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            String number = matcher1.group(1).trim();
            log.debug("Found booth number (Pattern 1): {}", number);
            return number;
        }

        // Pattern 2: Just find number after भाग
        Pattern pattern2 = Pattern.compile("भाग\\s+[\\u0966-\\u096F\\d]+", Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            String match = matcher2.group();
            String number = match.replaceAll("[^\\u0966-\\u096F\\d]", "");
            number = convertDevanagariToArabic(number);
            log.debug("Found booth number (Pattern 2): {}", number);
            return number;
        }

        log.warn("Booth number not found");
        return "";
    }

    /**
     * Extract Booth Name: मतदान केंद्र की संख्या व नाम : 27 - जिला निबंधन कार्यालय सिवान बायाँ भाग
     */
    private String extractBoothName(String text) {
        // Pattern: मतदान केंद्र की संख्या व नाम : 27 - जिला निबंधन कार्यालय सिवान बायाँ भाग
        Pattern pattern = Pattern.compile(
                "निबंधन\\s*.\\s*\\(.*?\\)\\s*\\n\\s*([0-9]+\\s*-\\s*.+?)(?=\\n|इस\\s*मतदान|$)",
                Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String name = matcher.group(1).trim();
            // Clean up extra whitespace and newlines
            name = name.replaceAll("\\s+", " ").trim();
            log.debug("Found booth name: {}", name);
            return name;
        }

        // Alternative pattern: Look for number followed by dash and name
        Pattern altPattern = Pattern.compile("(\\d+\\s*-\\s*[^\\n]+?(?:कार्यालय|विद्यालय|भवन|केंद्र)[^\\n]*)",
                Pattern.UNICODE_CHARACTER_CLASS);
        Matcher altMatcher = altPattern.matcher(text);
        if (altMatcher.find()) {
            String name = altMatcher.group(1).trim();
            log.debug("Found booth name (alternative): {}", name);
            return name;
        }

        log.warn("Booth name not found");
        return "";
    }

    /**
     * Extract Constituency: जिला : सिवान
     */
    private String extractConstituency(String text) {
        // Pattern: जिला : सिवान
        Pattern pattern = Pattern.compile("जिला\\s*[:\\s]+([\\u0900-\\u097F\\s]+?)(?=\\n|पिन|$)",
                Pattern.UNICODE_CHARACTER_CLASS);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String constituency = matcher.group(1).trim();
            // Clean up
            constituency = constituency.replaceAll("\\s+", " ").trim();
            log.debug("Found constituency: {}", constituency);
            return constituency;
        }

        log.warn("Constituency not found");
        return "";
    }
    private String extractBoothAddress(String text) {
        // Pattern: मतदान केंद्र का पता : की संख्या :
        Pattern pattern = Pattern.compile(
                "मतदान\\s*केंद्र\\s*का\\s*पता\\s*:\\s*की\\s*संख्या\\s*[:\\s]+(.+?)(?=\\n|4.\\s*मतदाताओं|$)",
                Pattern.UNICODE_CHARACTER_CLASS | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            String name = matcher.group(1).trim();
            // Clean up extra whitespace and newlines
            name = name.replaceAll("\\s+", " ").trim();
            log.debug("Found booth name: {}", name);
            return name;
        }

        log.warn("Booth name not found");
        return "";
    }

        /**
         * Convert Devanagari digits (०-९) to Arabic (0-9)
         */
    private String convertDevanagariToArabic(String input) {
        if (input == null || input.isEmpty()) return input;

        StringBuilder result = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= '\u0966' && c <= '\u096F') {
                // Devanagari digit
                result.append((char) (c - '\u0966' + '0'));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}


