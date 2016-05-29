/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package emustudio.drawing;

import emustudio.gui.ElementPropertiesDialog;

import javax.swing.*;

/**
 * This class represents pop-up menu that shows up when a user clicks with
 * right button on drawing canvas in abstract schema editor.
 *
 */
public class ElementPopUpMenu extends JPopupMenu {
    private Element elem;
    private JDialog parent;

    public ElementPopUpMenu(Element el, JDialog par){
        this.parent = par;
        this.elem = el;
        JMenuItem anItem = new JMenuItem("Settings...");
        add(anItem);

        anItem.addActionListener(ae -> {
            new ElementPropertiesDialog(parent,elem).setVisible(true);
            parent.repaint();
        });
    }
}
