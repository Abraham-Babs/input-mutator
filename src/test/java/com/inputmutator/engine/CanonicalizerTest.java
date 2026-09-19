package com.inputmutator.engine;

import com.inputmutator.engine.encoding.Canonicalizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalizerTest {

    private Canonicalizer canonicalizer;

    @BeforeEach
    void setUp() {
        canonicalizer = new Canonicalizer();
    }

    @Test
    void testCanonicalizeUrl() {
        String decoded = canonicalizer.canonicalize("%61%64%6d%69%6e");
        assertEquals("admin", decoded);
    }

    @Test
    void testCanonicalizeUnicodeAndHex() {
        String uni = canonicalizer.canonicalize("\\u0061dmin");
        assertEquals("admin", uni);

        String hex = canonicalizer.canonicalize("\\x61dmin");
        assertEquals("admin", hex);
    }

    @Test
    void testCanonicalizeHtml() {
        String html = canonicalizer.canonicalize("&quot;&#x61;&#100;min&quot;");
        assertEquals("\"admin\"", html);
    }

    @Test
    void testFullCanonicalizeMultiLayer() {
        // Double URL-encoded "admin"
        String doubleEncoded = "%2561%2564%256D%2569%256E";
        String full = canonicalizer.fullCanonicalize(doubleEncoded);
        assertEquals("admin", full.toLowerCase());
    }

    @Test
    void testCanonicalizeMultibyteUtf8() {
        // UTF-8 %C3%A9 = é, %E2%82%AC = €
        String decoded = canonicalizer.canonicalize("caf%C3%A9%20%E2%82%AC10");
        assertEquals("café €10", decoded);
    }
}
