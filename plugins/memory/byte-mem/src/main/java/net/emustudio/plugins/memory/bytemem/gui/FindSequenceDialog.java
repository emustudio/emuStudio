/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.memory.bytemem.gui.actions.FindSequenceAction;
import net.emustudio.plugins.memory.bytemem.gui.model.MemoryTableModel;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class FindSequenceDialog extends JDialog {
    private final FindSequenceAction findSequenceAction;

    public FindSequenceDialog(Dialogs dialogs, JDialog parent, MemoryTableModel tableModel, int currentAddress,
                              Consumer<Integer> setFoundAddress) {
        super(parent, true);
        setLocationRelativeTo(parent);

        this.findSequenceAction = new FindSequenceAction(
            dialogs, this::dispose, tableModel, setFoundAddress, radioCurrentPage::isSelected,
            radioPlainText::isSelected, currentAddress, txtPosition, txtSequence
        );
        initComponents();
    }

    private void initComponents() {
        ButtonGroup btnGroupSequenceToFind = new ButtonGroup();
        ButtonGroup btnGroupStartPosition = new ButtonGroup();
        JPanel jPanel1 = new JPanel();
        JRadioButton radioBytes = new JRadioButton();
        JPanel jPanel2 = new JPanel();
        JRadioButton radioSpecificPosition = new JRadioButton();
        JButton btnFind = new JButton(findSequenceAction);
        btnGroupSequenceToFind.add(radioPlainText);
        btnGroupSequenceToFind.add(radioBytes);

        btnGroupStartPosition.add(radioCurrentPage);
        btnGroupStartPosition.add(radioSpecificPosition);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().setDefaultButton(btnFind);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        setTitle("Find sequence");

        jPanel1.setBorder(BorderFactory.createTitledBorder("Sequence to find"));

        radioPlainText.setSelected(true);
        radioPlainText.setText("Plain text (case-sensitive)");

        radioBytes.setText("Sequence of bytes (space-separated)");

        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                        .addComponent(radioBytes, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(radioPlainText)
                        .addComponent(txtSequence))
                    .addContainerGap(42, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(txtSequence, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(radioPlainText)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(radioBytes)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(BorderFactory.createTitledBorder("Start position"));

        radioCurrentPage.setSelected(true);
        radioCurrentPage.setText("Current page");

        radioSpecificPosition.setText("Specific position:");

        txtPosition.setText("0");

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                            .addGap(21, 21, 21)
                            .addComponent(txtPosition))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addGroup(jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                .addComponent(radioCurrentPage)
                                .addComponent(radioSpecificPosition))
                            .addGap(0, 0, Short.MAX_VALUE)))
                    .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(jPanel2Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(radioCurrentPage)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(radioSpecificPosition)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(txtPosition, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(24, Short.MAX_VALUE))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addGap(0, 0, Short.MAX_VALUE)
                            .addComponent(btnFind, GroupLayout.PREFERRED_SIZE, 92, GroupLayout.PREFERRED_SIZE))
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(btnFind)
                    .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }


    private final JRadioButton radioCurrentPage = new JRadioButton();
    private final JRadioButton radioPlainText = new JRadioButton();
    private final JTextField txtPosition = new JTextField();
    private final JTextField txtSequence = new JTextField();
}
