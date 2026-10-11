/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;

final class MdsGui extends DialogBase {
    private static final String TITLE = "MITS 88-MDS minidisk";

    private final MdsController controller;
    private final Dialogs dialogs;
    private final GUI gui;
    private final JComboBox<Integer> drive = new JComboBox<>();
    private final JLabel image = new JLabel();
    private final JLabel position = new JLabel();
    private final JLabel status = new JLabel();
    private final Timer refreshTimer = new Timer(100, event -> refresh());

    MdsGui(JFrame parent, MdsController controller, Dialogs dialogs, GUI gui) {
        super(parent, TITLE, false);
        this.controller = controller;
        this.dialogs = dialogs;
        this.gui = gui;
        for (int i = 0; i < MdsController.DRIVE_COUNT; i++) {
            drive.addItem(i);
        }
        drive.addActionListener(event -> refresh());
        refresh();
        buildContent();
        setSize(560, getHeight());
        setLocationRelativeTo(parent);
    }

    @Override
    protected JComponent initializeComponents() {
        JPanel content = gui.panel("insets dialog, fillx", "[grow]", "[][][][][]");
        content.add(drive, "growx, wrap");
        content.add(image, "growx, wrap");
        content.add(position, "growx, wrap");
        content.add(status, "growx, wrap");
        content.add(gui.button("Attach image", this::attach), "split 2, align center");
        content.add(gui.button("Detach", this::detach));
        return content;
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            refresh();
            refreshTimer.start();
        } else {
            refreshTimer.stop();
        }
        super.setVisible(visible);
    }

    @Override
    public void dispose() {
        refreshTimer.stop();
        super.dispose();
    }

    private int selectedDrive() {
        return (Integer) drive.getSelectedItem();
    }

    private void attach() {
        dialogs.chooseFile("Attach minidisk image", "Attach", false).ifPresent(path -> {
            try {
                controller.attach(selectedDrive(), path);
                refresh();
            } catch (IOException e) {
                dialogs.showError(e.getMessage(), TITLE);
            }
        });
    }

    private void detach() {
        try {
            controller.detach(selectedDrive());
            refresh();
        } catch (IOException e) {
            dialogs.showError(e.getMessage(), TITLE);
        }
    }

    private void refresh() {
        int selected = selectedDrive();
        image.setText(controller.imagePath(selected).map(Path::toString).orElse("No image attached"));
        position.setText("Track " + controller.track(selected) + ", sector " + controller.sector(selected));
        status.setText(controller.isSelected(selected) ? "Selected" : "Idle");
    }
}
