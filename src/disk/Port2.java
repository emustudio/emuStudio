/*
 * Port2.java
 *
 * Created on 18.6.2008, 15:10:20
 * hold to: KISS, YAGNI
 *
 * IN: sector pos
 * OUT: set flags
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

    @Override
    public Object in(EventObject evt) {
        return ((Drive)dsk.drives.get(dsk.current_drive)).getSectorPos();
    }

    @Override
    public void out(EventObject evt, Object val) {
        ((Drive)dsk.drives.get(dsk.current_drive)).setFlags((Short)val);
    }

    public String getID() { return "88-DISK-PORT2"; }

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
