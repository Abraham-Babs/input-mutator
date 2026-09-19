package com.inputmutator.engine;

import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.tokenizer.InputTokenizer;
import com.inputmutator.engine.tokenizer.Token;
import com.inputmutator.engine.tokenizer.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InputTokenizerTest {

    private InputTokenizer tokenizer;

    @BeforeEach
    void setUp() {
        tokenizer = new InputTokenizer();
    }

    @Test
    void testTokenizeGeneric() {
        List<Token> tokens = tokenizer.tokenize("admin' OR 1=1--", InputType.GENERIC_STRING);
        assertFalse(tokens.isEmpty());

        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.LITERAL && t.value().equals("admin")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.DELIMITER && t.value().equals("'")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.NUMERIC && t.value().equals("1")));
    }

    @Test
    void testTokenizeEmail() {
        List<Token> tokens = tokenizer.tokenize("user.test@example.com", InputType.EMAIL);
        assertEquals(3, tokens.size());
        assertEquals(TokenType.EMAIL_LOCAL, tokens.get(0).type());
        assertEquals("user.test", tokens.get(0).value());
        assertEquals(TokenType.DELIMITER, tokens.get(1).type());
        assertEquals("@", tokens.get(1).value());
        assertEquals(TokenType.EMAIL_DOMAIN, tokens.get(2).type());
        assertEquals("example.com", tokens.get(2).value());
    }

    @Test
    void testTokenizeJson() {
        String json = "{\"id\": 42, \"name\": \"admin\"}";
        List<Token> tokens = tokenizer.tokenize(json, InputType.JSON);

        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.JSON_KEY && t.value().equals("\"id\"")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.NUMERIC && t.value().equals("42")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.JSON_KEY && t.value().equals("\"name\"")));
        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.JSON_VALUE && t.value().equals("\"admin\"")));
    }

    @Test
    void testTokenizeControlChars() {
        String input = "test\u0007\0val";
        List<Token> tokens = tokenizer.tokenize(input, InputType.GENERIC_STRING);

        assertTrue(tokens.stream().anyMatch(t -> t.type() == TokenType.CONTROL_CHAR));
    }

    @Test
    void testTokenizeAlphanumericIdentifier() {
        List<Token> tokens = tokenizer.tokenize("user123", InputType.GENERIC_STRING);
        assertEquals(1, tokens.size());
        assertEquals(TokenType.LITERAL, tokens.get(0).type());
        assertEquals("user123", tokens.get(0).value());
    }
}
