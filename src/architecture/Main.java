/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 *
 * KISS, YAGNI
 */

package architecture;

import gui.AddEditArchDialog;
import gui.OpenArchDialog;
import gui.StartDialog;
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

        StartDialog sFrame = new StartDialog();
        sFrame.setVisible(true);
        
        String configName = null;
        switch (sFrame.getRes()) {
            case newArch:
                AddEditArchDialog di = new AddEditArchDialog(null, true);
                di.setVisible(true);
                if (di.getOK()) {
                    configName = di.getSchema().getConfigName();
                    ArchLoader.saveSchema(di.getSchema());
                }
                break;
            case openArch:
                OpenArchDialog odi = new OpenArchDialog();
                odi.setVisible(true);
                if (odi.getOK()) configName = odi.getArchName();
                break;
            case exit:
                return;
        }
        if (configName == null) return;
        currentArch = aloader.load(configName);
        if (currentArch != null)
            new StudioFrame().setVisible(true);
    }
    
}
