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
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.awt.Frame;
import java.nio.file.Path;
import java.util.Locale;

/** Configures a new instruction recording for the toolbar's Start action. */
public final class RecordingDialog extends DialogBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecordingDialog.class);
    private final GUI gui;
    private final DebuggerSession session;
    private final Runnable created;
    private final JCheckBox range = new JCheckBox("Specific address range");
    private final JComboBox<String> order = new JComboBox<>(new String[] {"Last", "First", "Until stopped (to file)"});
    private final JTextField count = new JTextField(Integer.toString(InstructionRecording.DEFAULT_LIMIT), 6);
    private final JTextField from = new JTextField("0x0000", 10);
    private final JTextField to = new JTextField(10);
    private final JLabel error = new JLabel();

    public RecordingDialog(Frame owner, GUI gui, DebuggerSession session, Runnable created) {
        super(owner, "New recording", true);
        this.gui = gui;
        this.session = session;
        this.created = created;
        to.setText(String.format("0x%04X", Math.max(0, session.cpu.getAddressSpaceSize() - 1)));
        setResizable(false);
        buildContent();
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel panel = gui.panel("insets 8, fillx, wmin 356", "[grow]", "");
        order.addActionListener(event -> updateFields());
        range.addItemListener(event -> updateFields());
        JPanel keepRow = gui.panel("insets 0, gapx 6", "[][pref!][64!,fill][]", "");
        order.getAccessibleContext().setAccessibleName("Instruction order");
        count.getAccessibleContext().setAccessibleName("Instructions");
        JLabel keepLabel = gui.label("Keep");
        keepLabel.setLabelFor(order);
        keepRow.add(keepLabel);
        keepRow.add(order, "hmin 22");
        keepRow.add(count, "hmin 22");
        keepRow.add(gui.label("instructions"));
        panel.add(keepRow, "growx, wrap");
        panel.add(range, "wrap");
        JPanel addresses = gui.panel("insets 6 26 0 0, gapy 6", "[38!][122!,fill]", "");
        addAddress(addresses, "From:", from);
        addAddress(addresses, "To:", to);
        panel.add(addresses, "growx, gapbottom 0, wrap");
        panel.add(error, "growx, wrap");
        panel.add(new JSeparator(), "growx, gaptop 2, gapbottom 8, wrap");
        updateFields();
        JButton create = gui.button("Create recording");
        create.addActionListener(e -> create());
        panel.add(create, "alignx right, hmin 22");
        getRootPane().setDefaultButton(create);
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
        count.setEnabled(order.getSelectedIndex() != 2);
        from.setEnabled(range.isSelected());
        to.setEnabled(range.isSelected());
    }

    private void create() {
        try {
            InstructionRecording.AddressRange addresses = range.isSelected()
                    ? new InstructionRecording.AddressRange(Numbers.parseRadix(from.getText().strip()),
                    Numbers.parseRadix(to.getText().strip())) : null;
            if (!count.isEnabled()) {
                Path file = chooseFile(this, "Save instruction recording", true);
                if (file == null) return;
                session.recording.prepare(file, addresses);
            } else {
                InstructionRecording.Mode selected = order.getSelectedIndex() == 0
                        ? InstructionRecording.Mode.LAST : InstructionRecording.Mode.FIRST;
                int limit = Numbers.parseRadix(count.getText().strip());
                session.recording.prepare(new InstructionRecording.Options(selected, limit, addresses));
            }
            created.run();
            dispose();
        } catch (RuntimeException failure) {
            String message = failure instanceof NumberFormatException
                    ? "Enter a valid instruction count or address."
                    : failure.getMessage();
            error.setText(message);
            pack();
            LOGGER.warn(message, failure);
        }
    }

    public static Path chooseFile(Component owner, String title, boolean save) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileFilter(new FileNameExtensionFilter("Instruction recordings (*.emutrace)", "emutrace"));
        if (save) chooser.setSelectedFile(new java.io.File("recording.emutrace"));
        int result = save ? chooser.showSaveDialog(owner) : chooser.showOpenDialog(owner);
        if (result != JFileChooser.APPROVE_OPTION) return null;
        Path path = chooser.getSelectedFile().toPath();
        return !save || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".emutrace")
                ? path : path.resolveSibling(path.getFileName() + ".emutrace");
    }
}
