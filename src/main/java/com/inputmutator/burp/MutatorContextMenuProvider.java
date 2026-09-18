package com.inputmutator.burp;

import burp.api.montoya.MontoyaApi;
import burp.api.montoya.ui.contextmenu.ContextMenuEvent;
import burp.api.montoya.ui.contextmenu.ContextMenuItemsProvider;
import com.inputmutator.ui.MainPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.List;

/**
 * Registers right-click context menu options across Burp Repeater, Proxy, and Intruder.
 */
public class MutatorContextMenuProvider implements ContextMenuItemsProvider {

    private final MontoyaApi api;
    private final MainPanel mainPanel;

    public MutatorContextMenuProvider(MontoyaApi api, MainPanel mainPanel) {
        this.api = api;
        this.mainPanel = mainPanel;
    }

    @Override
    public List<Component> provideMenuItems(ContextMenuEvent event) {
        // Extract selected text if available
        String selectedText = event.messageEditorRequestResponse()
                .flatMap(editor -> editor.selectionOffsets().map(offsets -> {
                    byte[] data = editor.requestResponse().request().toByteArray().getBytes();
                    int start = offsets.startIndexInclusive();
                    int end = offsets.endIndexExclusive();
                    if (start >= 0 && end <= data.length && end > start) {
                        return new String(data, start, end - start, java.nio.charset.StandardCharsets.UTF_8);
                    }
                    return null;
                })).orElse(null);

        if (selectedText == null || selectedText.isBlank()) {
            return Collections.emptyList();
        }

        JMenuItem sendToMutatorItem = new JMenuItem("Send to Input Mutator");
        sendToMutatorItem.addActionListener(e -> {
            mainPanel.setInputText(selectedText);
            api.logging().logToOutput("Target input sent to Input Mutator: " + (selectedText.length() > 30 ? selectedText.substring(0, 30) + "..." : selectedText));
        });

        return List.of(sendToMutatorItem);
    }
}
