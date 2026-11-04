/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence.PerformFindSequenceAction;
import net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence.PerformFindSequenceAction.Format;

import javax.swing.*;
import java.nio.ByteOrder;
import java.util.function.BiConsumer;

public class FindSequenceDialog extends DialogBase {
    private final GUI gui;
    private final PerformFindSequenceAction performFindSequenceAction;
    private final JRadioButton radioCurrentPage = new JRadioButton();
    private final JRadioButton radioPlainText = new JRadioButton();
    private final JRadioButton radioBytes = new JRadioButton();
    private final JComboBox<String> numericFormat = new JComboBox<>(new String[]{"Integer 16-bit", "Integer 32-bit", "Integer 64-bit", "Float 32-bit (IEEE 754)", "Float 64-bit (IEEE 754)"});
    private final JComboBox<String> byteOrder = new JComboBox<>(new String[]{"Big endian", "Little endian"});
    private final JRadioButton radioNumber = new JRadioButton("Number (decimal):");
    private final JTextField txtPosition = new JTextField();
    private final JTextField txtSequence = new JTextField();

    public FindSequenceDialog(Dialogs dialogs, JDialog parent, int currentAddress,
                              BiConsumer<byte[], Integer> startSearch, GUI gui) {
        super(parent, "Find sequence", true);
        this.gui = gui;

        this.performFindSequenceAction = new PerformFindSequenceAction(
                dialogs, this::dispose, startSearch, radioCurrentPage::isSelected,
                this::selectedFormat, () -> byteOrder.getSelectedIndex() == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN,
                currentAddress, txtPosition, txtSequence
        );
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        ButtonGroup btnGroupSequenceToFind = new ButtonGroup();
        ButtonGroup btnGroupStartPosition = new ButtonGroup();
        JRadioButton radioSpecificPosition = new JRadioButton();
        JButton btnFind = new JButton(performFindSequenceAction);

        btnGroupSequenceToFind.add(radioPlainText);
        btnGroupSequenceToFind.add(radioBytes);
        btnGroupSequenceToFind.add(radioNumber);
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

        JPanel panelSequence = gui.section("Sequence to find", "insets dialog", "[grow]", "[25!]unrel[][]");
        panelSequence.add(txtSequence, "growx, wrap");
        panelSequence.add(radioPlainText, "wrap");
        panelSequence.add(radioBytes, "wrap");
        panelSequence.add(radioNumber, "wrap");
        panelSequence.add(numericFormat, "split 2, gapleft 21");
        panelSequence.add(byteOrder);
        numericFormat.addActionListener(e -> radioNumber.setSelected(true));
        byteOrder.addActionListener(e -> radioNumber.setSelected(true));

        JPanel panelPosition = gui.section("Start position", "insets dialog", "[grow]", "[][][25!]");
        panelPosition.add(radioCurrentPage, "wrap");
        panelPosition.add(radioSpecificPosition, "wrap");
        panelPosition.add(txtPosition, "gapleft 21, growx");

        JPanel content = gui.panel("insets dialog", "[grow]", "[]6[]unrel[]");
        content.add(panelSequence, "growx, wrap");
        content.add(panelPosition, "growx, wrap");
        content.add(btnFind, "w 92!, align right");
        return content;
    }

    private Format selectedFormat() {
        if (radioPlainText.isSelected()) return Format.TEXT;
        if (radioBytes.isSelected()) return Format.BYTES;
        return Format.values()[numericFormat.getSelectedIndex() + 2];
    }
}
