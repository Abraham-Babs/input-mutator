# Input Mutator

A lightweight, constraint-guided input permutation engine written in Java 21. Designed for application input validation testing, parser boundary analysis, and differential compliance verification without the noise of naive random fuzzers.

Runs in three modes from a single fat JAR:
1. **Burp Suite Montoya Extension**: Native suite tab, Repeater/Proxy context-menu action, and custom Intruder payload generator.
2. **Standalone Desktop GUI**: Swing interface with live differential parser simulation, granularity toggles, and an embedded reference guide.
3. **Headless CLI**: Command-line tool built for shell piping, automated seed generation, and CI/CD validation pipelines.

---

## Why Input Mutator?

Most test permutation tools either emit malformed byte sequences rejected at the network edge or carry heavy third-party runtimes.

Input Mutator takes a structured, parser-aware approach:
* **Context & Semantic Awareness**: Tokenizes inputs according to protocol and format rules (URL, Email, JSON, Numeric, or Generic text) to test parser differential layers while preserving syntactic validity where required.
* **Granular Sliding-Window Testing**: Supports character-level sliding-window permutations (`single`) to systematically test parser edge cases and regular expressions one index at a time, balanced across all positions.
* **Combinatorial Permutations**: Permutes multiple positions simultaneously using heterogeneous transformations (e.g. fullwidth in position 1, percent-encoding in position 2) up to a bounded position limit.
* **Pre-Encoded Auto-Detection & Canonicalization**: Detects URL, HTML, Unicode, and Hex escapes upfront and canonicalizes before applying mutations, preventing unintentional double-escaping while enabling controlled multi-layer encoding.
* **Zero-Input Archetype Generation**: When target input is omitted, the engine produces curated boundary test vectors tailored to the target data type (numeric limits, NaN, scientific notation, traversal tokens).
* **Live Normalization Inspection**: Side-by-side inspection showing how mutations decode under URL decode, Unicode NFKC normalization, and HTML unescaping.
* **Zero Third-Party Runtime Dependencies**: Built entirely on Java 21 standard libraries (`java.text.Normalizer`, records, pattern matching).

---

## Mutation Matrix

| Domain | Techniques & Edge Cases Targeted |
| :--- | :--- |
| **Parser Differentials** | URL path matrix parameters (`/;param=1/`, `;\jsessionid=0`), IIS trailing dots (`/.`), dot-segment normalization (`/./`, `/..;/`), RFC 5322 email comment wrapping (`admin(test)@domain.com`), quoted local-parts (`"admin"@domain.com`), sub-addressing (`admin+tag@`), and raw Unicode JSON key escapes (`"\u0061dmin"`). |
| **Unicode & Homoglyphs** | NFC, NFD combining accents, NFKC/NFKD compatibility decompositions, NFKC singletons (Kelvin sign `\u212A` -> `K`, feminine ordinal `\u00AA` -> `a`, long-s `\u017F` -> `s`), Cyrillic/Greek confusables, Turkish locale-sensitive case variants (`\u0131`, `\u0130`), fullwidth ASCII, and mathematical alphanumeric alphabets. |
| **Encodings & Escapes** | URL percent-encoding (uppercase/lowercase), multi-layer encoding (`%252F`), JSON Unicode escapes (`\u002F`), C-style hex (`\x2F`), octal escapes (`\057`), overlong zero-padded decimal/hex HTML entities (`&#00000047;`, `&#x0000002F;`), and Unicode division slashes (`\u2044`, `\u2215`). |
| **Numeric & Radices** | Hexadecimal (`0x`), modern/legacy octal (`0o`, `0`), binary (`0b`), scientific notation (`1e3`, `1.5E-2`), floating-point edge cases (`.5`, `-0.0`, `NaN`, `Infinity`, `1e999`), 32-bit and 64-bit integer boundaries, Unicode fullwidth digits (`０`-`９`), and Arabic-Indic cultural digits (`٠`-`٩`). |
| **Invisibles & Controls** | Zero-width spaces (`\u200B`), soft hyphens (`\u00AD`), zero-width joiners/non-joiners (`\u200C`, `\u200D`), word joiners (`\u2060`), C0/C1 control codes, lone surrogates, and null byte sequences (`\0`, `\x00`, `%00`). |
| **Boundaries & Delimiters** | Delimiter duplication (`//`, `""`), dot-segment traversal variations, and buffer boundary expansion/truncation. |

---

## Granularity Modes

* **`token` (Default)**: Transforms whole token slices at once using fair round-robin scheduling across mutator categories. Best for broad coverage and fuzzing.
* **`single` (Sliding Window)**: Iterates through each character position in the candidate token, mutating one index at a time (e.g., `admin` -> `%61dmin`, `a%64min`, `ad%6Dmin`). Eliminates positional starvation by round-robin scheduling across all token positions.
* **`combinatorial`**: Systematically permutes multi-character subsets using heterogeneous mutators up to a configurable position limit (`--max-positions`), strictly bounded by result thresholds to prevent state explosion.

---

## Prerequisites & Build

* **Java**: JDK 21+
* **Maven**: 3.8+

Compile, run tests, and package the shaded JAR:

```bash
mvn clean test package
```

The executable JAR is generated at:
```
target/input-mutator-1.0.0-SNAPSHOT.jar
```

---

## CLI Usage & Examples

Run headless by providing target strings via `-t / --target` (or omit `-t` to generate archetype seeds):

```bash
# View all flags, options, and examples
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar --help

# 1. Sliding-window keyword filter testing (mutate 1 char at a time across all positions)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "admin" -g single -n 30

# 2. Structured email parser differential testing
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "admin@corp.internal" --type email -n 25

# 3. URL path and matrix parameter testing
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "/api/v1/resource" --type url -n 25

# 4. Numeric radix, boundary, and cultural digit testing
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "42" --type numeric -n 25

# 5. Zero-input archetype generation (boundary vectors for selected data type)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar --type numeric -n 20

# 6. Pre-encoded input handling (auto-canonicalizes %61dmin before targeted mutation)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "%61dmin" -g single -n 25

# 7. Output redirection for test pipelines
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "../config/settings.json" -n 50 > test_vectors.txt
```

---

## GUI & Burp Suite Integration

### Standalone Desktop GUI
Launch without arguments (or double-click the JAR):
```bash
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar
```
* **Differential Normalization Inspector**: Select any generated permutation in the results table to view its canonical representation across URL decoding, Unicode NFKC normalization, and HTML unescaping in real time.
* **Zero-Input Seeds**: Click *Generate Permutations* with an empty input field to produce boundary archetype seeds for the active type.
* **Embedded Reference Guide**: Comprehensive documentation detailing all mutators, granularity mechanics, and constraint profiles.

### Burp Suite Extension
1. Open **Burp Suite** (Community or Professional).
2. Navigate to **Extensions** > **Installed** > **Add**.
3. Select **Extension type**: `Java`.
4. Choose `target/input-mutator-1.0.0-SNAPSHOT.jar` and click **Next**.

**Features inside Burp**:
* **Native Suite Tab**: Access the full Input Mutator GUI and Live Normalization Inspector directly within Burp.
* **Repeater / Proxy Context Menu**: Highlight any parameter or string in an HTTP request, right-click, and choose **Send to Input Mutator**.
* **Burp Intruder Custom Payload Generator**: In Intruder attacks, select `Payload type: Extension-generated` > `Input Mutator - Constraint Engine` to generate constraint-guided mutations on-the-fly.

