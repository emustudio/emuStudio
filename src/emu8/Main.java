/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package emu8;

import emu8.gui.frmConfiguration;

/**
 *
 * @author vbmacher
 */
public class Main {
    public ArchitectureLoader emuConfig;
    public static Main thisInstance = null;
    
    public static Main getInstance() {
        return thisInstance;
    }
    
    /** Creates a new instance of Main */
    public Main() {
        emuConfig = new ArchitectureLoader();
        Main.thisInstance = this;
    }
    
    public static void showErrorMessage(String message) {
        javax.swing.JOptionPane.showMessageDialog(null,
                message,"Error",javax.swing.JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); }
        catch (javax.swing.UnsupportedLookAndFeelException e) {}
        catch (ClassNotFoundException e) {}
        catch (InstantiationException e) {}
        catch (IllegalAccessException e) {}

        new Main();
        new frmConfiguration().setVisible(true);
    }
    
}
