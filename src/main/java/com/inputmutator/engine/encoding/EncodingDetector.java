package com.inputmutator.engine.encoding;

import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Detects whether an input string or substring already contains encoded or transformed characters.
 */
public class EncodingDetector {

    private static final Pattern DOUBLE_URL_PATTERN = Pattern.compile("(?i)%25[0-9a-f]{2}");
    private static final Pattern SINGLE_URL_PATTERN = Pattern.compile("(?i)%[0-9a-f]{2}");
    private static final Pattern HTML_NAMED_PATTERN = Pattern.compile("&(?:quot|apos|lt|gt|amp);");
    private static final Pattern HTML_NUMERIC_PATTERN = Pattern.compile("&#[0-9]{1,7};");
    private static final Pattern HTML_HEX_PATTERN = Pattern.compile("(?i)&#x[0-9a-f]{1,6};");
    private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("(?i)\\\\u[0-9a-f]{4}");
    private static final Pattern HEX_ESCAPE_PATTERN = Pattern.compile("(?i)\\\\x[0-9a-f]{2}");

    public Set<EncodingType> detect(String input) {
        if (input == null || input.isEmpty()) {
            return EnumSet.of(EncodingType.RAW);
        }

        Set<EncodingType> detected = EnumSet.noneOf(EncodingType.class);

        if (DOUBLE_URL_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.DOUBLE_URL);
        } else if (SINGLE_URL_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.URL_PERCENT);
        }

        if (HTML_NAMED_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.HTML_NAMED);
        }
        if (HTML_NUMERIC_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.HTML_NUMERIC);
        }
        if (HTML_HEX_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.HTML_HEX);
        }
        if (UNICODE_ESCAPE_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.UNICODE_ESCAPE);
        }
        if (HEX_ESCAPE_PATTERN.matcher(input).find()) {
            detected.add(EncodingType.HEX_ESCAPE);
        }

        if (detected.isEmpty()) {
            detected.add(EncodingType.RAW);
        }

        return detected;
    }

    public boolean isEncoded(String input) {
        Set<EncodingType> types = detect(input);
        return !types.contains(EncodingType.RAW) || types.size() > 1;
    }
}
