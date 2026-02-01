/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.plugins.memory.MemoryContext;

import javax.swing.*;
import java.util.Objects;

class DisplayGui extends JDialog {
    private final MemoryContext<Byte> memory;
    private final DisplayPanel displayPanel;
    private final JScrollPane scrollPane = new JScrollPane();

    DisplayGui(JFrame parent, MemoryContext<Byte> memory, DisplayPanel displayPanel) {
        super(parent);

        this.memory = Objects.requireNonNull(memory);
        this.displayPanel = Objects.requireNonNull(displayPanel);

        initComponents();
        setLocationRelativeTo(parent);

        scrollPane.setViewportView(displayPanel);
        displayPanel.reset(memory);
        initListener();
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

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("SSEM CRT Display");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 432, Short.MAX_VALUE)
                                .addContainerGap())
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(scrollPane, GroupLayout.DEFAULT_SIZE, 416, Short.MAX_VALUE)
                                .addContainerGap())
        );

        pack();
    }
}
