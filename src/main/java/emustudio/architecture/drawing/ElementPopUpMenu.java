/*
 * ElementPopUpMenu.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2011-2012, Peter Jakubčo
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package emustudio.architecture.drawing;

import emustudio.gui.ElementPropertiesDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * This class represents pop-up menu that shows up when a user clicks with
 * right button on drawing canvas in abstract schema editor.
 *
 * @author vbmacher
 */
public class ElementPopUpMenu extends JPopupMenu {
    private final JMenuItem anItem;
    private Element elem;
    private JDialog parent;

    public ElementPopUpMenu(Element el, JDialog par){
        this.parent = par;
        this.elem = el;
        anItem = new JMenuItem("Settings...");
        add(anItem);

        anItem.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                new ElementPropertiesDialog(parent,elem).setVisible(true);
                parent.repaint();
            }

        });
    }
}
