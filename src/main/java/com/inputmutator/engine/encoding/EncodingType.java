package com.inputmutator.engine.encoding;

/**
 * Categorizes detected encoding schemes present in raw or partially transformed input.
 */
public enum EncodingType {
    RAW,
    URL_PERCENT,
    DOUBLE_URL,
    HTML_NAMED,
    HTML_NUMERIC,
    HTML_HEX,
    UNICODE_ESCAPE,
    HEX_ESCAPE
}
