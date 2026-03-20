/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.util.Objects;

class DisplayGui extends DialogBase {
    private final GUI gui;
    private final MemoryContext<Byte> memory;
    private final DisplayPanel displayPanel;

    DisplayGui(JFrame parent, MemoryContext<Byte> memory, DisplayPanel displayPanel, GUI gui) {
        super(parent, "SSEM CRT Display", false);
        this.gui = gui;

        this.memory = Objects.requireNonNull(memory);
        this.displayPanel = Objects.requireNonNull(displayPanel);

        displayPanel.reset(memory);
        initListener();
        buildContent();
    }

    private void initListener() {
        memory.addMemoryListener(new MemoryContext.MemoryListener() {
            @Override
            public void memoryContentChanged(int fromLocation, int toLocation) {
                if (fromLocation == -1) {
                    displayPanel.reset(memory);
                } else {
                    for (int location = fromLocation; location < toLocation; location++) {
                        int row = location / 4;
                        int rowBytePosition = row * 4;
                        displayPanel.writeRow(memory.read(rowBytePosition, 4), row);
                    }
                }
            }

            @Override
            public void memorySizeChanged() {
                // never happens
            }
        });
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel content = gui.panel("insets dialog", "[432:432:,grow]", "[416:416:,grow]");
        content.add(new JScrollPane(displayPanel), "grow");
        return content;
    }
}
