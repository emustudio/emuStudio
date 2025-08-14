/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.function.Consumer;

/**
 * A button with the constant size.
 */
public class ConstantSizeButton extends JButton {
    private final static int NB_WIDTH = 95;
    private static int NB_HEIGHT = 30;

    public ConstantSizeButton(Action action) {
        super(action);

        setHeight();
        Dimension d = getPreferredSize();
        d.setSize(NB_WIDTH, NB_HEIGHT);
        this.setPreferredSize(d);
        this.setSize(NB_WIDTH, NB_HEIGHT);
        this.setMinimumSize(d);
        this.setMaximumSize(d);
    }

    public ConstantSizeButton(Consumer<ActionEvent> actionConsumer) {
        this(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                actionConsumer.accept(actionEvent);
            }
        });
    }

    private void setHeight() {
        FontMetrics metrics = this.getFontMetrics(getFont());
        NB_HEIGHT = metrics.getHeight() + 9;
    }
}
