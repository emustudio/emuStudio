/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence.PerformFindSequenceAction;
import net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence.PerformFindSequenceAction.Format;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

public final class FindSequencePanel extends JPanel {
    private final JTextField sequence = new JTextField();
    private final JTextField position = new JTextField("0", 8);
    private final JComboBox<String> format = new JComboBox<>(new String[]{
            "Plain text (case-sensitive)", "Sequence of bytes (space-separated)", "Number (decimal)"});
    private final JComboBox<String> numericFormat = new JComboBox<>(new String[]{
            "Integer 16-bit", "Integer 32-bit", "Integer 64-bit", "Float 32-bit (IEEE 754)", "Float 64-bit (IEEE 754)"});
    private final JComboBox<String> byteOrder = new JComboBox<>(new String[]{"Big endian", "Little endian"});
    private final JComboBox<String> startPosition = new JComboBox<>(new String[]{"Current page", "Specific position"});

    public FindSequencePanel(Dialogs dialogs, GUI gui, IntSupplier currentAddress,
                             BiConsumer<byte[], Integer> startSearch, Runnable close) {
        super(new BorderLayout());
        Objects.requireNonNull(dialogs);
        Objects.requireNonNull(gui);
        Objects.requireNonNull(currentAddress);
        Objects.requireNonNull(startSearch);
        Objects.requireNonNull(close);

        sequence.setName("sequence");
        format.setName("findFormat");
        numericFormat.setName("numberFormat");
        byteOrder.setName("byteOrder");
        startPosition.setName("startPosition");
        position.setName("position");

        Runnable find = () -> new PerformFindSequenceAction(dialogs, startSearch,
                () -> startPosition.getSelectedIndex() == 0, this::selectedFormat,
                () -> byteOrder.getSelectedIndex() == 0 ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN,
                currentAddress.getAsInt(), position, sequence).actionPerformed(null);
        JButton findButton = new JButton("Find");
        findButton.setName("findButton");
        findButton.addActionListener(e -> find.run());
        sequence.addActionListener(e -> find.run());
        position.addActionListener(e -> find.run());
        JButton closeButton = new JButton("Close");
        closeButton.setName("closeFind");
        closeButton.addActionListener(e -> close.run());

        JLabel sequenceLabel = gui.label("Find:");
        sequenceLabel.setLabelFor(sequence);
        JLabel formatLabel = gui.label("Format:");
        formatLabel.setLabelFor(format);
        JLabel startLabel = gui.label("Start:");
        startLabel.setLabelFor(startPosition);
        numericFormat.getAccessibleContext().setAccessibleName("Number format");
        byteOrder.getAccessibleContext().setAccessibleName("Byte order");
        position.getAccessibleContext().setAccessibleName("Specific start position");
        JPanel content = gui.panel("insets 4 8 4 8, fillx", "[][grow,fill][][]", "[][][]");
        content.add(sequenceLabel);
        content.add(sequence, "growx");
        content.add(findButton);
        content.add(closeButton, "wrap");
        content.add(formatLabel);
        content.add(format, "split 3");
        content.add(numericFormat);
        content.add(byteOrder, "wrap");
        content.add(startLabel);
        content.add(startPosition, "split 2");
        content.add(position, "wrap");
        add(content);

        format.addActionListener(e -> updateEnabled());
        startPosition.addActionListener(e -> updateEnabled());
        updateEnabled();
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeFind");
        getActionMap().put("closeFind", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { close.run(); }
        });
        setVisible(false);
    }

    public void showSearch() {
        setVisible(true);
        sequence.requestFocusInWindow();
    }

    private void updateEnabled() {
        boolean number = format.getSelectedIndex() == 2;
        numericFormat.setEnabled(number);
        byteOrder.setEnabled(number);
        position.setEnabled(startPosition.getSelectedIndex() == 1);
    }

    private Format selectedFormat() {
        switch (format.getSelectedIndex()) {
            case 0: return Format.TEXT;
            case 1: return Format.BYTES;
            default: return Format.values()[numericFormat.getSelectedIndex() + 2];
        }
    }
}
