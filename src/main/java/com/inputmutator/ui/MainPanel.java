package com.inputmutator.ui;

import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.pipeline.MutationEngine;
import com.inputmutator.engine.pipeline.ParserSimulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.List;

public class MainPanel extends JPanel {

    private final JTextField inputField;
    private final JComboBox<String> typeSelector;
    private final JComboBox<String> granularitySelector;
    private final JCheckBox allowNonPrintableBox;
    private final JCheckBox allowNullBytesBox;
    private final JCheckBox canonicalizeBox;
    private final JCheckBox preserveStructureBox;
    private final JSpinner layersSpinner;
    private final JSpinner maxPermsSpinner;
    private final JSpinner maxDepthSpinner;
    private final JTextArea outputArea;
    private final JLabel statusLabel;
    private final JButton generateButton;
    private final MutationEngine engine;
    private final ParserSimulator simulator;

    // Differential Parser Inspector fields
    private final JTextField simUrlField;
    private final JTextField simNfkcField;
    private final JTextField simHtmlField;
    private final JLabel simMatchBadge;

    public MainPanel() {
        this.engine = new MutationEngine();
        this.simulator = new ParserSimulator();

        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 4));
        JLabel titleLabel = new JLabel("Input Mutator");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 18f));
        JLabel subtitleLabel = new JLabel("Constraint-guided permutation engine for Desktop & Burp Suite");
        subtitleLabel.setForeground(Color.GRAY);
        headerPanel.add(titleLabel);
        headerPanel.add(subtitleLabel);

        // Input & Controls
        JPanel controlsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 4, 3, 4);

        JLabel inputLabel = new JLabel("Target Input:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        controlsPanel.add(inputLabel, gbc);

        inputField = new JTextField();
        inputField.setToolTipText("Enter target string (or leave empty to generate boundary archetype seeds)");
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        controlsPanel.add(inputField, gbc);

        // Input Type Selector
        JLabel typeLabel = new JLabel("Input Type:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        controlsPanel.add(typeLabel, gbc);

        typeSelector = new JComboBox<>(new String[]{
                "Auto-Detect", "Generic String", "Numeric", "JSON", "Email", "URL / Path", "Phone Number"
        });
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        controlsPanel.add(typeSelector, gbc);

        // Granularity Selector
        JLabel granularityLabel = new JLabel("Granularity:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        controlsPanel.add(granularityLabel, gbc);

        granularitySelector = new JComboBox<>(new String[]{
                "Token-Level (Default)", "Single Character (Sliding)", "Combinatorial (Multi-Char)"
        });
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        controlsPanel.add(granularitySelector, gbc);

        // Advanced Constraints & Options
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        allowNonPrintableBox = new JCheckBox("Allow Controls / Surrogates", false);
        allowNullBytesBox = new JCheckBox("Allow Raw NUL (\\0)", false);
        canonicalizeBox = new JCheckBox("Canonicalize Pre-Encoded", true);

        optionsPanel.add(canonicalizeBox);
        preserveStructureBox = new JCheckBox("Preserve Structure", true);
        optionsPanel.add(preserveStructureBox);
        optionsPanel.add(allowNonPrintableBox);
        optionsPanel.add(allowNullBytesBox);

        optionsPanel.add(new JLabel("Layers:"));
        layersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        optionsPanel.add(layersSpinner);

        optionsPanel.add(new JLabel("Max Output:"));
        maxPermsSpinner = new JSpinner(new SpinnerNumberModel(150, 10, 1000, 25));
        optionsPanel.add(maxPermsSpinner);

        optionsPanel.add(new JLabel("Depth:"));
        maxDepthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
        optionsPanel.add(maxDepthSpinner);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        controlsPanel.add(optionsPanel, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        generateButton = new JButton("Generate Permutations");
        JButton copyButton = new JButton("Copy Output");
        JButton clearButton = new JButton("Clear");
        JButton helpButton = new JButton("Help & Guide");

        buttonPanel.add(generateButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(helpButton);

        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.weightx = 1.0;
        controlsPanel.add(buttonPanel, gbc);

        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(controlsPanel, BorderLayout.CENTER);

        // Output Area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane resultsScrollPane = new JScrollPane(outputArea);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("Permutation Results (click any line to inspect normalization)"));

        // Differential Parser Inspector Panel
        JPanel inspectorPanel = new JPanel(new GridBagLayout());
        inspectorPanel.setBorder(BorderFactory.createTitledBorder("Live Differential Parser Inspector"));
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.fill = GridBagConstraints.HORIZONTAL;
        igbc.insets = new Insets(3, 4, 3, 4);

        igbc.gridx = 0; igbc.gridy = 0; igbc.weightx = 0;
        inspectorPanel.add(new JLabel("URL-Decoded:"), igbc);
        simUrlField = new JTextField();
        simUrlField.setEditable(false);
        igbc.gridx = 1; igbc.gridy = 0; igbc.weightx = 1.0;
        inspectorPanel.add(simUrlField, igbc);

        igbc.gridx = 0; igbc.gridy = 1; igbc.weightx = 0;
        inspectorPanel.add(new JLabel("NFKC Normal:"), igbc);
        simNfkcField = new JTextField();
        simNfkcField.setEditable(false);
        igbc.gridx = 1; igbc.gridy = 1; igbc.weightx = 1.0;
        inspectorPanel.add(simNfkcField, igbc);

        igbc.gridx = 0; igbc.gridy = 2; igbc.weightx = 0;
        inspectorPanel.add(new JLabel("HTML Unescape:"), igbc);
        simHtmlField = new JTextField();
        simHtmlField.setEditable(false);
        igbc.gridx = 1; igbc.gridy = 2; igbc.weightx = 1.0;
        inspectorPanel.add(simHtmlField, igbc);

        igbc.gridx = 0; igbc.gridy = 3; igbc.weightx = 0;
        inspectorPanel.add(new JLabel("Resolves to Target?"), igbc);
        simMatchBadge = new JLabel("[Select a permutation]");
        simMatchBadge.setFont(simMatchBadge.getFont().deriveFont(Font.BOLD));
        igbc.gridx = 1; igbc.gridy = 3; igbc.weightx = 1.0;
        inspectorPanel.add(simMatchBadge, igbc);

        // Split Pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, resultsScrollPane, inspectorPanel);
        splitPane.setResizeWeight(0.65);

        // Status Bar
        statusLabel = new JLabel("Ready. Enter input and click 'Generate Permutations'.");
        statusLabel.setBorder(new EmptyBorder(4, 2, 2, 2));

        // Assembly
        add(topContainer, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Listeners
        generateButton.addActionListener(e -> onGenerate());
        copyButton.addActionListener(e -> onCopy());
        clearButton.addActionListener(e -> onClear());
        helpButton.addActionListener(e -> onHelp());

        outputArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                onLineSelected();
            }
        });
    }

    public void setInputText(String text) {
        if (text != null) {
            inputField.setText(text.trim());
            statusLabel.setText("Loaded target input from Burp context.");
        }
    }

    private void onLineSelected() {
        try {
            int caretPos = outputArea.getCaretPosition();
            int lineNum = outputArea.getLineOfOffset(caretPos);
            int start = outputArea.getLineStartOffset(lineNum);
            int end = outputArea.getLineEndOffset(lineNum);
            String line = outputArea.getText(start, end - start).trim();

            if (!line.isEmpty() && !line.startsWith("[Notice]")) {
                ParserSimulator.SimulationResult sim = simulator.simulate(line, inputField.getText().trim());
                simUrlField.setText(sim.urlDecoded());
                simNfkcField.setText(sim.nfkcNormalized());
                simHtmlField.setText(sim.htmlUnescaped());

                if (sim.matchesOriginal()) {
                    simMatchBadge.setText("MATCH (Downstream parser normalizes to original target)");
                    simMatchBadge.setForeground(new Color(0, 130, 50));
                } else {
                    simMatchBadge.setText("NO DIRECT MATCH (Downstream parser treats as distinct token)");
                    simMatchBadge.setForeground(Color.DARK_GRAY);
                }
            }
        } catch (Exception ignored) {}
    }

    private void onGenerate() {
        String input = inputField.getText().trim();
        boolean isZeroInput = input.isEmpty();

        InputType selectedType = switch (typeSelector.getSelectedIndex()) {
            case 1 -> InputType.GENERIC_STRING;
            case 2 -> InputType.NUMERIC;
            case 3 -> InputType.JSON;
            case 4 -> InputType.EMAIL;
            case 5 -> InputType.URL_PATH;
            case 6 -> InputType.PHONE_NUMBER;
            default -> null; // Auto-Detect
        };

        GranularityMode selectedGranularity = switch (granularitySelector.getSelectedIndex()) {
            case 1 -> GranularityMode.SINGLE_POSITION;
            case 2 -> GranularityMode.COMBINATORIAL;
            default -> GranularityMode.TOKEN_ONLY;
        };

        boolean allowNonPrintable = allowNonPrintableBox.isSelected();
        boolean allowNullBytes = allowNullBytesBox.isSelected();
        boolean canonicalize = canonicalizeBox.isSelected();
        boolean preserveStructure = preserveStructureBox.isSelected();
        int layers = (Integer) layersSpinner.getValue();
        int maxPerms = (Integer) maxPermsSpinner.getValue();
        int depth = (Integer) maxDepthSpinner.getValue();

        generateButton.setEnabled(false);
        statusLabel.setText(isZeroInput ? "Generating boundary archetype seeds..." : "Generating permutations...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                ConstraintProfile profile = ConstraintProfile.builder()
                        .inputType(selectedType)
                        .granularityMode(selectedGranularity)
                        .allowNonPrintable(allowNonPrintable)
                        .allowNullBytes(allowNullBytes)
                        .preserveStructure(preserveStructure)
                        .canonicalizePreEncoded(canonicalize)
                        .encodingLayers(layers)
                        .maxDepth(depth)
                        .maxPermutations(maxPerms)
                        .build();
                return engine.generate(input, profile);
            }

            @Override
            protected void done() {
                try {
                    List<String> results = get();
                    if (results.isEmpty()) {
                        outputArea.setText("[Notice] No permutations met the active constraints.\n"
                                + "Possible cause: Input does not conform to the selected type structure (e.g. Email / JSON) with preserveStructure enabled.");
                        statusLabel.setText("0 permutations generated (filtered by constraints).");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (String res : results) {
                            sb.append(res).append(System.lineSeparator());
                        }
                        outputArea.setText(sb.toString());
                        outputArea.setCaretPosition(0);
                        statusLabel.setText(String.format("Generated %d %s.",
                                results.size(),
                                isZeroInput ? "archetype boundary seeds" : "distinct permutations"));
                    }
                } catch (Exception ex) {
                    outputArea.setText("Error generating permutations: " + ex.getMessage());
                    statusLabel.setText("Execution failed: " + ex.getMessage());
                } finally {
                    generateButton.setEnabled(true);
                }
            }
        };
        worker.execute();
    }

    private void onHelp() {
        String helpMessage = """
                INPUT MUTATOR - ENGINE GUIDE
                --------------------------------------------------------------------------------
                A constraint-guided permutation engine designed for input validation auditing,
                parser boundary analysis, and normalization testing.

                1. INPUT TYPES:
                   - Auto-Detect: Detects JSON, Email, Numeric, or URL/Path automatically.
                   - Generic String: Freeform text, tokens, and query parameters.
                   - Numeric: Integers, floats, scientific notation, and alternate radices.
                   - JSON: Parses keys & values; preserves structural braces/brackets.
                   - Email: Preserves RFC structure while permuting local-part.
                   - URL / Path: Targets path traversal, dot segments, and URL encodings.

                2. ZERO-INPUT / ARCHETYPE SEEDS:
                   - Leaving target input empty automatically generates boundary archetype seeds
                     tailored specifically to the chosen Input Type (e.g. 0, -1, NaN, IP emails, traversal paths).

                3. GRANULARITY MODES:
                   - Token-Level (Default):
                     Permutes tokens as whole units (e.g. 'admin' -> all-homoglyph or all-encoded).
                   - Single Character (Sliding Window):
                     Mutates one character at a time across the input (e.g. '%61dmin', 'a%64min').
                     Ideal for finding character-level keyword or regex filter gaps.
                   - Combinatorial (Multi-Char):
                     Permutes combinations of character positions simultaneously with asymmetric transforms.

                4. ADVANCED CONSTRAINT CONTROLS:
                   - Allow Controls / Surrogates: Enables lone surrogates (\\uD800) and C0/C1 codes.
                   - Allow Raw NUL: Permits unescaped null bytes (\\0).
                   - Canonicalize Pre-Encoded: Decodes pre-transformed inputs before mutating.
                   - Encoding Layers: Allows double/triple nested encodings.
                   - Max Output / Depth: Configures generation limits and recursion depth.

                5. LIVE DIFFERENTIAL PARSER INSPECTOR:
                   - Click any permutation in the results view to inspect how downstream backends
                     normalize it in real time (URL decode, NFKC normalization, HTML unescape).

                6. BURP SUITE INTEGRATION:
                   - Right-click selected text in Repeater/Proxy -> "Send to Input Mutator".
                   - In Burp Intruder, select "Input Mutator - Constraint Engine" as the Payload Generator.
                """;

        JTextArea helpArea = new JTextArea(helpMessage);
        helpArea.setEditable(false);
        helpArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        helpArea.setCaretPosition(0);

        JScrollPane scrollPane = new JScrollPane(helpArea);
        scrollPane.setPreferredSize(new Dimension(740, 480));

        JOptionPane.showMessageDialog(this, scrollPane, "Input Mutator - User Guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private void onCopy() {
        String text = outputArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
            statusLabel.setText("Copied output to clipboard.");
        }
    }

    private void onClear() {
        inputField.setText("");
        outputArea.setText("");
        simUrlField.setText("");
        simNfkcField.setText("");
        simHtmlField.setText("");
        simMatchBadge.setText("[Select a permutation]");
        statusLabel.setText("Cleared.");
    }
}
