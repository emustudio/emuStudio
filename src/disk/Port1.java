/*
 * Port1.java
 *
 * Created on 18.6.2008, 15:01:27
 * hold to: KISS, YAGNI
 *
 */

package disk;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class Port1 implements IDeviceContext {
    private DiskImpl dsk;
    
    public Port1(DiskImpl dsk) {
        this.dsk = dsk;
    }
    
    public boolean attachDevice(IDeviceContext device) { return false; }
    public void detachDevice(IDeviceContext device) { }
    
    public int in(EventObject evt) {
        return ((Drive)dsk.drives.get(dsk.current_drive)).getFlags();
    }

    public void out(EventObject evt, int val) {
        // select device
        dsk.current_drive = val & 0x0F;
        if ((val & 0x80) != 0) {
            // disable device
            ((Drive)dsk.drives.get(dsk.current_drive)).deselect();
            dsk.current_drive = 0xFF;
        } else
            ((Drive)dsk.drives.get(dsk.current_drive)).select();

    }

    public String getID() { return "88-DISK-PORT1"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 0; }
    public String getVersionRev() { return "b1"; }

}
