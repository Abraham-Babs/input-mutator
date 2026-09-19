package com.inputmutator.engine.tokenizer;

/**
 * Immutable token representing a slice of the input string.
 */
public record Token(TokenType type, String value, int startIndex, int endIndex) {

    public Token {
        if (type == null) {
            throw new IllegalArgumentException("TokenType cannot be null");
        }
        if (value == null) {
            throw new IllegalArgumentException("Token value cannot be null");
        }
        if (startIndex < 0 || endIndex < startIndex) {
            throw new IllegalArgumentException("Invalid token offsets: [" + startIndex + ", " + endIndex + "]");
        }
    }
}
