/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.abstracttape.gui;

import javax.swing.*;
import java.awt.*;

class NiceButton extends JButton {

    private final static int x_WIDTH = 95;
    private final static int x_HEIGHT = 30;

    private NiceButton() {
        super();
        Dimension d = getPreferredSize();
        d.setSize(x_WIDTH, x_HEIGHT); //d.getHeight());
        this.setPreferredSize(d);
        this.setSize(x_WIDTH, x_HEIGHT);//this.getHeight());
        this.setMinimumSize(d);
        this.setMaximumSize(d);
    }

    NiceButton(String text) {
        this();
        this.setText(text);
    }
}
