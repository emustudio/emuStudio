/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.gui;

import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.plugins.device.ay38910.Ay38910Chip;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Objects;
import java.util.function.IntConsumer;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;

public class Ay38910Gui extends DialogBase {
    private static final ImageIcon VOLUME_ICON = GUI.loadIcon("toolbar-volume.png");
    private static final int REFRESH_MS = 33;

    private final GUI gui;
    private final Ay38910Chip chip;
    private final WaveformPanel waveformPanel;

    public Ay38910Gui(javax.swing.JFrame parent, Ay38910Chip chip, GUI gui) {
        super(parent, "AY-3-8910 Audio", false);
        this.gui = Objects.requireNonNull(gui);
        this.chip = Objects.requireNonNull(chip);
        this.waveformPanel = new WaveformPanel(chip);

        buildContent();
    }

    public void destroy() {
        waveformPanel.stopRefreshing();
        dispose();
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            waveformPanel.startRefreshing(REFRESH_MS);
        } else {
            waveformPanel.stopRefreshing();
        }
        super.setVisible(visible);
    }

    @Override
    protected JComponent initializeComponents() {
        JLabel title = gui.label("<html><b>AY-3-8910</b> waveform monitor</html>");
        JLabel subtitle = gui.label("Live PCM trace of the generated chip output");

        JPopupMenu volumePopup = createVerticalSliderPopup(
                "Volume",
                chip.getVolumePercent(),
                chip::setVolumePercent
        );

        JButton btnVolume = createToolbarButton(VOLUME_ICON, "AY output volume", e ->
                togglePopup((AbstractButton) e.getSource(), volumePopup)
        );

        JToolBar toolbar = gui.toolBar();
        toolbar.add(btnVolume);

        JPanel header = gui.panel("insets 12 14 8 14", "[grow]", "[][]");
        header.add(title, "wrap");
        header.add(subtitle);

        JPanel bottomBar = gui.panel("insets 0", "[pref!]push", "[]");
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar);

        JPanel content = gui.panel("insets 0", "[grow]", "[][grow]0[40!]");
        content.add(header, "growx, wrap");
        content.add(waveformPanel, "grow, wrap");
        content.add(bottomBar, "growx");
        return content;
    }

    private JPopupMenu createVerticalSliderPopup(String title, int initialValue, IntConsumer onChange) {
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialValue);
        slider.setFocusable(false);

        JLabel titleLabel = gui.label(title);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel valueLabel = gui.label(initialValue + "%");
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ChangeListener changeListener = e -> {
            int value = slider.getValue();
            valueLabel.setText(value + "%");
            onChange.accept(value);
        };
        slider.addChangeListener(changeListener);

        JPanel panel = gui.panel("insets 8", "[grow]", "[]6[grow]6[]");
        panel.add(titleLabel, "growx, wrap");
        panel.add(slider, "align center, wrap");
        panel.add(valueLabel, "growx");

        JPopupMenu popup = new JPopupMenu();
        popup.add(panel);
        return popup;
    }

    private JButton createToolbarButton(Icon icon, String tooltip, java.util.function.Consumer<ActionEvent> action) {
        AbstractAction toolbarAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        };
        toolbarAction.putValue(SMALL_ICON, icon);
        toolbarAction.putValue(SHORT_DESCRIPTION, tooltip);
        return gui.toolbarButton(toolbarAction);
    }

    private void togglePopup(AbstractButton button, JPopupMenu popup) {
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        Dimension preferredSize = popup.getPreferredSize();
        int x = Math.max(0, (button.getWidth() - preferredSize.width) / 2);
        int y = -preferredSize.height;
        popup.show(button, x, y);
    }
}
