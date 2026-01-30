/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * Simplified panel builder for emuStudio with fluent API.
 * Hides MigLayout complexity behind simple methods.
 */
public class EPanel extends JPanel {

    public EPanel() {
        super(new MigLayout("insets dialog", "[grow]", "[]"));
    }

    public EPanel(String layoutConstraints) {
        super(new MigLayout(layoutConstraints));
    }

    public EPanel(String layoutConstraints, String colConstraints, String rowConstraints) {
        super(new MigLayout(layoutConstraints, colConstraints, rowConstraints));
    }

    public EPanel add(Component comp, String constraints) {
        super.add(comp, constraints);
        return this;
    }

    public EPanel addRow(Component... components) {
        for (Component comp : components) {
            super.add(comp);
        }
        super.add(new JLabel(), "wrap"); // Force new row
        return this;
    }

    public EPanel addGrowRow(Component comp) {
        super.add(comp, "growx, wrap");
        return this;
    }

    public EPanel addSpan(Component comp) {
        super.add(comp, "span, wrap");
        return this;
    }

    public EPanel addSeparator() {
        super.add(new JSeparator(), "growx, span, wrap");
        return this;
    }

    public EPanel addSeparator(String title) {
        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(Font.BOLD));

        EPanel panel = new EPanel("insets 0", "[grow]", "[]");
        panel.add(label, "split 2, span");
        panel.add(new JSeparator(), "growx, gapleft rel");

        super.add(panel, "growx, span, wrap");
        return this;
    }

    public EPanel addGap() {
        super.add(new JLabel(), "wrap, height 10!");
        return this;
    }

    // Static factory methods

    public static EPanel vertical() {
        return new EPanel("insets dialog, fillx", "[grow]", "[]");
    }

    public static EPanel horizontal() {
        return new EPanel("insets dialog, filly", "[]", "[grow]");
    }

    public static EPanel grid(int cols) {
        StringBuilder colSpec = new StringBuilder();
        for (int i = 0; i < cols; i++) {
            colSpec.append("[grow]");
        }
        return new EPanel("insets dialog", colSpec.toString(), "[]");
    }

    public static EPanel buttonBar() {
        return new EPanel("insets dialog", "[grow, right]", "[]");
    }
}
