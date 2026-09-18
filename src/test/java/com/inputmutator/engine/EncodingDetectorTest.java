package com.inputmutator.engine;

import com.inputmutator.engine.encoding.EncodingDetector;
import com.inputmutator.engine.encoding.EncodingType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class EncodingDetectorTest {

    private EncodingDetector detector;

    @BeforeEach
    void setUp() {
        detector = new EncodingDetector();
    }

    @Test
    void testDetectRaw() {
        Set<EncodingType> types = detector.detect("admin");
        assertTrue(types.contains(EncodingType.RAW));
        assertFalse(detector.isEncoded("admin"));
    }

    @Test
    void testDetectUrlPercent() {
        Set<EncodingType> types = detector.detect("%61dmin");
        assertTrue(types.contains(EncodingType.URL_PERCENT));
        assertTrue(detector.isEncoded("%61dmin"));
    }

    @Test
    void testDetectDoubleUrl() {
        Set<EncodingType> types = detector.detect("%2561dmin");
        assertTrue(types.contains(EncodingType.DOUBLE_URL));
        assertTrue(detector.isEncoded("%2561dmin"));
    }

    @Test
    void testDetectHtmlEntities() {
        Set<EncodingType> named = detector.detect("&quot;admin&quot;");
        assertTrue(named.contains(EncodingType.HTML_NAMED));

        Set<EncodingType> dec = detector.detect("&#65;dmin");
        assertTrue(dec.contains(EncodingType.HTML_NUMERIC));

        Set<EncodingType> hex = detector.detect("&#x41;dmin");
        assertTrue(hex.contains(EncodingType.HTML_HEX));
    }

    @Test
    void testDetectUnicodeAndHexEscapes() {
        Set<EncodingType> uni = detector.detect("\\u0061dmin");
        assertTrue(uni.contains(EncodingType.UNICODE_ESCAPE));

        Set<EncodingType> hex = detector.detect("\\x61dmin");
        assertTrue(hex.contains(EncodingType.HEX_ESCAPE));
    }
}
