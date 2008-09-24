/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 *
 * KISS, YAGNI
 */

package architecture;

import gui.LoadingDialog;
import gui.OpenArchDialog;
import gui.StudioFrame;

/**
 *
 * @author vbmacher
 */
public class Main {
    public static ArchLoader aloader;
    public static ArchHandler currentArch = null;    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); }
        catch (javax.swing.UnsupportedLookAndFeelException e) {}
        catch (ClassNotFoundException e) {}
        catch (InstantiationException e) {}
        catch (IllegalAccessException e) {}

        aloader = new ArchLoader();
        String configName = null;
        OpenArchDialog odi = new OpenArchDialog();
        odi.setVisible(true);
        if (odi.getOK()) configName = odi.getArchName();
        if (configName == null) return;

        LoadingDialog splash = new LoadingDialog();
        splash.setVisible(true);
        currentArch = aloader.load(configName);
        splash.dispose();
        splash = null;
        
        if (currentArch != null)
            new StudioFrame().setVisible(true);
        else System.exit(0);
    }
    
}
