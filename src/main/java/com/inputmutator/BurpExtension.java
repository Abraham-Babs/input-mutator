package com.inputmutator;

import burp.api.montoya.MontoyaApi;
import com.inputmutator.ui.MainPanel;

public class BurpExtension implements burp.api.montoya.BurpExtension {

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("Input Mutator");

        MainPanel mainPanel = new MainPanel();
        api.userInterface().applyThemeToComponent(mainPanel);
        api.userInterface().registerSuiteTab("Input Mutator", mainPanel);

        // Register Context Menu in Repeater, Proxy, Intruder
        api.userInterface().registerContextMenuItemsProvider(new com.inputmutator.burp.MutatorContextMenuProvider(api, mainPanel));

        // Register custom Payload Generator for Burp Intruder
        api.intruder().registerPayloadGeneratorProvider(new com.inputmutator.burp.MutatorPayloadGeneratorProvider());

        api.logging().logToOutput("Input Mutator extension, Context Menu, and Intruder Generator loaded successfully.");
    }
}
