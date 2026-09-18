package com.inputmutator.ui;

import javax.swing.*;
import java.awt.*;

public class StandaloneFrame {

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Fallback to default L&F
            }

            JFrame frame = new JFrame("Input Mutator");
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setMinimumSize(new Dimension(650, 450));
            frame.setPreferredSize(new Dimension(800, 550));

            MainPanel mainPanel = new MainPanel();
            frame.setContentPane(mainPanel);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
