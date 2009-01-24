/*
 * ACpuContext.java
 * (interface)
 *
 * Created on 18.6.2008, 8:56:44
 * hold to: KISS, YAGNI
 *
 */

package interfaces;

import plugins.cpu.ICPUContext;
import plugins.device.IDeviceContext;

/**
 * CPU context for 8080 processor
 * @author vbmacher
 */
public interface ACpuContext extends ICPUContext {
    public boolean attachDevice(IDeviceContext listener, int port);
    public void detachDevice(int port);
    
    public void interrupt(byte[] instr);
}
