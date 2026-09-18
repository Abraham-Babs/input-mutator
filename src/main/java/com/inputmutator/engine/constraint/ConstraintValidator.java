package com.inputmutator.engine.constraint;

import com.inputmutator.engine.model.InputType;

/**
 * Validates candidate mutated inputs against active constraint profile criteria.
 */
public class ConstraintValidator {

    public boolean isValid(String candidate, ConstraintProfile profile) {
        if (candidate == null) {
            return false;
        }

        // Length validation
        int len = candidate.length();
        if (len < profile.minLength() || len > profile.maxLength()) {
            return false;
        }

        // Null-byte validation
        if (!profile.allowNullBytes() && candidate.indexOf('\0') >= 0) {
            return false;
        }

        // Non-printable validation
        if (!profile.allowNonPrintable() && containsUnescapedControlChars(candidate)) {
            return false;
        }

        // Regex pattern constraint
        if (profile.allowedPattern() != null && !profile.allowedPattern().matcher(candidate).matches()) {
            return false;
        }

        // Structural preservation for structured inputs
        if (profile.preserveStructure()) {
            if (!isStructurallyValid(candidate, profile.inputType())) {
                return false;
            }
        }

        return true;
    }

    private boolean containsUnescapedControlChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c <= 0x1F && c != '\t' && c != '\r' && c != '\n') || c == 0x7F) {
                return true;
            }
        }
        return false;
    }

    private boolean isStructurallyValid(String candidate, InputType type) {
        if (type == null) {
            return true;
        }
        return switch (type) {
            case EMAIL -> validateEmailStructure(candidate);
            case JSON -> validateJsonStructure(candidate);
            case NUMERIC -> validateNumericStructure(candidate);
            default -> true;
        };
    }

    private boolean validateEmailStructure(String email) {
        int at = email.indexOf('@');
        return at > 0 && at < email.length() - 1 && email.indexOf('@', at + 1) == -1;
    }

    private boolean validateJsonStructure(String json) {
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        return (first == '{' && last == '}') || (first == '[' && last == ']');
    }

    private boolean validateNumericStructure(String num) {
        String trimmed = num.trim();
        return trimmed.matches("(?U)^[+-]?(?:0x[0-9a-fA-F]+|0b[01]+|0o[0-7]+|\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?|[0-9]+)$");
    }
}
