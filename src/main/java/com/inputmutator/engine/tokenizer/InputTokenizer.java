package com.inputmutator.engine.tokenizer;

import com.inputmutator.engine.model.InputType;

import java.util.ArrayList;
import java.util.List;

/**
 * Single-pass deterministic tokenizer tailored for targeted mutation.
 */
public class InputTokenizer {

    public List<Token> tokenize(String input, InputType type) {
        if (input == null || input.isEmpty()) {
            return List.of();
        }

        InputType targetType = (type == null) ? InputType.detect(input) : type;

        return switch (targetType) {
            case EMAIL -> tokenizeEmail(input);
            case JSON -> tokenizeJson(input);
            default -> tokenizeGeneric(input);
        };
    }

    private List<Token> tokenizeEmail(String input) {
        int atIndex = input.indexOf('@');
        if (atIndex <= 0 || atIndex == input.length() - 1) {
            return tokenizeGeneric(input);
        }

        List<Token> tokens = new ArrayList<>();
        tokens.add(new Token(TokenType.EMAIL_LOCAL, input.substring(0, atIndex), 0, atIndex));
        tokens.add(new Token(TokenType.DELIMITER, "@", atIndex, atIndex + 1));
        tokens.add(new Token(TokenType.EMAIL_DOMAIN, input.substring(atIndex + 1), atIndex + 1, input.length()));
        return tokens;
    }

    private List<Token> tokenizeJson(String input) {
        List<Token> tokens = new ArrayList<>();
        int len = input.length();
        int i = 0;

        while (i < len) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                int start = i;
                while (i < len && Character.isWhitespace(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.WHITESPACE, input.substring(start, i), start, i));
            } else if (c == '{' || c == '}' || c == '[' || c == ']' || c == ':' || c == ',') {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(c), i, i + 1));
                i++;
            } else if (c == '"') {
                // String literal (key or value)
                int start = i;
                i++; // skip open quote
                boolean escaped = false;
                while (i < len) {
                    char cur = input.charAt(i);
                    if (escaped) {
                        escaped = false;
                    } else if (cur == '\\') {
                        escaped = true;
                    } else if (cur == '"') {
                        i++;
                        break;
                    }
                    i++;
                }
                String strToken = input.substring(start, i);
                // Lookahead past whitespace for ':' to distinguish JSON_KEY from JSON_VALUE
                int peek = i;
                while (peek < len && Character.isWhitespace(input.charAt(peek))) {
                    peek++;
                }
                boolean isKey = (peek < len && input.charAt(peek) == ':');
                tokens.add(new Token(isKey ? TokenType.JSON_KEY : TokenType.JSON_VALUE, strToken, start, i));
            } else if (isNumberStart(input, i)) {
                int start = i;
                if (input.charAt(i) == '-' || input.charAt(i) == '+') {
                    i++;
                }
                while (i < len && isNumberChar(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMERIC, input.substring(start, i), start, i));
            } else if (isControlChar(c)) {
                tokens.add(new Token(TokenType.CONTROL_CHAR, String.valueOf(c), i, i + 1));
                i++;
            } else {
                int start = i;
                while (i < len && !isDelimiter(input.charAt(i)) && !Character.isWhitespace(input.charAt(i)) && input.charAt(i) != '"') {
                    i++;
                }
                tokens.add(new Token(TokenType.LITERAL, input.substring(start, i), start, i));
            }
        }
        return tokens;
    }

    private List<Token> tokenizeGeneric(String input) {
        List<Token> tokens = new ArrayList<>();
        int len = input.length();
        int i = 0;

        while (i < len) {
            char c = input.charAt(i);

            if (isControlChar(c)) {
                int start = i;
                while (i < len && isControlChar(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.CONTROL_CHAR, input.substring(start, i), start, i));
            } else if (Character.isWhitespace(c)) {
                int start = i;
                while (i < len && Character.isWhitespace(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.WHITESPACE, input.substring(start, i), start, i));
            } else if (isNumberStart(input, i)) {
                int start = i;
                if (c == '-' || c == '+') {
                    i++;
                }
                while (i < len && isNumberChar(input.charAt(i))) {
                    i++;
                }
                tokens.add(new Token(TokenType.NUMERIC, input.substring(start, i), start, i));
            } else if (isDelimiter(c)) {
                tokens.add(new Token(TokenType.DELIMITER, String.valueOf(c), i, i + 1));
                i++;
            } else if (Character.isLetter(c)) {
                int start = i;
                while (i < len && (Character.isLetterOrDigit(input.charAt(i)) || input.charAt(i) == '_')) {
                    i++;
                }
                tokens.add(new Token(TokenType.LITERAL, input.substring(start, i), start, i));
            } else {
                int start = i;
                tokens.add(new Token(TokenType.SPECIAL_CHAR, String.valueOf(c), start, start + 1));
                i++;
            }
        }
        return tokens;
    }

    private boolean isControlChar(char c) {
        return (c <= 0x1F && c != '\t' && c != '\r' && c != '\n') || c == 0x7F || (c >= 0x80 && c <= 0x9F);
    }

    private boolean isDelimiter(char c) {
        return c == '/' || c == '\\' || c == '=' || c == '&' || c == '?' ||
               c == ';' || c == ':' || c == '\'' || c == '"' || c == '<' ||
               c == '>' || c == '(' || c == ')' || c == '{' || c == '}' ||
               c == '[' || c == ']' || c == ',' || c == '%' || c == '#';
    }

    private boolean isNumberStart(String s, int idx) {
        char c = s.charAt(idx);
        if (Character.isDigit(c)) {
            return true;
        }
        if ((c == '-' || c == '+') && idx + 1 < s.length() && Character.isDigit(s.charAt(idx + 1))) {
            return true;
        }
        return false;
    }

    private boolean isNumberChar(char c) {
        return Character.isDigit(c) || c == '.' || c == 'e' || c == 'E' ||
               c == 'x' || c == 'X' || c == 'b' || c == 'B' || c == 'o' || c == 'O' ||
               (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == '+' || c == '-';
    }
}
