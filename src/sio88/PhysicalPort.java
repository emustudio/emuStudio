/*
 * PhysicalPort.java
 *
 * RS-232 physical port
 * 
 * Created on 28.7.2008, 8:22:30
 * hold to: KISS, YAGNI
 *
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

    public int in(EventObject evt) { return port2.in(evt); }
    public void out(EventObject evt, int val) { port2.writeBuffer(val); }

    public String getID() { return "RS232"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 0; }
    public String getVersionRev() { return "b1"; }
}
