/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence.PerformFindSequenceAction;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import javax.swing.*;
import java.util.function.Consumer;

public class FindSequenceDialog extends DialogBase {
    private final PerformFindSequenceAction performFindSequenceAction;
    private final JRadioButton radioCurrentPage = new JRadioButton();
    private final JRadioButton radioPlainText = new JRadioButton();
    private final JTextField txtPosition = new JTextField();
    private final JTextField txtSequence = new JTextField();

    public FindSequenceDialog(Dialogs dialogs, JDialog parent, MemoryTableModel tableModel, int currentAddress,
                              Consumer<Integer> setFoundAddress) {
        super(parent, "Find sequence", true);

        this.performFindSequenceAction = new PerformFindSequenceAction(
                dialogs, this::dispose, tableModel, setFoundAddress, radioCurrentPage::isSelected,
                radioPlainText::isSelected, currentAddress, txtPosition, txtSequence
        );
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup btnGroupSequenceToFind = new ButtonGroup();
        ButtonGroup btnGroupStartPosition = new ButtonGroup();
        JRadioButton radioBytes = new JRadioButton();
        JRadioButton radioSpecificPosition = new JRadioButton();
        JButton btnFind = new JButton(performFindSequenceAction);

        btnGroupSequenceToFind.add(radioPlainText);
        btnGroupSequenceToFind.add(radioBytes);
        btnGroupStartPosition.add(radioCurrentPage);
        btnGroupStartPosition.add(radioSpecificPosition);

        btnFind.setText("Find");
        getRootPane().setDefaultButton(btnFind);

        radioPlainText.setSelected(true);
        radioPlainText.setText("Plain text (case-sensitive)");
        radioBytes.setText("Sequence of bytes (space-separated)");

        radioCurrentPage.setSelected(true);
        radioCurrentPage.setText("Current page");
        radioSpecificPosition.setText("Specific position:");
        txtPosition.setText("0");

        JPanel panelSequence = GUI.section("Sequence to find", "insets dialog", "[grow]", "[25!]unrel[][]");
        panelSequence.add(txtSequence, "growx, wrap");
        panelSequence.add(radioPlainText, "wrap");
        panelSequence.add(radioBytes);

        JPanel panelPosition = GUI.section("Start position", "insets dialog", "[grow]", "[][][25!]");
        panelPosition.add(radioCurrentPage, "wrap");
        panelPosition.add(radioSpecificPosition, "wrap");
        panelPosition.add(txtPosition, "gapleft 21, growx");

        JPanel content = GUI.panel("insets dialog", "[grow]", "[]6[]unrel[]");
        content.add(panelSequence, "growx, wrap");
        content.add(panelPosition, "growx, wrap");
        content.add(btnFind, "w 92!, align right");
        return content;
    }
}
