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

        // Character set constraint
        if (profile.characterSetConstraint() != null && !profile.characterSetConstraint().satisfies(candidate)) {
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
        int len = s.length();
        for (int i = 0; i < len; ) {
            int cp = s.codePointAt(i);
            if ((cp <= 0x1F && cp != '\t' && cp != '\r' && cp != '\n')
                    || cp == 0x7F
                    || (cp >= 0x80 && cp <= 0x9F)
                    || (cp >= 0xD800 && cp <= 0xDFFF)) {
                return true;
            }
            i += Character.charCount(cp);
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
        if (email == null || email.length() < 3) {
            return false;
        }
        int atIndex;
        if (email.startsWith("\"")) {
            int closingQuote = -1;
            boolean escaped = false;
            for (int i = 1; i < email.length(); i++) {
                char c = email.charAt(i);
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    closingQuote = i;
                    break;
                }
            }
            if (closingQuote == -1 || closingQuote >= email.length() - 2) {
                return false;
            }
            if (email.charAt(closingQuote + 1) != '@') {
                return false;
            }
            atIndex = closingQuote + 1;
        } else {
            atIndex = email.indexOf('@');
            if (atIndex <= 0 || atIndex == email.length() - 1 || email.indexOf('@', atIndex + 1) != -1) {
                return false;
            }
        }
        String domain = email.substring(atIndex + 1);
        return !domain.isEmpty() && !domain.startsWith(".") && !domain.endsWith(".");
    }

    private boolean validateJsonStructure(String json) {
        String trimmed = json.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        char first = trimmed.charAt(0);
        char last = trimmed.charAt(trimmed.length() - 1);
        if (!((first == '{' && last == '}') || (first == '[' && last == ']'))) {
            return false;
        }
        java.util.ArrayDeque<Character> stack = new java.util.ArrayDeque<>();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '{' || c == '[') {
                    stack.push(c);
                } else if (c == '}') {
                    if (stack.isEmpty() || stack.pop() != '{') return false;
                } else if (c == ']') {
                    if (stack.isEmpty() || stack.pop() != '[') return false;
                }
            }
        }
        return !inString && stack.isEmpty();
    }

    private boolean validateNumericStructure(String num) {
        String trimmed = num.trim();
        return trimmed.matches("^(?i)[+-]?(?:nan|infinity|0x[0-9a-f]+|0b[01]+|0o[0-7]+|0[0-7]*|(?U)(?:\\d*\\.\\d+|\\d+)(?:[eE][+-]?\\d+)?)$");
    }
}
