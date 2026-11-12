/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.emulation.debugger.InstructionRecording;
import net.emustudio.emulib.Numbers;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.ui.components.DialogBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Frame;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

/** Configures a new instruction recording without changing the current recording on cancellation. */
public final class RecordingDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingDialog.class);
    private final GUI gui;
    private final DebuggerSession session;
    private final Consumer<List<InstructionRecording.Event>> started;
    private final JRadioButton keep = new JRadioButton("Keep", true);
    private final JRadioButton range = new JRadioButton("Specific address range:");
    private final JRadioButton unlimited = new JRadioButton("Unlimited");
    private final JComboBox<String> order = new JComboBox<>(new String[] {"first", "last"});
    private final JTextField count = new JTextField("1000", 6);
    private final JTextField from = new JTextField("0x0000", 10);
    private final JTextField to = new JTextField(10);

    public RecordingDialog(Frame owner, GUI gui, DebuggerSession session,
                           Consumer<List<InstructionRecording.Event>> started) {
        super(owner, "Start recording", true);
        this.gui = gui;
        this.session = session;
        this.started = started;
        to.setText(String.format("0x%04X", Math.max(0, session.cpu.getAddressSpaceSize() - 1)));
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = gui.panel("insets 8, fillx, wmin 356", "[grow]", "");
        panel.add(gui.label("Please specify the recording"), "gapbottom 16, wrap");
        ButtonGroup buttonGroup = new ButtonGroup();
        for (JRadioButton choice : new JRadioButton[] {keep, range, unlimited}) {
            buttonGroup.add(choice);
            choice.addItemListener(event -> updateFields());
        }
        JPanel keepRow = gui.panel("insets 0, gapx 6", "[][64!,fill][72!,fill][]", "");
        order.getAccessibleContext().setAccessibleName("Instruction order");
        count.getAccessibleContext().setAccessibleName("Instructions");
        keepRow.add(keep);
        keepRow.add(count, "hmin 22");
        keepRow.add(order, "hmin 22");
        keepRow.add(gui.label("instructions"));
        panel.add(keepRow, "growx, wrap");
        panel.add(range, "wrap");
        JPanel addresses = gui.panel("insets 6 26 0 0, gapy 6", "[38!][122!,fill]", "");
        addAddress(addresses, "From:", from);
        addAddress(addresses, "To:", to);
        panel.add(addresses, "growx, gapbottom 0, wrap");
        panel.add(unlimited, "wrap");
        panel.add(new JSeparator(), "growx, gaptop 2, gapbottom 8, wrap");
        updateFields();
        JButton start = gui.button("Start recording");
        start.addActionListener(e -> start());
        panel.add(start, "alignx right, hmin 22");
        getRootPane().setDefaultButton(start);
        return panel;
    }

    private void addAddress(JPanel panel, String text, JTextField field) {
        JLabel label = gui.label(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text.substring(0, text.length() - 1));
        field.setToolTipText("Inclusive instruction address; decimal, hexadecimal, binary, or octal");
        panel.add(label);
        panel.add(field, "hmin 22, wrap");
    }

    private void updateFields() {
        count.setEnabled(keep.isSelected());
        order.setEnabled(keep.isSelected());
        from.setEnabled(range.isSelected());
        to.setEnabled(range.isSelected());
    }

    private void start() {
        try {
            InstructionRecording.Mode selected = keep.isSelected()
                    ? (order.getSelectedIndex() == 0 ? InstructionRecording.Mode.FIRST : InstructionRecording.Mode.LAST)
                    : (range.isSelected() ? InstructionRecording.Mode.ADDRESS_RANGE : InstructionRecording.Mode.UNLIMITED);
            int limit = count.isEnabled() ? Numbers.parseRadix(count.getText().strip()) : 0;
            int first = from.isEnabled() ? Numbers.parseRadix(from.getText().strip()) : 0;
            int last = to.isEnabled() ? Numbers.parseRadix(to.getText().strip()) : 0;
            session.update(() -> {
                List<InstructionRecording.Event> previous = session.recording.events();
                session.startNewRecording(limit, selected, first, last);
                started.accept(previous);
            }).join();
            dispose();
        } catch (RuntimeException failure) {
            Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                    ? failure.getCause() : failure;
            LOGGER.warn(cause instanceof NumberFormatException
                    ? "Enter a valid instruction count or address."
                    : cause.getMessage(), cause);
        }
    }

}
