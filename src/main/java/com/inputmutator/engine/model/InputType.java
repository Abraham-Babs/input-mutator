package com.inputmutator.engine.model;

/**
 * Classifies the semantic format of the input to guide targeted mutations.
 */
public enum InputType {
    GENERIC_STRING,
    NUMERIC,
    JSON,
    EMAIL,
    URL_PATH,
    PHONE_NUMBER;

    /**
     * Auto-detect input type using lightweight heuristics.
     */
    public static InputType detect(String input) {
        if (input == null || input.isBlank()) {
            return GENERIC_STRING;
        }
        String trimmed = input.trim();

        // JSON check
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) ||
            (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            return JSON;
        }

        // Email check
        if (trimmed.contains("@") && !trimmed.contains(" ") && trimmed.indexOf('@') == trimmed.lastIndexOf('@')) {
            int at = trimmed.indexOf('@');
            if (at > 0 && at < trimmed.length() - 1 && trimmed.substring(at + 1).contains(".")) {
                return EMAIL;
            }
        }

        // URL / Path check
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("/")) {
            return URL_PATH;
        }

        // Numeric check (integer, hex, float, scientific)
        if (trimmed.matches("^[+-]?(?:0x[0-9a-fA-F]+|0b[01]+|0o[0-7]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?)$")) {
            return NUMERIC;
        }

        // Phone number check
        if (trimmed.matches("^\\+?[0-9\\-\\s()]{7,20}$")) {
            return PHONE_NUMBER;
        }

        return GENERIC_STRING;
    }
}
