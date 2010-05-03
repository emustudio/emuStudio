/*
 * TerminalWindow.java
 *
 * Implementation of interactive display terminal ADM-3A
 *
 * Created on Piatok, 2007, november 16, 11:42
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package terminal.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.io.InputStream;


import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;


import terminal.TerminalDisplay;
import terminal.TerminalFemale;
import terminal.gui.utils.TerminalKeyboard;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class TerminalWindow extends JFrame {
//    private static final int termWIDTH = 750;
//    private static final int termHEIGHT = 584;
    
    private TerminalDisplay terminal;
    private Font terminalFont;
    private TerminalKeyboard keyboard;
    
    /** Creates new form TerminalWindow */
    public TerminalWindow(TerminalDisplay lblTerminal, TerminalFemale female) {
        this.terminal = lblTerminal;
        keyboard = new TerminalKeyboard(terminal,female);
        setVisible(false);
        initComponents();

        keyboard.addListenerRecursively(this);
        this.setLocationRelativeTo(null);
    }
    
    public void destroyMe() {
        terminal.destroyMe();
        this.dispose();
    }
    
    public void setHalfDuplex(boolean hd) { keyboard.setHalfDuplex(hd); }
    
    private void initComponents() {
        JLabel lblBack = new JLabel();
        ImageIcon img = new ImageIcon(getClass().getResource("/resources/display.gif"));

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Terminal ADM-3A");
        setResizable(false);
        
        try {
            // load terminal font from resources
            InputStream fin = this.getClass().getResourceAsStream("/resources/terminal.ttf");
            this.terminalFont = Font.createFont(Font.TRUETYPE_FONT,fin).deriveFont(12f);
            fin.close();
            terminal.setFont(this.terminalFont);
        } catch (Exception e) {
            terminal.setFont(new java.awt.Font("Monospaced", 0, 12));
        }
        terminal.setForeground(new java.awt.Color(0, 255, 0));
        terminal.setBackground(new Color(0,0,0));
        terminal.setBounds(53, 60, 653, 400);

        lblBack.setLocation(0, 0);
        lblBack.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblBack.setIcon(img); // NOI18N
        lblBack.setFocusable(false);
        
        lblBack.setBounds(0, 0, img.getIconWidth(), img.getIconHeight());

        Container pane = getContentPane();
        pane.setBackground(Color.BLACK);
        pane.setLayout(null);

        pane.add(terminal);
        pane.add(lblBack);
        pack();
        setSize(img.getIconWidth(),img.getIconHeight());

    }

}
