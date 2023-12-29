/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
