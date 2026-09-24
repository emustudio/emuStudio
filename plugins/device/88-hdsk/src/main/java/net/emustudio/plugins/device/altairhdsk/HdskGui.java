/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;

final class HdskGui extends JFrame {
    private final HdskController controller;
    private final JComboBox<Integer> drive = new JComboBox<>();
    private final JSpinner sectorSize = new JSpinner(new SpinnerNumberModel(128, 128, 1024, 128));
    private final JSpinner sectorsPerTrack = new JSpinner(new SpinnerNumberModel(32, 1, 255, 1));
    private final JLabel image = new JLabel();
    private final JLabel geometry = new JLabel();
    private final JLabel activity = new JLabel();
    private final Timer refreshTimer = new Timer(100, event -> refresh());

    HdskGui(JFrame parent, HdskController controller) {
        super("SIMH Altair HDSK");
        this.controller = controller;
        for (int i = 0; i < HdskController.DRIVE_COUNT; i++) {
            drive.addItem(i);
        }
        drive.addActionListener(event -> loadDrive());

        JPanel details = new JPanel(new GridLayout(6, 1));
        details.add(drive);
        details.add(sectorSize);
        details.add(sectorsPerTrack);
        details.add(image);
        details.add(geometry);
        details.add(activity);
        add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout());
        addButton(actions, "Apply geometry", this::configure);
        addButton(actions, "Attach/create image", this::attach);
        addButton(actions, "Detach", this::detach);
        add(actions, BorderLayout.SOUTH);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        pack();
        setSize(600, getHeight());
        setLocationRelativeTo(parent);
        refreshTimer.start();
        loadDrive();
    }

    private void addButton(JPanel panel, String title, Runnable action) {
        JButton button = new JButton(title);
        button.addActionListener(event -> action.run());
        panel.add(button);
    }

    private int selectedDrive() {
        return (Integer) drive.getSelectedItem();
    }

    private void configure() {
        try {
            controller.configure(selectedDrive(), (Integer) sectorSize.getValue(), (Integer) sectorsPerTrack.getValue());
            refresh();
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "HDSK geometry error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void attach() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            configure();
            controller.attach(selectedDrive(), chooser.getSelectedFile().toPath());
            refresh();
        } catch (IOException e) {
            showError(e);
        }
    }

    private void detach() {
        try {
            controller.detach(selectedDrive());
            refresh();
        } catch (IOException e) {
            showError(e);
        }
    }

    private void refresh() {
        int selected = selectedDrive();
        image.setText(controller.imagePath(selected).map(Path::toString).orElse("No image attached"));
        geometry.setText(controller.tracks(selected) + " tracks × " + controller.sectorsPerTrack(selected)
                + " sectors × " + controller.sectorSize(selected) + " bytes");
        activity.setText(controller.activity(selected));
    }

    private void loadDrive() {
        int selected = selectedDrive();
        sectorSize.setValue(controller.sectorSize(selected));
        sectorsPerTrack.setValue(controller.sectorsPerTrack(selected));
        refresh();
    }

    private void showError(IOException error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "HDSK error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
