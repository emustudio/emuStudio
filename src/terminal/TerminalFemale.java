/*
 * TerminalFemale.java
 *
 * Created on 28.7.2008, 21:38:02
 * hold to: KISS, YAGNI
 * 
 * Female plug
 *
 */

package terminal;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class TerminalFemale implements IDeviceContext {
    private IDeviceContext dev;
        
    public void attachDevice(IDeviceContext device) { this.dev = device; }
    public void detachDevice() { this.dev = null; }

    @Override
    public Object in(EventObject evt) { return (short)0; }

    @Override
    public void out(EventObject evt, Object val) {
        if (dev == null) return;
        dev.out(evt, val);
    }

    @Override
    public String getID() { return "ADM-3A"; }
    
	@Override
	public Class<?> getDataType() {
		return Short.class;
	}
	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
