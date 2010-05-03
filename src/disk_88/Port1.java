/*
 * Port1.java
 *
 * Created on 18.6.2008, 15:01:27
 * hold to: KISS, YAGNI
 *
 * IN:  disk flags
 * OUT: select/unselect drive
 */

package disk_88;

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
    
    @Override
    public Object in(EventObject evt) {
        return ((Drive)dsk.drives.get(dsk.current_drive)).getFlags();
    }

    @Override
    public void out(EventObject evt, Object val) {
    	short v = (Short)val; 
        // select device
        dsk.current_drive = v & 0x0F;
        if ((v & 0x80) != 0) {
            // disable device
            ((Drive)dsk.drives.get(dsk.current_drive)).deselect();
            dsk.current_drive = 0xFF;
        } else
            ((Drive)dsk.drives.get(dsk.current_drive)).select();
    }
    
    @Override
    public Class<?> getDataType() { return Short.class; }

    @Override
    public String getID() { return "88-DISK-PORT1"; }

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
