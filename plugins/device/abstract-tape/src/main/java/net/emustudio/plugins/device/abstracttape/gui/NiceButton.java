/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
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


    NiceButton(String text, Icon icon) {
        this();
        this.setIcon(icon);
        this.setText(text);
    }
}
