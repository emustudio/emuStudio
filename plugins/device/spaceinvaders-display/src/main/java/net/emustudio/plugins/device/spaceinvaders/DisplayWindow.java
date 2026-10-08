/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.GUI;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

final class DisplayWindow extends JFrame {
    private static final ImageIcon VOLUME_ICON = GUI.loadIcon("toolbar-volume.png");
    private final DisplayPanel display;

    DisplayWindow(JFrame parent, MemoryContext<Byte> memory, SpaceInvadersHardware hardware,
                  int scale, boolean colorOverlay, SampleSoundOutput sound, GUI gui) {
        super("Space Invaders");
        display = new DisplayPanel(memory, hardware, scale, colorOverlay);
        add(display, BorderLayout.CENTER);
        add(createSoundBar(sound, gui), BorderLayout.SOUTH);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel createSoundBar(SampleSoundOutput sound, GUI gui) {
        int initialVolume = sound.getVolumePercent();
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialVolume);
        slider.setFocusable(false);
        JLabel title = gui.label("Volume");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel value = gui.label(initialVolume + "%");
        value.setHorizontalAlignment(SwingConstants.CENTER);
        slider.addChangeListener(e -> {
            int volume = slider.getValue();
            value.setText(volume + "%");
            sound.setVolumePercent(volume);
        });
        JPanel controls = gui.panel("insets 8", "[grow]", "[]6[grow]6[]");
        controls.add(title, "growx, wrap");
        controls.add(slider, "align center, wrap");
        controls.add(value, "growx");
        JPopupMenu popup = new JPopupMenu();
        popup.add(controls);

        Action volumeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    popup.setVisible(false);
                } else {
                    AbstractButton button = (AbstractButton) e.getSource();
                    Dimension size = popup.getPreferredSize();
                    popup.show(button, Math.max(0, (button.getWidth() - size.width) / 2), -size.height);
                }
            }
        };
        volumeAction.putValue(Action.SMALL_ICON, VOLUME_ICON);
        volumeAction.putValue(Action.SHORT_DESCRIPTION, "Audio volume");
        JToolBar toolbar = gui.toolBar();
        toolbar.add(gui.toolbarButton(volumeAction));

        JPanel bottomBar = gui.panel("insets 0", "[pref!]push", "[40!]");
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar);
        return bottomBar;
    }

    void frameReady() {
        display.repaint();
    }
}
