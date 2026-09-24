/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Path;

final class MdsGui extends JFrame {
    private final MdsController controller;
    private final JComboBox<Integer> drive = new JComboBox<>();
    private final JLabel image = new JLabel();
    private final JLabel position = new JLabel();
    private final JLabel status = new JLabel();
    private final Timer refreshTimer = new Timer(100, event -> refresh());

    MdsGui(JFrame parent, MdsController controller) {
        super("MITS 88-MDS minidisk");
        this.controller = controller;
        for (int i = 0; i < MdsController.DRIVE_COUNT; i++) {
            drive.addItem(i);
        }
        drive.addActionListener(event -> refresh());

        JPanel details = new JPanel(new GridLayout(4, 1));
        details.add(drive);
        details.add(image);
        details.add(position);
        details.add(status);
        add(details, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout());
        JButton attach = new JButton("Attach image");
        attach.addActionListener(event -> attach());
        actions.add(attach);
        JButton detach = new JButton("Detach");
        detach.addActionListener(event -> detach());
        actions.add(detach);
        add(actions, BorderLayout.SOUTH);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        pack();
        setSize(560, getHeight());
        setLocationRelativeTo(parent);
        refreshTimer.start();
        refresh();
    }

    private int selectedDrive() {
        return (Integer) drive.getSelectedItem();
    }

    private void attach() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
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
        position.setText("Track " + controller.track(selected) + ", sector " + controller.sector(selected));
        status.setText(controller.isSelected(selected) ? "Selected" : "Idle");
    }

    private void showError(IOException error) {
        JOptionPane.showMessageDialog(this, error.getMessage(), "88-MDS error", JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }
}
