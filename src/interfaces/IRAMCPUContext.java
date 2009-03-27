/**
 * IRAMCPUContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */
package interfaces;

import plugins.cpu.ICPUContext;
import plugins.device.IDeviceContext;

public interface IRAMCPUContext extends ICPUContext {
    public String attachTape(IDeviceContext tape);

}
