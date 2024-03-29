/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
