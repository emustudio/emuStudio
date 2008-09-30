/*
 * SIMHpseudo.java
 *
 * (c) Copyright 2008, P. Jakubco
 * 
 * hold to: KISS, YAGNI
 */

package simh;

import interfaces.ACpuContext;
import interfaces.SMemoryContext;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

/**
 * SIMH emulator's pseudo device
 * 
 * @author vbmacher
 */
public class SIMHpseudo implements IDevice {
    private PseudoContext male;
    private ACpuContext cpu;
    private SMemoryContext mem;
                
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, ISettingsHandler sHandler) {
        // ID of cpu can be ofcourse also else.
        if ((cpu instanceof ACpuContext) == false) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device can not be attached"
                    + " to this kind of CPU");
            return false;
        }

        if ((mem instanceof SMemoryContext) == false) {
            StaticDialogs.showErrorMessage("SIMH-pseudo device can't collaborate "
                    + "with this kind of MEMORY");
            return false;
        }

        this.cpu = (ACpuContext) cpu;
        this.mem = (SMemoryContext) mem;
        
        male = new PseudoContext(this.mem);
        
        // attach IO port
        if (this.cpu.attachDevice(male, 0xFE) == false) {
            StaticDialogs.showErrorMessage("Error: SIMH device can't be"
                    + " attached to CPU (there is a hardware conflict)");
            return false;
        }
        reset();
        return true;
    }

    public void showGUI() {
        StaticDialogs.showMessage("GUI not supported");
    }

    public IDeviceContext getFreeFemale() { return null; }

    public IDeviceContext getFreeMale() { return male; }

    public boolean attachDevice(IDeviceContext female, IDeviceContext male) {
        return false;
    }

    public void detachDevice(IDeviceContext device, boolean male) {}

    public void reset() {
        male.reset();
    }

    public String getName() { return "SIMH pseudo device"; }
    public String getCopyright() {
        return "Copyright (c) 2002-2007, Peter Schorn\n\u00A9 Copyright 2007-2008, Peter Jakubčo";
    }
    public String getDescription() {
        return "Some of the images taken from SIMH emulator contain Z80 or 8080" +
                " programs that communicate with the SIMH pseudo device "
                + "via port 0xfe. The version of the interface is: SIMH003";
    }
    public String getVersion() { return "0.1b"; }

    public void destroy() {}

}
