/*
 * TerminalWindow.java
 *
 * Implementation of interactive display terminal ADM-3A
 *
 * Created on Piatok, 2007, november 16, 11:42
 */

package gui;

import sio88.Mits88SIO;
import terminal.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

/**
 *
 * @author  vbmacher
 */
public class TerminalWindow extends javax.swing.JFrame {
    private boolean halfDuplex = false;
    private TerminalDisplay lblTerminal;
    private Object keyLock; // monitor for key pressing
    private int keyCode = 0;
    private Mits88SIO sio;
    private Font terminalFont;
    
    /** Creates new form TerminalWindow */
    public TerminalWindow(Mits88SIO sio) {
        this.sio = sio;
        keyLock = new Object();
        initTerminalLabel();
        initComponents();
        getContentPane().setBackground(Color.BLACK);

        this.setSize(lblBack.getWidth()+5, lblBack.getHeight()+configButton.getHeight());
        addKeyAndContainerListenerRecursively(this);
        this.setLocationRelativeTo(null);
    }
    
    private void initTerminalLabel() {
        lblTerminal = new TerminalDisplay(80,25);
        try {
            // load terminal font from resources
            InputStream fin = this.getClass().getResourceAsStream("/resources/terminal.ttf");
            this.terminalFont = Font.createFont(Font.TRUETYPE_FONT,fin).deriveFont(12f);
            fin.close();
            lblTerminal.setFont(this.terminalFont);
        } catch (Exception e) {
            lblTerminal.setFont(new java.awt.Font("Monospaced", 0, 12));
        }
        lblTerminal.setForeground(new java.awt.Color(0, 255, 0));
        lblTerminal.setBackground(new Color(0,0,0));
        getContentPane().add(lblTerminal);
        lblTerminal.setBounds(35, 70, 635, 400);
    }
        
    private void addKeyAndContainerListenerRecursively(Component c) {
        TerminalKeyboard terminalK = new TerminalKeyboard();
        c.addKeyListener(terminalK);
        if(c instanceof Container) {
            Container cont = (Container)c;
            cont.addContainerListener(terminalK);
            Component[] children = cont.getComponents();
            for(int i = 0; i < children.length; i++)
                addKeyAndContainerListenerRecursively(children[i]);
        }
     }
    
    public void destroyMe() {
        lblTerminal.destroyMe();
    }

    public void sendChar(char c) {
        lblTerminal.sendChar(c);
    }
    
    public int getChar() {
        if (keyCode != 0) {
            int v = keyCode;
            keyCode = 0;
            return v;
        }
        synchronized(keyLock) {
	    try { keyLock.wait(); }
            catch (InterruptedException ex) {
                int v = keyCode;
                keyCode = 0;
                return v;
            }
        }
        return 0;
    }    
    
