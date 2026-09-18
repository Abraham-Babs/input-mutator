package com.inputmutator.engine.tokenizer;

/**
 * Structural categories for tokenized input segments.
 */
public enum TokenType {
    LITERAL,
    NUMERIC,
    DELIMITER,
    WHITESPACE,
    CONTROL_CHAR,
    SPECIAL_CHAR,
    JSON_KEY,
    JSON_VALUE,
    EMAIL_LOCAL,
    EMAIL_DOMAIN
}
