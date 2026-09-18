package com.inputmutator;

import com.inputmutator.cli.CliRunner;
import com.inputmutator.ui.StandaloneFrame;

import java.awt.GraphicsEnvironment;

public class Main {

    public static void main(String[] args) {
        if (args.length > 0 && !args[0].equalsIgnoreCase("--gui")) {
            CliRunner.run(args);
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Running in headless environment. Falling back to CLI mode.");
            CliRunner.run(args);
            return;
        }

        StandaloneFrame.launch();
    }
}
