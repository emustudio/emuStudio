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
package net.emustudio.application.gui.debugtable;

import javax.swing.*;

public class BooleanComponent extends JLabel {
    public static final Icon BOOLEAN_ICON = new ImageIcon(BooleanCellRenderer.class.getResource("/net/emustudio/application/gui/dialogs/breakpoint.png"));

    private boolean value;

    public BooleanComponent(boolean value) {
        this.value = value;
        setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
    }

    public boolean getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = value;
        if (value) {
            setIcon(BOOLEAN_ICON);
        } else {
            setIcon(null);
        }
    }
}
