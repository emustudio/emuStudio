/*
 * PhysicalPort.java
 *
 * RS-232 physical port
 * 
 * Created on 28.7.2008, 8:22:30
 * hold to: KISS, YAGNI
 *
 * Male plug
 */

package sio88;

import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class PhysicalPort implements IDeviceContext {
    private CpuPort2 port2;
    
    public PhysicalPort(CpuPort2 port2) { this.port2 = port2; }

    @Override
    public Object in(EventObject evt) { 
    	return port2.in(evt);
    }

    @Override
    public void out(EventObject evt, Object val) {
        short v = (Short)val;
    	port2.writeBuffer(v);
    }
    
    @Override
    public Class<?> getDataType() {
    	return Short.class;
    }

    @Override
    public String getID() { return "RS232"; }

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
