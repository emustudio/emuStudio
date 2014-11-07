/*
 * Copyright (C) 2014 Peter Jakubčo
 *
 * KISS, DRY, YAGNI
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
package net.sf.emustudio.brainduck.terminal.io;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BrainTerminalDialog extends javax.swing.JDialog implements IOProvider, KeyListener {
    private static final int MAX_WIDTH = 80;
    private static final int MAX_HEIGHT = 25;

    private final ImageIcon blueIcon;
    private final ImageIcon redIcon;
    private final ImageIcon greenIcon;
    
    private final TextCanvas canvas;
    private final BlockingQueue<Integer> inputBuffer = new LinkedBlockingQueue<>();

    private BrainTerminalDialog() {
        URL blueIconURL = getClass().getResource(
                "/net/sf/emustudio/brainduck/terminal/16_circle_blue.png"
        );
        URL redIconURL = getClass().getResource(
                "/net/sf/emustudio/brainduck/terminal/16_circle_red.png"
        );
        URL greenIconURL = getClass().getResource(
                "/net/sf/emustudio/brainduck/terminal/16_circle_green.png"
        );

        blueIcon = new ImageIcon(Objects.requireNonNull(blueIconURL));
        redIcon = new ImageIcon(Objects.requireNonNull(redIconURL));
        greenIcon = new ImageIcon(Objects.requireNonNull(greenIconURL));

        setTitle("BrainDuck Terminal");
        initComponents();
        setLocationRelativeTo(null);
        
        canvas = new TextCanvas(MAX_WIDTH, MAX_HEIGHT);
        scrollPane.setViewportView(canvas);
        canvas.start();
    }

    public static BrainTerminalDialog create() {
        BrainTerminalDialog dialog = new BrainTerminalDialog();
        GUIUtils.addListenerRecursively(dialog, dialog);
        return dialog;
    }

    private void setReadIcon() {
        lblStatusIcon.setIcon(greenIcon);
        lblStatusIcon.repaint();
    }
    
    private void setIdleIcon() {
        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.repaint();
    }

    private void setWriteIcon() {
        lblStatusIcon.setIcon(redIcon);
        lblStatusIcon.repaint();
    }

    @Override
    public void write(int c) {
        setWriteIcon();
        try {
            Cursor cursor = canvas.getTextCanvasCursor();
            if (c < 32) {
                switch (c) {
                    case 7:  /* bell */
                        break;
                    case 8:  /* backspace*/
                        cursor.back();
                        break;
                    case 9:
                        cursor.advance(4);
                        break;
                    case 0x0A: /* line feed */
                        cursor.newLine();
                        cursor.carriageReturn(); // simulate DOS
                        break;
                    case 0x0D: /* carriage return */
                        cursor.carriageReturn();
                        break;
                }
                repaint();
                return;
            }
            canvas.writeAtCursor(c);
        } finally {
            setIdleIcon();
        }
    }

    @Override
    public int read() {
        setReadIcon();
        try {
            return inputBuffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            setIdleIcon();
        }
        return IOProvider.EOF;
    }

    @Override
    public void reset() {
        canvas.clear();
    }

    @Override
    public void showGUI() {
        this.setVisible(true);
    }
    
    @Override
    public void close() {
        canvas.stop();
        GUIUtils.removeListenerRecursively(this, this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keycode = e.getKeyCode();
        if (keycode == KeyEvent.VK_ESCAPE) {
            inputBuffer.add(IOProvider.EOF);
        } else if (keycode == KeyEvent.VK_SHIFT || keycode == KeyEvent.VK_CONTROL ||
                keycode == KeyEvent.VK_ALT || keycode == KeyEvent.VK_META) {
            return;
        } else {
            inputBuffer.add((int) e.getKeyChar());
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
    
    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        panelStatus = new javax.swing.JPanel();
        lblStatusIcon = new javax.swing.JLabel();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Waiting for input? (red - yes, blue - no)");

        javax.swing.GroupLayout panelStatusLayout = new javax.swing.GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelStatusLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblStatusIcon)
                .addContainerGap(676, Short.MAX_VALUE))
        );
        panelStatusLayout.setVerticalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelStatusLayout.createSequentialGroup()
                .addGap(0, 9, Short.MAX_VALUE)
                .addComponent(lblStatusIcon))
        );

        scrollPane.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(panelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(scrollPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 403, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel lblStatusIcon;
    private javax.swing.JPanel panelStatus;
    private javax.swing.JScrollPane scrollPane;
    // End of variables declaration//GEN-END:variables

}
