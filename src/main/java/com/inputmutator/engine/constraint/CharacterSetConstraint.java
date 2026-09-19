package com.inputmutator.engine.constraint;

/**
 * Declarative character set constraints enforced on mutation candidates.
 */
public enum CharacterSetConstraint {
    ANY("Any Characters") {
        @Override
        public boolean satisfies(String s) {
            return true;
        }
    },
    ASCII_ONLY("ASCII Only (<= 127)") {
        @Override
        public boolean satisfies(String s) {
            if (s == null) return false;
            for (int i = 0; i < s.length(); i++) {
                if (s.charAt(i) > 127) return false;
            }
            return true;
        }
    },
    ALPHANUMERIC("Alphanumeric Only (a-z, A-Z, 0-9)") {
        @Override
        public boolean satisfies(String s) {
            if (s == null || s.isEmpty()) return false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (!Character.isLetterOrDigit(c) || c > 127) return false;
            }
            return true;
        }
    },
    PRINTABLE_ASCII("Printable ASCII Only (0x20 - 0x7E)") {
        @Override
        public boolean satisfies(String s) {
            if (s == null) return false;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                if (c < 0x20 || c > 0x7E) return false;
            }
            return true;
        }
    };

    private final String displayName;

    CharacterSetConstraint(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public abstract boolean satisfies(String s);
}
