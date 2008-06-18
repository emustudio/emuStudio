/*
 * Port2.java
 *
 * Created on 18.6.2008, 15:10:20
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
public class Port2 implements IDeviceContext {
    private DiskImpl dsk;
    
    public Port2(DiskImpl dsk) {
        this.dsk = dsk;
    }
    
    public boolean attachDevice(IDeviceContext device) { return false; }
    public void detachDevice(IDeviceContext device) {}

    public int in(EventObject evt) {
        return ((Drive)dsk.drives.get(dsk.current_drive)).getSectorPos();
    }

    public void out(EventObject evt, int val) {
        ((Drive)dsk.drives.get(dsk.current_drive)).setFlags((short)val);
    }

    public String getID() { return "88-DISK-PORT2"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 0; }
    public String getVersionRev() { return "b1"; }

}
