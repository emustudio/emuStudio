/**
 * BrainTerminalDialog.java
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package brainterminal.gui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public class BrainTerminalDialog extends JDialog {

    private final static String VERBOSE_FILE_NAME = "BrainTerminal.out";
    private String inputBuffer;
    // verbose mode = output to a file
    private boolean verbose;
    private FileWriter outw = null;

    public BrainTerminalDialog() {
        super();
        inputBuffer = "";
        initComponents();
    }

    /**
     * Set verbose mode. If verbose mode is set, the output
     * is redirected also to a file.
     * 
     * @param verbose set/unset verbose mode
     */
    public void setVerbose(boolean verbose) {
        if (verbose) {
            File f = new File(VERBOSE_FILE_NAME);
            try {
                outw = new FileWriter(f);
            } catch (IOException e) {
            }
        } else if (outw != null) {
            try {
                outw.close();
            } catch (IOException e) {
            }
            outw = null;
        }
        this.verbose = verbose;
    }

    /**
     * Metóda "zmaže" obrazovku
     */
    public void clearScreen() {
        txtTerminal.setText("");
    }

    private void verbose_char(char val) {
        if (verbose && (outw != null)) {
            try {
                outw.write(val);
                outw.flush();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Metóda vypíše znak na obrazovku
     *
     * @param c znak, ktorý sa má vypísať
     */
    public void putChar(char c) {
        txtTerminal.append(String.valueOf(c));
        verbose_char(c);
    }

    /**
     * Metóda vráti jeden znak zo vstupného
     * buffra. Ak je prázdny, naplní ho.
     *
     * @return znak z buffra
     */
    public char getChar() {
        if (inputBuffer.equals("")) {
            inputBuffer += JOptionPane.showInputDialog("Zadaj vstupný znak (alebo reťazec):");
        }
        try {
            char c = inputBuffer.charAt(0);
            inputBuffer = inputBuffer.substring(1);
            putChar(c);
            return c;
        } catch (Exception e) {
            // ak náhodou používateľ zadal prázdny
            // reťazec
            return 0;
        }
    }

    private void initComponents() {
        JScrollPane scrollTerminal = new JScrollPane();
        txtTerminal = new JTextArea();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("BrainDuck Terminal");
        setAlwaysOnTop(true);

        txtTerminal.setColumns(20);
        txtTerminal.setEditable(false);
        txtTerminal.setRows(5);
        scrollTerminal.setViewportView(txtTerminal);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scrollTerminal, GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(scrollTerminal, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE));
        pack();
    }
    private JTextArea txtTerminal;
}
