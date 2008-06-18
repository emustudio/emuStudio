/*
 * Mits88SIO_2.java
 *
 * Created on Utorok, 2007, november 13, 17:01
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 *
 * These functions support a simulated MITS 2SIO interface card.
 * The card had two physical I/O ports which could be connected
 * to any serial I/O device that would connect to a current loop,
 * RS232, or TTY interface.  Available baud rates were jumper
 * selectable for each port from 110 to 9600.
 *
 * All I/O is via programmed I/O.  Each each has a status port
 * and a data port.
 * 
 * From: http://www.altair32.com/Altair32specs.htm
 * The standard I/O addresses assigned by MITS was 20Q-21Q  for the first port
 * and 22Q-23Q for the second. The second port of the 2SIO is "connected" to a
 * virtual line printer and the paper tape reader/punch for support under CP/M.
 */

package sio88;

import interfaces.ACpuContext;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;
import gui.TerminalWindow;


/**
 *
 * @author vbmacher
 */
public class Mits88SIO implements IDevice {
    public int buffer;
    public short status;
    private Port1 port1;
    private Port2 port2;
    
    private boolean attached = false;
    private ISettingsHandler settings;

    private ACpuContext cpu = null;
    public TerminalWindow gui = null;

    public Mits88SIO() {
        port1 = new Port1(this);
        port2 = new Port2(this);
    }
    
    public String getDescription() {
        return "Recomended to use as a part of MITS Altair8800 computer. It is"
                + "implemented using MITS SIO serial interface. It have 2 IO ports:"
                + " 0x10(status), 0x11(data). For SIO programming see "
                + "http://www.classiccmp.org/dunfield/s100c/mits/88sio_1.pdf";
    }
    public String getVersion() { return "0.11b"; }
    public String getName() { return "ADM-3A Interactive display terminal"; }
    public String getCopyright() { return "\u00A9 Copyright 2007-2008, Peter Jakubčo"; }
    
    /* Reset routine */
    public void reset() {
        buffer = 0;    /* Data */
        status = 0x02; /* Status */
    }
        
    /**
     * I/O instruction handlers, called from the CPU module when an IN or OUT
     * instruction is issued.
     * Each function is passed an 'io' flag, where 0 means a read from
     * the port, and 1 means a write to the port.  On input, the actual
     * input is passed as the return value, on output, 'data' is written
     * to the device.
     */
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, 
            ISettingsHandler sHandler) {

        // ID of cpu can be ofcourse also else.
        if ((cpu instanceof ACpuContext) == false) {
            StaticDialogs.showErrorMessage("The device can not be attached"
                    + " to this kind of CPU");
            return false;
        }
        
        this.cpu = (ACpuContext) cpu;
        this.settings = sHandler;
        this.attached = false;
        reset();
        // attach IO ports
        if (this.cpu.attachDevice(port1, 0x10) == false) {
            StaticDialogs.showErrorMessage("Error: this device can't be"
                    + " attached (there is a hardware conflict )");
            return false;
        }
        if (this.cpu.attachDevice(port2 ,0x11) == false) {
            StaticDialogs.showErrorMessage("Error: this device can't be"
                    + " attached (maybe there is a hardware conflict)");
            this.cpu.detachDevice(0x10);
            return false;
        }
        this.attached = true;
        return true;
    }

    public IDeviceContext[] getContext() {
        IDeviceContext[] idev = {port1, port2};
        return idev;
    }
   
    public void showGUI() {
        if (gui == null) gui = new TerminalWindow(this);
        gui.setVisible(true);
    }
    
    public void destroy() {
        cpu.detachDevice(0x10);
        cpu.detachDevice(0x11);
        this.attached = false;
        if (gui != null) {
            gui.destroyMe();
            gui.dispose();
        }
        gui = null;
    }
    
    public boolean isAttached() { return attached; }

    /**
     * This is communication method between terminal and
     * SIO device. If user pressed a key, then it is
     * sent from terminal to SIO device via this method.
     */
    public void writeBuffer(int data) {
        status |= 0x01;
        buffer = data;
    }

}
