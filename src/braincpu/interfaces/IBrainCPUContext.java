/*
 * IBrainCPUContext.java
 *
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */

package braincpu.interfaces;

import plugins.cpu.ICPUContext;
import plugins.device.IDeviceContext;

public interface IBrainCPUContext extends ICPUContext {
    public boolean attachDevice(IDeviceContext device);
    public void detachDevice();
}
