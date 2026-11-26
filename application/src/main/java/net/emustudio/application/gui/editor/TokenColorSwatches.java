/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;

/** Swatches whose selection follows the chooser model even when another component has focus. */
final class TokenColorSwatches extends AbstractColorChooserPanel {
    private static final int ROWS = 9;
    private static final int HUES = 30;
    private static final int CELL_SIZE = 16;
    private static final int RECENT_COLUMNS = 5;
    private static final int RECENT_ROWS = 7;
    private static final int CHANNEL_STEP = 51;
    private static final int[] GRAYS = {255, 204, 204, 153, 153, 102, 102, 51, 0};
    private static final Border SELECTED_BORDER = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 2), BorderFactory.createLineBorder(Color.WHITE, 2));
    private static final Border SWATCH_BORDER = BorderFactory.createLineBorder(Color.DARK_GRAY);

    private final DefaultListModel<Color> recentColors = new DefaultListModel<>();
    private JList<Color> palette;
    private JList<Color> recent;
    private boolean updatingSelection;

    @Override
    protected void buildChooser() {
        removeAll();
        palette = new JList<>(paletteColors());
        palette.setVisibleRowCount(ROWS);
        configure(palette, "Swatches");
        recent = new JList<>(recentColors);
        recent.setVisibleRowCount(0);
        recent.setPreferredSize(new Dimension(RECENT_COLUMNS * CELL_SIZE, RECENT_ROWS * CELL_SIZE));
        configure(recent, "Recent colors");

        JPanel recentPanel = new JPanel(new BorderLayout());
        JLabel recentLabel = new JLabel("Recent:");
        recentLabel.setLabelFor(recent);
        recentPanel.add(recentLabel, BorderLayout.PAGE_START);
        recentPanel.add(recent, BorderLayout.CENTER);
        setLayout(new BorderLayout(12, 0));
        add(palette, BorderLayout.CENTER);
        add(recentPanel, BorderLayout.LINE_END);
    }

    @Override
    public void updateChooser() {
        Color color = getColorFromModel();
        updatingSelection = true;
        try {
            palette.setSelectedValue(color, false);
            if (color != null) remember(color);
            if (color != null && !color.equals(palette.getSelectedValue())) {
                palette.clearSelection();
                recent.setSelectedValue(color, false);
            } else {
                recent.clearSelection();
            }
        } finally {
            updatingSelection = false;
        }
    }

    @Override public String getDisplayName() { return "Swatches"; }
    @Override public Icon getSmallDisplayIcon() { return null; }
    @Override public Icon getLargeDisplayIcon() { return null; }

    private void configure(JList<Color> swatches, String name) {
        swatches.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        swatches.setFixedCellWidth(CELL_SIZE);
        swatches.setFixedCellHeight(CELL_SIZE);
        swatches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        swatches.getAccessibleContext().setAccessibleName(name);
        swatches.setToolTipText("");
        JLabel cell = new JLabel();
        cell.setOpaque(true);
        swatches.setCellRenderer((list, color, index, selected, focused) -> {
            cell.setBackground(new Color(color.getRGB()));
            cell.setBorder(selected ? SELECTED_BORDER : SWATCH_BORDER);
            String value = color.getAlpha() == 255 ? String.format("#%06X", color.getRGB() & 0xFFFFFF)
                    : String.format("#%08X (ARGB)", color.getRGB());
            cell.setToolTipText(value);
            cell.getAccessibleContext().setAccessibleName(value);
            return cell;
        });
        swatches.addListSelectionListener(event -> {
            Color selected = swatches.getSelectedValue();
            if (!updatingSelection && !event.getValueIsAdjusting() && selected != null) {
                if (selected.equals(getColorFromModel())) updateChooser();
                else getColorSelectionModel().setSelectedColor(selected);
            }
        });
    }

    private void remember(Color color) {
        if (recentColors.contains(color)) return;
        recentColors.add(0, color);
        if (recentColors.size() > RECENT_COLUMNS * RECENT_ROWS) recentColors.remove(recentColors.size() - 1);
    }

    private static Color[] paletteColors() {
        Color[] colors = new Color[ROWS * (HUES + 1)];
        for (int row = 0; row < ROWS; row++) {
            int start = row * (HUES + 1);
            int gray = GRAYS[row];
            colors[start] = new Color(gray, gray, gray);
            // Clamp RGB channels to retain the color steps of Swing's default palette.
            int min = Math.max(0, (4 - row) * CHANNEL_STEP);
            int max = Math.min(255, (ROWS - row) * CHANNEL_STEP);
            for (int hue = 0; hue < HUES; hue++) {
                Color base = Color.getHSBColor(0.5f + (float) hue / HUES, 1, 1);
                colors[start + hue + 1] = new Color(clamp(base.getRed(), min, max),
                        clamp(base.getGreen(), min, max), clamp(base.getBlue(), min, max));
            }
        }
        // The darkest row ends with two shifted greens and a gray swatch in the default palette.
        int lastRow = (ROWS - 1) * (HUES + 1);
        colors[lastRow + 25] = new Color(0, CHANNEL_STEP, 0);
        colors[lastRow + 26] = new Color(0, CHANNEL_STEP, CHANNEL_STEP);
        colors[lastRow + HUES] = new Color(CHANNEL_STEP, CHANNEL_STEP, CHANNEL_STEP);
        return colors;
    }

    private static int clamp(int channel, int min, int max) {
        return Math.max(min, Math.min(channel, max));
    }
}
