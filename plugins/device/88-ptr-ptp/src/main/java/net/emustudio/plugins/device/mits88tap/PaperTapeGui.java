/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;

final class PaperTapeGui extends JFrame {
    private final PaperTapeUnit tape;
    private final JLabel readerFile = new JLabel("No reader tape");
    private final JLabel punchFile = new JLabel("No punch tape");
    private final JProgressBar progress = new JProgressBar();
    private final Timer refreshTimer = new Timer(100, event -> refresh());

    PaperTapeGui(JFrame parent, PaperTapeUnit tape) {
        super("Altair paper tape");
        this.tape = tape;
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        JPanel files = new JPanel(new GridLayout(2, 1));
        files.add(readerFile);
        files.add(punchFile);
        add(files, BorderLayout.NORTH);
        add(progress, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout());
        addButton(actions, "Load tape", this::loadReader);
        addButton(actions, "Rewind", tape::rewindReader);
        addButton(actions, "Eject", tape::detachReader);
        addButton(actions, "Save tape", this::openPunch);
        addButton(actions, "Close output", tape::detachPunch);
        add(actions, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(parent);
        refreshTimer.start();
        refresh();
    }

    private void addButton(JPanel panel, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private void loadReader() {
        chooseFile(false).ifPresent(path -> {
            try {
                tape.attachReader(path);
            } catch (IOException e) {
                showError(e);
            }
        });
    }

    private void openPunch() {
        chooseFile(true).ifPresent(path -> {
            try {
                tape.attachPunch(path);
            } catch (IOException e) {
                showError(e);
            }
        });
    }

    private java.util.Optional<Path> chooseFile(boolean save) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Paper tape (*.pt, *.bin, *.hex)", "pt", "bin", "hex"));
        int result = save ? chooser.showSaveDialog(this) : chooser.showOpenDialog(this);
        return result == JFileChooser.APPROVE_OPTION
                ? java.util.Optional.of(chooser.getSelectedFile().toPath())
                : java.util.Optional.empty();
    }

    private void showError(IOException error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "Paper tape error", JOptionPane.ERROR_MESSAGE);
    }

    private void refresh() {
        readerFile.setText(tape.getReaderPath().map(path -> "Reader: " + path).orElse("No reader tape"));
        punchFile.setText(tape.getPunchPath().map(path -> "Punch: " + path).orElse("No punch tape"));
        progress.setMaximum(Math.max(1, tape.getReaderLength()));
        progress.setValue(tape.getReaderPosition());
        progress.setStringPainted(true);
        progress.setString(tape.getReaderPosition() + " / " + tape.getReaderLength());
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
