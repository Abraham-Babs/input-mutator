package com.inputmutator.engine.pipeline;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.model.TestingIntent;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates boundary archetype test cases when no target input is specified by the user,
 * guided by the active TestingIntent and CharacterSetConstraint.
 */
public class SeedGenerator {

    public List<String> generateSeeds(ConstraintProfile profile) {
        InputType type = profile.inputType();
        if (type == null) {
            type = InputType.GENERIC_STRING;
        }

        List<String> rawSeeds = switch (type) {
            case NUMERIC -> List.of(
                    "0", "-1", "1", "2147483647", "2147483648", "-2147483648",
                    "9223372036854775807", "0000", "0x0", "0b0", "0o0",
                    "1e0", "1e-308", "1e308", "+0", "-0.0", "NaN", "Infinity", "-Infinity"
            );
            case EMAIL -> List.of(
                    "admin@localhost",
                    "user.name+tag@example.com",
                    "\"user name\"@example.com",
                    "a@b.co",
                    "user@[127.0.0.1]",
                    "test_user@sub.domain.org",
                    "user-name@domain-name.com"
            );
            case JSON -> List.of(
                    "{}",
                    "[]",
                    "{\"key\":\"val\"}",
                    "{\"id\":0}",
                    "{\"a\":[1,2,3]}",
                    "{\"nested\":{\"depth\":1}}",
                    "{\"\\u0061\":true}",
                    "[null, true, false, 0, \"\"]"
            );
            case URL_PATH -> List.of(
                    "/",
                    "/.",
                    "/..",
                    "/./",
                    "/../",
                    "....//",
                    "/%2e%2e/",
                    "/%2f/",
                    "/api/v1/resource",
                    "/index.html%00",
                    "\\..\\"
            );
            case PHONE_NUMBER -> List.of(
                    "+1234567890",
                    "+1 (800) 555-0199",
                    "+44-20-7946-0991",
                    "0000000000",
                    "+0"
            );
            default -> generateGenericSeeds(profile.testingIntent());
        };

        // Filter seeds against CharacterSetConstraint if defined
        if (profile.characterSetConstraint() == null) {
            return rawSeeds;
        }
        List<String> filtered = new ArrayList<>();
        for (String s : rawSeeds) {
            if (profile.characterSetConstraint().satisfies(s)) {
                filtered.add(s);
            }
        }
        return filtered.isEmpty() ? rawSeeds : filtered;
    }

    private List<String> generateGenericSeeds(TestingIntent intent) {
        if (intent == TestingIntent.WHITELIST_AUDITING) {
            return List.of(
                    "admin", "root", "user123", "guest", "test", "default", "system", "42", "127.0.0.1"
            );
        } else if (intent == TestingIntent.PARSER_DIFFERENTIAL) {
            return List.of(
                    "/admin", "/api/v1/users", "/..;/", "admin;param=1", "admin%00", "admin/**/",
                    "admin//", "admin\\", "<test>", "null", "undefined", "user(comment)@domain.com", "{\"role\":\"admin\"}"
            );
        }
        return List.of(
                "admin", "root", "../etc/passwd", "' OR '1'='1", "<script>alert(1)</script>",
                "sleep(5)", "cmd.exe", "/bin/sh", "test", "guest", "null", "undefined"
        );
    }
}
