package com.inputmutator.ui;

import com.inputmutator.engine.constraint.CharacterSetConstraint;
import com.inputmutator.engine.constraint.ConstraintProfile;
import com.inputmutator.engine.model.ArtifactCategory;
import com.inputmutator.engine.model.GranularityMode;
import com.inputmutator.engine.model.InputType;
import com.inputmutator.engine.model.TestingIntent;
import com.inputmutator.engine.pipeline.MutationEngine;
import com.inputmutator.engine.pipeline.ParserSimulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MainPanel extends JPanel {

    private final JTextField inputField;
    private final JComboBox<String> typeSelector;
    private final JComboBox<String> intentSelector;
    private final JComboBox<String> charsetSelector;
    private final JComboBox<String> granularitySelector;
    private final Map<ArtifactCategory, JCheckBox> categoryBoxes = new EnumMap<>(ArtifactCategory.class);
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
    private final JButton cancelButton;
    private final JButton exportButton;
    private final JLabel conflictNoticeLabel;
    private final MutationEngine engine;
    private final ParserSimulator simulator;
    private final Timer inspectorDebounceTimer;
    private SwingWorker<List<String>, Void> currentWorker;
    private SwingWorker<ParserSimulator.SimulationResult, Void> activeInspectorWorker;

    // Differential Parser Inspector fields
    private final JTextField simUrlField;
    private final JTextField simNfkcField;
    private final JTextField simHtmlField;
    private final JLabel simMatchBadge;

    public MainPanel() {
        this.engine = new MutationEngine();
        this.simulator = new ParserSimulator();
        this.inspectorDebounceTimer = new Timer(75, e -> runAsyncSimulation());
        this.inspectorDebounceTimer.setRepeats(false);

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

        // Tab 1: Target & Strategy
        JPanel targetTab = new JPanel(new GridBagLayout());
        targetTab.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints tgbc = new GridBagConstraints();
        tgbc.fill = GridBagConstraints.HORIZONTAL;
        tgbc.insets = new Insets(3, 4, 3, 4);

        tgbc.gridx = 0; tgbc.gridy = 0; tgbc.weightx = 0;
        targetTab.add(createLabelWithHelp("Target Input:",
                "Target string to mutate. Leave empty to synthesize data-type boundary archetype seeds (e.g. NaN, IP emails, traversal paths)."), tgbc);

        inputField = new JTextField();
        inputField.setToolTipText("Target string to mutate (or leave empty to synthesize boundary archetype seeds)");
        tgbc.gridx = 1; tgbc.gridy = 0; tgbc.weightx = 1.0;
        targetTab.add(inputField, tgbc);

        tgbc.gridx = 0; tgbc.gridy = 1; tgbc.weightx = 0;
        targetTab.add(createLabelWithHelp("Input Type:",
                "Input semantic structure (Numeric, JSON, Email, URL/Path, Phone). Directs tokenizer and preserves format syntax boundaries."), tgbc);

        typeSelector = new JComboBox<>(new String[]{
                "Auto-Detect", "Generic String", "Numeric", "JSON", "Email", "URL / Path", "Phone Number"
        });
        typeSelector.setToolTipText("Input semantic structure (Auto-Detect, Numeric, JSON, Email, URL/Path, Phone Number)");
        tgbc.gridx = 1; tgbc.gridy = 1; tgbc.weightx = 1.0;
        targetTab.add(typeSelector, tgbc);

        tgbc.gridx = 0; tgbc.gridy = 2; tgbc.weightx = 0;
        targetTab.add(createLabelWithHelp("Testing Intent:",
                "Operational audit strategy: Whitelist Auditing (strict spec compliance), Blacklist Evasion (WAF/filter bypass), or Parser Differential (desync vectors)."), tgbc);

        intentSelector = new JComboBox<>(new String[]{
                "Blacklist Evasion (Bypass WAF / Keyword Filters)",
                "Whitelist Auditing (Strict Spec & Character Boundaries)",
                "Parser Differential (Encoding Desync & Norm Flaws)"
        });
        intentSelector.setToolTipText("Testing intent guiding baseline artifact categories and strategy selection");
        intentSelector.addActionListener(e -> updateDefaultCategoriesForIntent());
        tgbc.gridx = 1; tgbc.gridy = 2; tgbc.weightx = 1.0;
        targetTab.add(intentSelector, tgbc);

        tgbc.gridx = 0; tgbc.gridy = 3; tgbc.weightx = 0;
        targetTab.add(createLabelWithHelp("Granularity:",
                "Permutation scope: Token-Level (whole token), Single Character (sliding-window across all positions), or Combinatorial (multi-position subsets)."), tgbc);

        granularitySelector = new JComboBox<>(new String[]{
                "Token-Level (Default)", "Single Character (Sliding)", "Combinatorial (Multi-Char)"
        });
        granularitySelector.setToolTipText("Permutation scope: Token-Level (fast), Single-Char (sliding-window), or Combinatorial (multi-char)");
        tgbc.gridx = 1; tgbc.gridy = 3; tgbc.weightx = 1.0;
        targetTab.add(granularitySelector, tgbc);

        // Tab 2: Constraints & Rules
        JPanel constraintsTab = new JPanel(new GridBagLayout());
        constraintsTab.setBorder(new EmptyBorder(8, 8, 8, 8));
        GridBagConstraints cgbc = new GridBagConstraints();
        cgbc.fill = GridBagConstraints.HORIZONTAL;
        cgbc.insets = new Insets(3, 4, 3, 4);

        cgbc.gridx = 0; cgbc.gridy = 0; cgbc.weightx = 0;
        constraintsTab.add(createLabelWithHelp("Charset Rule:",
                "Declarative character constraint: Any (unrestricted), ASCII Only (0-127), Alphanumeric (A-Z, a-z, 0-9), or Printable ASCII (0x20-0x7E)."), cgbc);

        charsetSelector = new JComboBox<>(new String[]{
                "Any Characters (Unrestricted)",
                "ASCII Only (0x00 - 0x7F)",
                "Alphanumeric (A-Z, a-z, 0-9)",
                "Printable ASCII (0x20 - 0x7E)"
        });
        charsetSelector.setToolTipText("Declarative character set boundary (Any, ASCII Only, Alphanumeric A-Z a-z 0-9, Printable ASCII)");
        charsetSelector.addActionListener(e -> checkConstraintConflicts());
        cgbc.gridx = 1; cgbc.gridy = 0; cgbc.weightx = 1.0;
        constraintsTab.add(charsetSelector, cgbc);

        cgbc.gridx = 0; cgbc.gridy = 1; cgbc.weightx = 0;
        constraintsTab.add(createLabelWithHelp("Rules & Flags:", "Structural integrity and byte filtering flags."), cgbc);

        JPanel flagsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        canonicalizeBox = new JCheckBox("Canonicalize Pre-Encoded", true);
        canonicalizeBox.setToolTipText("Auto-detect and canonicalize pre-encoded inputs before applying targeted mutations");
        preserveStructureBox = new JCheckBox("Preserve Structure", true);
        preserveStructureBox.setToolTipText("Preserve protocol/syntax structure (e.g. valid RFC email, balanced JSON brackets/quotes)");
        allowNonPrintableBox = new JCheckBox("Allow Controls / Surrogates", false);
        allowNonPrintableBox.setToolTipText("Allow unescaped C0/C1 control codes (0x00-0x1F, 0x80-0x9F, 0x7F) and lone surrogate codepoints");
        allowNullBytesBox = new JCheckBox("Allow Raw NUL (\\0)", false);
        allowNullBytesBox.setToolTipText("Permit raw unescaped NUL (\\0) bytes in generated outputs");

        flagsRow.add(createBoxWithHelp(canonicalizeBox, "Auto-detect and canonicalize pre-encoded inputs before applying targeted mutations"));
        flagsRow.add(createBoxWithHelp(preserveStructureBox, "Preserve protocol/syntax structure (e.g. valid RFC email, balanced JSON brackets/quotes)"));
        flagsRow.add(createBoxWithHelp(allowNonPrintableBox, "Allow unescaped C0/C1 control codes (0x00-0x1F, 0x80-0x9F, 0x7F) and lone surrogate codepoints"));
        flagsRow.add(createBoxWithHelp(allowNullBytesBox, "Permit raw unescaped NUL (\\0) bytes in generated outputs"));

        cgbc.gridx = 1; cgbc.gridy = 1; cgbc.weightx = 1.0;
        constraintsTab.add(flagsRow, cgbc);

        conflictNoticeLabel = new JLabel(" ");
        conflictNoticeLabel.setForeground(new Color(180, 50, 0));
        conflictNoticeLabel.setFont(conflictNoticeLabel.getFont().deriveFont(Font.BOLD, 11f));
        cgbc.gridx = 1; cgbc.gridy = 2; cgbc.weightx = 1.0;
        constraintsTab.add(conflictNoticeLabel, cgbc);

        // Tab 3: Artifact Categories
        JPanel categoryPanel = new JPanel(new GridLayout(2, 4, 6, 2));
        categoryPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        for (ArtifactCategory cat : ArtifactCategory.values()) {
            String label = switch (cat) {
                case URL_ENCODING -> "URL";
                case OVERLONG_UTF8 -> "Overlong UTF-8";
                case UNICODE_HOMOGLYPHS -> "Unicode / Homoglyphs";
                case HTML_ENTITIES -> "HTML";
                case NUMERIC_RADIX -> "Numeric Radix";
                case GRAMMAR_DIFFERENTIAL -> "Grammar Differentials";
                case CONTROL_NON_PRINTABLE -> "Controls / Nulls";
            };
            String tooltip = switch (cat) {
                case URL_ENCODING -> "URL percent-encoding (%xx, double %25xx, triple %2525xx) for ASCII and special characters";
                case OVERLONG_UTF8 -> "Multi-byte overlong UTF-8 encodings for ASCII characters (%c0%af, %e0%80%af) targeting bypass of naive decoders";
                case UNICODE_HOMOGLYPHS -> "Unicode lookalikes, Cyrillic/Greek confusables, NFKC compatibility expansions, and fullwidth text";
                case HTML_ENTITIES -> "HTML entity representations: decimal (&#0047;), hex (&#x2F;), and named entities (&sol;)";
                case NUMERIC_RADIX -> "Numeric alternate radices (0x hex, 0o octal, 0b binary), scientific notation, and IEEE 754 specials (NaN, Infinity)";
                case GRAMMAR_DIFFERENTIAL -> "Parser differentials: path matrix params (;/;param=1), dot-segments (..;/), email comments, and JSON escapes";
                case CONTROL_NON_PRINTABLE -> "Controls & invisibles: zero-width spaces (\\u200B), soft hyphens, C0/C1 control codes, and raw null bytes (\\0)";
            };
            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            itemPanel.setOpaque(false);
            JCheckBox box = new JCheckBox(label);
            box.setToolTipText(tooltip);
            box.addActionListener(e -> checkConstraintConflicts());
            JLabel q = createHelpIcon(tooltip);
            itemPanel.add(box);
            itemPanel.add(q);
            categoryBoxes.put(cat, box);
            categoryPanel.add(itemPanel);
        }
        updateDefaultCategoriesForIntent();

        // Assemble Tabbed Pane
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Target & Strategy", targetTab);
        tabbedPane.addTab("Constraints & Rules", constraintsTab);
        tabbedPane.addTab("Artifact Families", categoryPanel);

        // Bottom Action & Options Bar
        JPanel actionBar = new JPanel(new BorderLayout(8, 4));

        JPanel spinnersRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        layersSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 3, 1));
        layersSpinner.setToolTipText("Nested encoding layers for escape transformations (1 = single, 2 = double, 3 = triple)");
        spinnersRow.add(createLabelWithHelp("Layers:", "Nested encoding layers (1 = single %xx, 2 = double %25xx, 3 = triple %2525xx)"));
        spinnersRow.add(layersSpinner);

        maxPermsSpinner = new JSpinner(new SpinnerNumberModel(150, 0, 100000, 25));
        maxPermsSpinner.setToolTipText("Maximum unique mutations to output (0 = Natural Exhaustion / uncapped wordlist)");
        spinnersRow.add(createLabelWithHelp("Max Output:", "Maximum unique mutations to emit. Set to 0 for unlimited / natural graph exhaustion."));
        spinnersRow.add(maxPermsSpinner);

        maxDepthSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 4, 1));
        maxDepthSpinner.setToolTipText("Maximum recursive transformation depth for chained mutations (1 - 4)");
        spinnersRow.add(createLabelWithHelp("Depth:", "Maximum recursive transformation depth for chained mutations (1 - 4)"));
        spinnersRow.add(maxDepthSpinner);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 2));
        generateButton = new JButton("Generate Permutations");
        generateButton.setToolTipText("Execute mutation pipeline with active constraints and selected testing intent");
        cancelButton = new JButton("Cancel");
        cancelButton.setEnabled(false);
        cancelButton.setToolTipText("Abort the current permutation generation process");
        JButton copyButton = new JButton("Copy Output");
        copyButton.setToolTipText("Copy all generated permutation results to system clipboard");
        exportButton = new JButton("Export to File");
        exportButton.setToolTipText("Save generated permutations directly to a file without clipboard size constraints");
        JButton clearButton = new JButton("Clear");
        clearButton.setToolTipText("Clear the target input, results list, and differential inspector fields");
        JButton helpButton = new JButton("Help & Guide (?)");
        helpButton.setToolTipText("Open the complete engine guide, testing intents, and mutation matrix");

        buttonPanel.add(generateButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(copyButton);
        buttonPanel.add(exportButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(helpButton);

        actionBar.add(spinnersRow, BorderLayout.WEST);
        actionBar.add(buttonPanel, BorderLayout.EAST);

        JPanel topContainer = new JPanel(new BorderLayout(0, 4));
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(tabbedPane, BorderLayout.CENTER);
        topContainer.add(actionBar, BorderLayout.SOUTH);
        topContainer.setMinimumSize(new Dimension(500, 150));

        JScrollPane topScrollPane = new JScrollPane(topContainer);
        topScrollPane.setBorder(BorderFactory.createEmptyBorder());
        topScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        topScrollPane.getHorizontalScrollBar().setUnitIncrement(16);
        topScrollPane.setMinimumSize(new Dimension(500, 140));

        // Output Area
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        JScrollPane resultsScrollPane = new JScrollPane(outputArea);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("Permutation Results (click any line to inspect normalization)"));
        resultsScrollPane.setMinimumSize(new Dimension(280, 150));

        // Differential Parser Inspector Panel
        JPanel inspectorPanel = new JPanel(new GridBagLayout());
        inspectorPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Live Differential Parser Inspector"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        inspectorPanel.setMinimumSize(new Dimension(240, 150));
        GridBagConstraints igbc = new GridBagConstraints();
        igbc.fill = GridBagConstraints.HORIZONTAL;
        igbc.insets = new Insets(5, 5, 5, 5);

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
        inspectorPanel.add(new JLabel("Target Match:"), igbc);
        simMatchBadge = new JLabel("[Select a permutation]");
        simMatchBadge.setFont(simMatchBadge.getFont().deriveFont(Font.BOLD));
        igbc.gridx = 1; igbc.gridy = 3; igbc.weightx = 1.0;
        inspectorPanel.add(simMatchBadge, igbc);

        // Vertical trailing spacer so inspector fields stay pinned cleanly to top
        igbc.gridx = 0; igbc.gridy = 4; igbc.gridwidth = 2; igbc.weighty = 1.0;
        igbc.fill = GridBagConstraints.BOTH;
        inspectorPanel.add(new JPanel() {{ setOpaque(false); }}, igbc);

        JScrollPane inspectorScrollPane = new JScrollPane(inspectorPanel);
        inspectorScrollPane.setBorder(BorderFactory.createEmptyBorder());
        inspectorScrollPane.setMinimumSize(new Dimension(240, 150));
        inspectorScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // Bottom Split Pane (Results on Left, Inspector on Right)
        JSplitPane bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, resultsScrollPane, inspectorScrollPane);
        bottomSplitPane.setResizeWeight(0.58);
        bottomSplitPane.setOneTouchExpandable(true);
        bottomSplitPane.setContinuousLayout(true);
        bottomSplitPane.setMinimumSize(new Dimension(520, 150));

        // Master Vertical Split Pane (Controls on Top, Results/Inspector on Bottom)
        JSplitPane masterSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topScrollPane, bottomSplitPane);
        masterSplitPane.setResizeWeight(0.40);
        masterSplitPane.setOneTouchExpandable(true);
        masterSplitPane.setContinuousLayout(true);

        // Status Bar
        statusLabel = new JLabel("Ready. Enter input and click 'Generate Permutations'.");
        statusLabel.setBorder(new EmptyBorder(4, 2, 2, 2));

        // Assembly
        add(masterSplitPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);

        // Listeners
        generateButton.addActionListener(e -> onGenerate());
        cancelButton.addActionListener(e -> onCancel());
        copyButton.addActionListener(e -> onCopy());
        exportButton.addActionListener(e -> onExport());
        clearButton.addActionListener(e -> onClear());
        helpButton.addActionListener(e -> onHelp());

        outputArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                onLineSelected();
            }
        });
    }

    private JPanel createLabelWithHelp(String text, String tooltip) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        p.setOpaque(false);
        JLabel lbl = new JLabel(text);
        JLabel q = createHelpIcon(tooltip);
        p.add(lbl);
        p.add(q);
        return p;
    }

    private JLabel createHelpIcon(String tooltip) {
        JLabel badge = new JLabel("(?)");
        badge.setFont(badge.getFont().deriveFont(Font.BOLD, 10f));
        badge.setForeground(new Color(60, 110, 180));
        badge.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        badge.setToolTipText(tooltip);
        return badge;
    }

    private JPanel createBoxWithHelp(JCheckBox box, String tooltip) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setOpaque(false);
        p.add(box);
        p.add(createHelpIcon(tooltip));
        return p;
    }

    public void setInputText(String text) {
        if (text != null) {
            inputField.setText(text.trim());
            statusLabel.setText("Loaded target input from Burp context.");
        }
    }

    private void onLineSelected() {
        inspectorDebounceTimer.restart();
    }

    private void runAsyncSimulation() {
        try {
            int caretPos = outputArea.getCaretPosition();
            int lineNum = outputArea.getLineOfOffset(caretPos);
            int start = outputArea.getLineStartOffset(lineNum);
            int end = outputArea.getLineEndOffset(lineNum);
            String line = outputArea.getText(start, end - start).trim();
            String original = inputField.getText().trim();

            if (!line.isEmpty() && !line.startsWith("[Notice]")) {
                if (activeInspectorWorker != null && !activeInspectorWorker.isDone()) {
                    activeInspectorWorker.cancel(true);
                }
                activeInspectorWorker = new SwingWorker<>() {
                    @Override
                    protected ParserSimulator.SimulationResult doInBackground() {
                        return simulator.simulate(line, original);
                    }

                    @Override
                    protected void done() {
                        if (isCancelled()) return;
                        try {
                            ParserSimulator.SimulationResult sim = get();
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
                        } catch (Exception ignored) {}
                    }
                };
                activeInspectorWorker.execute();
            }
        } catch (Exception ignored) {}
    }

    private void updateDefaultCategoriesForIntent() {
        TestingIntent intent = getSelectedIntent();
        Set<ArtifactCategory> defaults = intent.defaultCategories();
        for (Map.Entry<ArtifactCategory, JCheckBox> entry : categoryBoxes.entrySet()) {
            entry.getValue().setSelected(defaults.contains(entry.getKey()));
        }
    }

    private TestingIntent getSelectedIntent() {
        return switch (intentSelector.getSelectedIndex()) {
            case 1 -> TestingIntent.WHITELIST_AUDITING;
            case 2 -> TestingIntent.PARSER_DIFFERENTIAL;
            default -> TestingIntent.BLACKLIST_EVASION;
        };
    }

    private CharacterSetConstraint getSelectedCharset() {
        return switch (charsetSelector.getSelectedIndex()) {
            case 1 -> CharacterSetConstraint.ASCII_ONLY;
            case 2 -> CharacterSetConstraint.ALPHANUMERIC;
            case 3 -> CharacterSetConstraint.PRINTABLE_ASCII;
            default -> CharacterSetConstraint.ANY;
        };
    }

    private Set<ArtifactCategory> getSelectedCategories() {
        Set<ArtifactCategory> set = EnumSet.noneOf(ArtifactCategory.class);
        for (Map.Entry<ArtifactCategory, JCheckBox> entry : categoryBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    private void checkConstraintConflicts() {
        CharacterSetConstraint charset = getSelectedCharset();
        Set<ArtifactCategory> cats = getSelectedCategories();
        if (charset == CharacterSetConstraint.ALPHANUMERIC) {
            conflictNoticeLabel.setText("Notice: Alphanumeric charset will filter out encodings, entities, and symbols.");
        } else if (charset == CharacterSetConstraint.ASCII_ONLY && cats.contains(ArtifactCategory.UNICODE_HOMOGLYPHS)) {
            conflictNoticeLabel.setText("Notice: ASCII Only charset will filter out non-ASCII Unicode homoglyphs.");
        } else {
            conflictNoticeLabel.setText(" ");
        }
    }

    public ConstraintProfile getActiveProfile() {
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

        return ConstraintProfile.builder()
                .inputType(selectedType)
                .granularityMode(selectedGranularity)
                .testingIntent(getSelectedIntent())
                .characterSetConstraint(getSelectedCharset())
                .enabledCategories(getSelectedCategories())
                .allowNonPrintable(allowNonPrintableBox.isSelected())
                .allowNullBytes(allowNullBytesBox.isSelected())
                .preserveStructure(preserveStructureBox.isSelected())
                .canonicalizePreEncoded(canonicalizeBox.isSelected())
                .encodingLayers((Integer) layersSpinner.getValue())
                .maxDepth((Integer) maxDepthSpinner.getValue())
                .maxPermutations((Integer) maxPermsSpinner.getValue())
                .build();
    }

    private void onGenerate() {
        String input = inputField.getText().trim();
        boolean isZeroInput = input.isEmpty();
        ConstraintProfile profile = getActiveProfile();

        generateButton.setEnabled(false);
        cancelButton.setEnabled(true);
        statusLabel.setText(isZeroInput ? "Generating boundary archetype seeds..." : "Generating permutations...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                return engine.generate(input, profile);
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    generateButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    return;
                }
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
                    cancelButton.setEnabled(false);
                }
            }
        };
        currentWorker.execute();
    }

    private void onCancel() {
        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
            statusLabel.setText("Generation cancelled by user.");
            generateButton.setEnabled(true);
            cancelButton.setEnabled(false);
        }
    }

    private void onExport() {
        String text = outputArea.getText();
        if (text.isEmpty() || text.startsWith("[Notice]")) {
            JOptionPane.showMessageDialog(this, "No permutations to export.", "Export Notice", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Permutations to File");
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                writer.write(text);
                statusLabel.setText("Exported permutations to " + file.getName() + ".");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void onHelp() {
        String helpMessage = """
                INPUT MUTATOR - ENGINE GUIDE & REFERENCE
                ================================================================================
                A constraint-guided permutation engine designed for input validation auditing,
                parser boundary analysis, differential desync testing, and signature evasion.

                1. TESTING INTENTS:
                   - Blacklist Evasion (Default):
                     Obfuscates forbidden keywords and delimiters (URL encoding, overlong UTF-8,
                     homoglyphs, HTML entities) to evade signature filters and WAF rules.
                   - Whitelist Auditing:
                     Tests downstream normalization expansion against strict character and format
                     specifications (homoglyphs, radices, zero-width bounds).
                   - Parser Differential Hunting:
                     Targets proxy/backend desync via path matrix parameters (;/;param=1), dot
                     segments (..;/), RFC 5322 comment wrapping, and grammar ambiguities.

                2. CHARACTER SET CONSTRAINTS:
                   - Any Characters: Full Unicode spectrum without restriction.
                   - ASCII Only: Rejects any byte outside 0x00 - 0x7F.
                   - Alphanumeric: Enforces strict [A-Za-z0-9] boundary for alphanumeric-only fields.
                   - Printable ASCII: Limits output to standard printable characters (0x20 - 0x7E).

                3. ARTIFACT CATEGORIES:
                   - URL: Standard (%xx) and nested multi-layer (%25xx, %2525xx) percent-encoding.
                   - Overlong UTF-8: Multi-byte overlong UTF-8 encodings (%c0%af, %e0%80%af).
                   - Unicode / Homoglyphs: Cyrillic/Greek lookalikes, NFKC compatibility singletons.
                   - HTML: Overlong decimal (&#0047;), hex (&#x2F;), and named entities.
                   - Numeric Radix: Hex (0x), octal (0o, 0), binary (0b), scientific, IEEE 754 NaN.
                   - Grammar Differentials: Path matrix params, dot-segments, email comments/quotes.
                   - Controls / Nulls: Zero-width spaces, C0/C1 control codes, and raw null bytes.

                4. INPUT SEMANTICS & TYPES:
                   - Auto-Detect: Detects JSON, Email, Numeric, or URL/Path automatically.
                   - Generic String: Freeform text, tokens, and query parameters.
                   - Numeric: Integers, floats, scientific notation, and alternate radices.
                   - JSON: Parses keys & values; preserves structural braces/brackets.
                   - Email: Preserves RFC structure while permuting local-part.
                   - URL / Path: Targets path traversal, dot segments, and URL encodings.

                5. ZERO-INPUT / ARCHETYPE SEEDS:
                   - Leaving target input empty automatically generates boundary archetype seeds
                     tailored specifically to the chosen Input Type (e.g. 0, -1, NaN, IP emails, traversal paths).

                6. GRANULARITY MODES:
                   - Token-Level (Default): Permutes tokens as whole units.
                   - Single Character (Sliding Window): Mutates one character position at a time.
                   - Combinatorial (Multi-Char): Permutes multi-position combinations simultaneously.

                7. ADVANCED CONSTRAINT CONTROLS:
                   - Canonicalize Pre-Encoded: Decodes pre-transformed inputs before mutating.
                   - Preserve Structure: Preserves RFC syntax boundaries (balanced braces, emails).
                   - Allow Controls / Surrogates: Enables lone surrogates (\\uD800) and C0/C1 codes.
                   - Allow Raw NUL: Permits unescaped null bytes (\\0).
                   - Encoding Layers: Allows single, double, or triple nested encodings.
                   - Max Output: Maximum unique mutations to output (0 = Natural Exhaustion / Uncapped).
                   - Depth: Maximum recursive transformation depth for chained mutations.

                8. LIVE DIFFERENTIAL PARSER INSPECTOR:
                   - Click any permutation in the results view to inspect how downstream backends
                     normalize it in real time (URL decode, NFKC normalization, HTML unescape).

                9. BURP SUITE INTEGRATION:
                   - Right-click selected text in Repeater/Proxy -> "Send to Input Mutator".
                   - In Burp Intruder, select "Input Mutator - Constraint Engine" as the Payload Generator.
                ================================================================================
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
