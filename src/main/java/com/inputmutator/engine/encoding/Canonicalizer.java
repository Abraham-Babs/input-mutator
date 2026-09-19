package com.inputmutator.engine.encoding;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes pre-encoded, escaped, or transformed inputs back to canonical UTF-8 characters.
 */
public class Canonicalizer {

    private static final Pattern UNICODE_ESCAPE_PAT = Pattern.compile("(?i)\\\\u([0-9a-f]{4})");
    private static final Pattern HEX_ESCAPE_PAT = Pattern.compile("(?i)\\\\x([0-9a-f]{2})");
    private static final Pattern HTML_HEX_PAT = Pattern.compile("(?i)&#x([0-9a-f]{1,6});");
    private static final Pattern HTML_DEC_PAT = Pattern.compile("&#([0-9]{1,7});");
    private static final Pattern URL_PERCENT_PAT = Pattern.compile("(?i)%([0-9a-f]{2})");

    /**
     * Decodes a single layer of detected escapes/encodings.
     */
    public String canonicalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String result = input;
        result = decodeUnicodeEscapes(result);
        result = decodeHexEscapes(result);
        result = decodeHtmlEntities(result);
        result = decodeUrlPercents(result);

        return result;
    }

    /**
     * Iteratively decodes until reaching fixpoint (unwrapping multi-layer encodings).
     */
    public String fullCanonicalize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String current = input;
        for (int i = 0; i < 5; i++) {
            String decoded = canonicalize(current);
            if (decoded.equals(current)) {
                break;
            }
            current = decoded;
        }
        return Normalizer.normalize(current, Normalizer.Form.NFKC);
    }

    private String decodeUnicodeEscapes(String s) {
        Matcher m = UNICODE_ESCAPE_PAT.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int code = Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(code))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String decodeHexEscapes(String s) {
        Matcher m = HEX_ESCAPE_PAT.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            int code = Integer.parseInt(m.group(1), 16);
            m.appendReplacement(sb, Matcher.quoteReplacement(new String(Character.toChars(code))));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    public String decodeHtmlEntities(String s) {
        String res = s.replace("&quot;", "\"")
                      .replace("&apos;", "'")
                      .replace("&lt;", "<")
                      .replace("&gt;", ">")
                      .replace("&amp;", "&");

        Matcher hexM = HTML_HEX_PAT.matcher(res);
        StringBuilder hexSb = new StringBuilder();
        while (hexM.find()) {
            int code = Integer.parseInt(hexM.group(1), 16);
            hexM.appendReplacement(hexSb, Matcher.quoteReplacement(new String(Character.toChars(code))));
        }
        hexM.appendTail(hexSb);
        res = hexSb.toString();

        Matcher decM = HTML_DEC_PAT.matcher(res);
        StringBuilder decSb = new StringBuilder();
        while (decM.find()) {
            int code = Integer.parseInt(decM.group(1), 10);
            decM.appendReplacement(decSb, Matcher.quoteReplacement(new String(Character.toChars(code))));
        }
        decM.appendTail(decSb);
        return decSb.toString();
    }

    private String decodeUrlPercents(String s) {
        StringBuilder sb = new StringBuilder();
        int len = s.length();
        int i = 0;
        java.io.ByteArrayOutputStream byteBuf = new java.io.ByteArrayOutputStream();

        while (i < len) {
            if (s.charAt(i) == '%' && i + 2 < len && isHex(s.charAt(i + 1)) && isHex(s.charAt(i + 2))) {
                try {
                    int b = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                    byteBuf.write(b);
                    i += 3;
                    continue;
                } catch (Exception ignored) {}
            }
            if (byteBuf.size() > 0) {
                flushByteBuf(sb, byteBuf);
            }
            sb.append(s.charAt(i));
            i++;
        }
        if (byteBuf.size() > 0) {
            flushByteBuf(sb, byteBuf);
        }
        return sb.toString();
    }

    private void flushByteBuf(StringBuilder sb, java.io.ByteArrayOutputStream byteBuf) {
        byte[] bytes = byteBuf.toByteArray();
        byteBuf.reset();
        try {
            java.nio.charset.CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT);
            String decoded = decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString();
            sb.append(decoded);
        } catch (java.nio.charset.CharacterCodingException e) {
            for (byte b : bytes) {
                sb.append((char) (b & 0xFF));
            }
        }
    }

    private static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
