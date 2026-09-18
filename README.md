# Input Mutator

A lightweight, constraint-guided input permutation engine written in Java 21. Designed for application security auditing, parser boundary testing, and input validation analysis without the noise of naive random fuzzers.

Runs in three modes from a single fat JAR:
1. **Burp Suite Montoya Extension**: Native tab, Repeater/Proxy context-menu action, and custom Intruder payload generator.
2. **Standalone Desktop GUI**: Swing interface with live differential parser simulation, granularity toggles, and an embedded user guide.
3. **Headless CLI**: Unix-friendly command-line tool built for shell piping, automated seed wordlist generation, and CI/CD testing.

---

## Why Input Mutator?

Most fuzzing tools either spray random byte garbage that gets dropped by the first parser layer or require heavy runtime dependencies.

Input Mutator takes a structured approach:
* **Context & Type Awareness**: Tokenizes inputs according to semantic rules (JSON, Email, Numeric, URI, or Generic text) so mutations don't unnecessarily break parent structures unless you want them to.
* **Granular Filter Testing**: Supports sliding-window character mutations (`SINGLE_POSITION`) to probe keyword blacklists and regex lookaheads one character at a time, alongside whole-token transformations.
* **Asymmetric Combinatorial Mutations**: Permutes multiple character positions simultaneously using heterogeneous transforms (e.g. double-encoding character 1, homoglyphing character 2, leaving character 3 raw).
* **Pre-Encoded Auto-Detection & Canonicalization**: Detects existing URL, HTML, Unicode, or Hex escapes upfront and canonicalizes before mutating, preventing accidental delimiter corruption while supporting intentional multi-layer stacking.
* **Zero-Input Archetype Wordlist Generation**: When target input is omitted, the engine generates curated boundary test cases tailored to the selected data type (e.g. integer limits, NaN, quoted emails, traversal paths).
* **Live Differential Parser Inspection**: Real-time side-by-side inspection showing how mutations decode under URL decode, NFKC normalization, and HTML unescaping.
* **Zero Runtime Dependencies**: Built entirely on Java 21 standard libraries (`java.text.Normalizer`, records, pattern matching). Zero third-party runtime baggage.

---

## Mutation Matrix

| Domain | Techniques & Edge Cases Targeted |
| :--- | :--- |
| **Unicode & Homoglyphs** | NFC, NFD (combining marks), NFKC, NFKD decomposition, ligatures (`ﬀ` $\rightarrow$ `ff`), Cyrillic/Greek confusables (`а`, `е`, `о`, `р`), full-width ASCII (`＜`, `＞`, `／`), and Mathematical Alphanumeric Plane 1 surrogate pairs (`\uD835\uDC1A` $\rightarrow$ `𝐚𝐝𝐦𝐢𝐧`). |
| **Encodings & Escapes** | URL percent-encoding (uppercase/lowercase), double-encoding (`%252f`), standard JSON Unicode (`\u0022`), surrogate pairs (`\uD83D\uDE00`), C-style hex (`\x22`), and HTML entities (named, decimal, hex, padded). |
| **Numeric & Radices** | Hexadecimal (`0x`), octal (`0`, `0o`), binary (`0b`), scientific notation (`1e1`, `1.5E-2`), floating-point edge cases (`.5`, `-0.0`, `NaN`, `Infinity`), explicit unary signs (`+42`), and 32-bit/64-bit integer overflow limits. |
| **Invisibles & Controls** | Zero-width spaces (`\u200B`), zero-width joiners/non-joiners (`\u200C`, `\u200D`), word joiners (`\u2060`), C0/C1 control codes, lone/unpaired UTF-16 surrogates (`\uD800`, `\uDC00`), and encoded null sequences (`\0`, `\x00`, `%00`). |
| **Boundaries & Delimiters** | Delimiter duplication (`//`, `''`), dot-segment traversal variations (`/./`, `/../`), and buffer-boundary padding/truncation. |

---

## Granularity Modes

* **`token` (Default)**: Transforms whole token slices at once. Best for fast, high-level permutation generation.
* **`single` (Sliding Window)**: Iterates character-by-character through candidate tokens, mutating exactly one character position at a time (e.g., `admin` $\rightarrow$ `%61dmin`, `a%64min`, `ad%6dmin`). Ideal for bypassing naive keyword filters that only match exact literals.
* **`combinatorial`**: Selectively permutes pairs and multi-character subsets using heterogeneous mutators up to a configurable position ceiling (`--max-positions`), strictly bounded by result limits to prevent state explosion.

---

## Prerequisites & Build

* **Java**: JDK 21+
* **Maven**: 3.8+

Compile, test, and produce the fat JAR:

```bash
mvn clean test package
```

The self-contained JAR is built at:
```
target/input-mutator-1.0.0-SNAPSHOT.jar
```

---

## CLI Usage & Examples

Run headless by providing target strings via `-t / --target` (or omit `-t` to generate archetype seeds):

```bash
# View all flags, options, and examples
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar --help

# 1. Sliding-window keyword filter testing (mutate 1 char at a time)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "admin" -g single -n 30

# 2. Structured email validation testing
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "auditor@internal.corp" --type email -n 25

# 3. Numeric radix & overflow boundaries
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "100" --type numeric -n 20

# 4. Zero-Input Archetype Wordlist Generation (e.g. generate boundary seeds for numeric inputs)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar --type numeric -n 20

# 5. Pre-encoded input handling (canonicalizes %61dmin before targeted mutation)
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "%61dmin" -g single -n 25

# 6. Pipe output directly to wordlists or other security tools
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar -t "../etc/passwd" -n 50 > custom_wordlist.txt
```

---

## GUI & Burp Suite Integration

### Standalone Desktop GUI
Launch without arguments (or double-click the JAR):
```bash
java -jar target/input-mutator-1.0.0-SNAPSHOT.jar
```
* **Live Differential Parser Inspector**: Click any generated permutation in the results table to view its canonical representation across URL decoding, Unicode NFKC normalization, and HTML unescaping in real time.
* **Zero-Input Seeds**: Click *Generate Permutations* with an empty input field to produce boundary archetype seeds for the chosen type.
* **Embedded Help Guide**: Modal manual detailing all mutators, granularity mechanics, and input types.

### Burp Suite Extension
1. Open **Burp Suite** (Community or Professional).
2. Navigate to **Extensions** $\rightarrow$ **Installed** $\rightarrow$ **Add**.
3. Select **Extension type**: `Java`.
4. Choose `target/input-mutator-1.0.0-SNAPSHOT.jar` and click **Next**.

**Features inside Burp**:
* **Native Suite Tab**: Access the full Input Mutator GUI and Live Normalization Inspector directly within Burp.
* **Repeater / Proxy Context Menu**: Highlight any parameter or string in an HTTP request, right-click, and choose **Send to Input Mutator**.
* **Burp Intruder Custom Payload Generator**: In Intruder attacks, select `Payload type: Extension-generated` $\rightarrow$ `Input Mutator - Constraint Engine` to fuzz parameters on-the-fly with constraint-guided mutations.