    private class TerminalKeyboard extends java.awt.event.KeyAdapter implements ContainerListener {
        @Override
        public void keyPressed(java.awt.event.KeyEvent evt) {
            if (evt.isControlDown()) {
                switch (evt.getKeyCode()) {
                    case (int)'@': keyCode = 0; break;
                    case (int)'A': keyCode = 1; break;
                    case (int)'B': keyCode = 2; break;
                    case (int)'C': keyCode = 3; break;
                    case (int)'D': keyCode = 4; break;
                    case (int)'E': keyCode = 5; break;
                    case (int)'F': keyCode = 6; break;
                    case (int)'G': keyCode = 7; break;
                    case (int)'H': keyCode = 8; break;
                    case (int)'I': keyCode = 9; break;
                    case (int)'J': keyCode = 10; break;
                    case (int)'K': keyCode = 11; break;
                    case (int)'L': keyCode = 12; break;
                    case (int)'M': keyCode = 13; break;
                    case (int)'N': keyCode = 14; break;
                    case (int)'O': keyCode = 15; break;
                    case (int)'P': keyCode = 16; break;
                    case (int)'Q': keyCode = 17; break;
                    case (int)'R': keyCode = 18; break;
                    case (int)'S': keyCode = 19; break;
                    case (int)'T': keyCode = 20; break;
                    case (int)'U': keyCode = 21; break;
                    case (int)'V': keyCode = 22; break;
                    case (int)'W': keyCode = 23; break;
                    case (int)'X': keyCode = 24; break;
                    case (int)'Y': keyCode = 25; break;
                    case (int)'Z': keyCode = 26; break;
                    case (int)'[': keyCode = 27; break;
                    case (int)'*': keyCode = 28; break;
                    case (int)']': keyCode = 29; break;
                    case (int)'^': keyCode = 30; break;
                    default: return;
                }
            } else {
                int kC = evt.getKeyCode();
                if (kC == KeyEvent.VK_DOWN) keyCode = 10;
                else if (kC == KeyEvent.VK_UP) keyCode = 11; 
                else if (kC == KeyEvent.VK_RIGHT) keyCode = 12;
                else if (kC == KeyEvent.VK_LEFT) keyCode = 8;
                else if (kC == KeyEvent.VK_ENTER) keyCode = 13;
                else {
                    keyCode = (int)evt.getKeyChar();
                    if (keyCode > 254) {
                        keyCode = 0;
                        return;
                    }
                }
            }
            if (halfDuplex == true) {
                if (keyCode == 13) lblTerminal.sendChar(10);
                lblTerminal.sendChar(keyCode);
            }
            sio.writeBuffer(keyCode);
            synchronized(keyLock) {
                keyLock.notifyAll();
            }
        }

        private void removeKeyAndContainerListenerRecursively(Component c) {
            c.removeKeyListener(this);
            if(c instanceof Container) {
                Container cont = (Container)c;
                cont.removeContainerListener(this);
                Component[] children = cont.getComponents();
                for(int i = 0; i < children.length; i++)
                    removeKeyAndContainerListenerRecursively(children[i]);
            }
        }
        
        @Override
        public void componentAdded(ContainerEvent e) {
            addKeyAndContainerListenerRecursively(e.getChild());
        }

        @Override
        public void componentRemoved(ContainerEvent e) {
            removeKeyAndContainerListenerRecursively(e.getChild());
        }
    }
    
    public void setHalfDuplex(boolean hd) {
        halfDuplex = hd;
    }
    public boolean isHalfDuplex() { return halfDuplex; }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        btnGroupDuplex = new javax.swing.ButtonGroup();
        configButton = new javax.swing.JButton();
        javax.swing.JButton btnAbout = new javax.swing.JButton();
        lblBack = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("ADM-3A Interactive terminal");
        setResizable(false);
        getContentPane().setLayout(null);

        configButton.setText("Config");
        configButton.setFocusable(false);
        configButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                configButtonActionPerformed(evt);
            }
        });
        getContentPane().add(configButton);
        configButton.setBounds(600, 550, 100, 29);

        btnAbout.setText("About...");
        btnAbout.setFocusable(false);
        btnAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAboutActionPerformed(evt);
            }
        });
        getContentPane().add(btnAbout);
        btnAbout.setBounds(520, 550, 65, 29);

        lblBack.setFont(new java.awt.Font("Monospaced", 0, 12));
        lblBack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/resources/display.gif"))); // NOI18N
        lblBack.setFocusable(false);
        getContentPane().add(lblBack);
        lblBack.setBounds(0, 0, 713, 584);
        lblBack.getAccessibleContext().setAccessibleName("Display");

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void configButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_configButtonActionPerformed
        new ConfigDialog(this, true, lblTerminal).setVisible(true);
    }//GEN-LAST:event_configButtonActionPerformed

private void btnAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAboutActionPerformed
    new AboutDialog(this,true).setVisible(true);
}//GEN-LAST:event_btnAboutActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    javax.swing.ButtonGroup btnGroupDuplex;
    javax.swing.JButton configButton;
    javax.swing.JLabel lblBack;
    // End of variables declaration//GEN-END:variables
    
}
