/*
 * Port3.java
 *
 * Created on 18.6.2008, 15:13:58
 * hold to: KISS, YAGNI
 *
 */

package disk;

import java.io.IOException;
import java.util.EventObject;
import plugins.device.IDeviceContext;
import runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class Port3 implements IDeviceContext {
    private DiskImpl dsk;
    
    public Port3(DiskImpl dsk) {
        this.dsk = dsk;
    }
    
    public boolean attachDevice(IDeviceContext device) { return false; }
    public void detachDevice(IDeviceContext device) {}

    public int in(EventObject evt) {
        int d = 0;
        try { d = ((Drive)dsk.drives.get(dsk.current_drive)).readData(); }
        catch(IOException e) {
            StaticDialogs.showErrorMessage("Couldn't read from disk");
        }
        return d;
    }

    public void out(EventObject evt, int val) {
        try {
            ((Drive)dsk.drives.get(dsk.current_drive)).writeData(val);
        } catch(IOException e) {
            StaticDialogs.showErrorMessage("Couldn't write to disk");
        }
    }

    public String getID() { return "88-DISK-PORT3"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 0; }
    public String getVersionRev() { return "b1"; }
    
}
