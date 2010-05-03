/*
 * Port3.java
 *
 * Created on 18.6.2008, 15:13:58
 * hold to: KISS, YAGNI
 *
 * IN: read data
 * OUT: write data
 */

package disk_88;

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

    @Override
    public Object in(EventObject evt) {
        short d = 0;
        try { d = ((Drive)dsk.drives.get(dsk.current_drive)).readData(); }
        catch(IOException e) {
            StaticDialogs.showErrorMessage("Couldn't read from disk");
        }
        return d;
    }

    @Override
    public void out(EventObject evt, Object val) {
        try {
            ((Drive)dsk.drives.get(dsk.current_drive)).writeData((Short)val);
        } catch(IOException e) {
            StaticDialogs.showErrorMessage("Couldn't write to disk");
        }
    }

    @Override
    public String getID() { return "88-DISK-PORT3"; }

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}


    
}
