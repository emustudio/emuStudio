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

import gui.SIODialog;
import interfaces.ACpuContext;
import plugins.ISettingsHandler;
import plugins.cpu.ICPUContext;
import plugins.device.IDevice;
import plugins.device.IDeviceContext;
import plugins.memory.IMemoryContext;
import runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class Mits88SIO implements IDevice {
	private static final String KNOWN_CPU_CONTEXT_HASH = "4bb574accc0ed96b5ed84b5832127289"; 
	
	private long hash;
    public short buffer;
    public short status;
    private CpuPort1 port1;
    private CpuPort2 port2;
    private PhysicalPort malePlug;
    private boolean port2Attached = false;
    
    @SuppressWarnings("unused")
	private ISettingsHandler settings;

    private ACpuContext cpu = null;
    public SIODialog gui = null;

    public Mits88SIO(Long hash) {
    	this.hash = hash;
        port1 = new CpuPort1(this);
        port2 = new CpuPort2(this);
        malePlug = new PhysicalPort(port2);
    }
    
    @Override
    public String getDescription() {
        return "Recomended to use with MITS Altair8800 computer. This is"
                + " an implementation of MITS 88-SIO serial card. It has one"
                + " physical port and 2 programmable IO ports:"
                + " 0x10(status), 0x11(data). For programming see manual at\n"
                + "http://www.classiccmp.org/dunfield/s100c/mits/88sio_1.pdf";
    }
    @Override
    public String getVersion() { return "0.12b"; }
    @Override
    public String getTitle() { return "MITS-88-SIO serial card"; }
    @Override
    public String getCopyright() { return "\u00A9 Copyright 2007-2009, P.Jakubčo"; }
    
    /* Reset routine */
    @Override
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
    @Override
    public boolean initialize(ICPUContext cpu, IMemoryContext mem, 
            ISettingsHandler sHandler) {

        this.settings = sHandler;
        if (cpu == null) return true;
        
        // ID of cpu can be ofcourse also else.
        if (!cpu.getHash().equals(KNOWN_CPU_CONTEXT_HASH)) {
            StaticDialogs.showErrorMessage("88-SIO device can not be attached"
                    + " to this kind of CPU");
            return false;
        }
        this.cpu = (ACpuContext) cpu;
        
        // attach IO ports
        if (this.cpu.attachDevice(port1, 0x10) == false) {
            StaticDialogs.showErrorMessage("Error: 88-SIO (port1) can't be"
                    + " attached to CPU (maybe there is a hardware conflict)");
            return false;
        }
        if (this.cpu.attachDevice(port2 ,0x11) == false) {
            StaticDialogs.showErrorMessage("Error: 88-SIO (port2) can't be"
                    + " attached to CPU (maybe there is a hardware conflict)");
            this.cpu.detachDevice(0x10);
            return false;
        }
        return true;
    }
   
    @Override
    public void showGUI() {
        if (gui == null) {
            String name = (port2.getAttachedDevice() == null) ? "none" :
                port2.getAttachedDevice().getID();
            gui = new SIODialog(null,false, name);
        }
        gui.setVisible(true);
    }
    
    @Override
    public void destroy() {
    	detachAll();
        cpu.detachDevice(0x10);
        cpu.detachDevice(0x11);
        if (gui != null) {
            gui.dispose();
        }
        gui = null;
    }

    /**
     * Attach a male plug (other device). Theoretically into this port
     * can be attached almost anything...
     * 
     * @param male device that is going to be connected to this device
     * @return true if attachment was completed successfully
     */
    @Override
    public boolean attachDevice(IDeviceContext male) {
        if (!port2Attached) {
            port2.attachDevice(male);
            port2Attached = true;
            return true;
        }
        return false;
    }

    /**
     * Detach all devices that are connected to any of the port.
     * If port is free already, nothing happened.
     */
	@Override
	public void detachAll() {
        if (port2Attached) {
        	port2.detachDevice();
        	port2Attached = false;
        }	
	}

    /**
     * Return "male plug" of the port that is connected to some
     * device.
     * This way is ensured two-way connection, eg.:
     * 
     * SIO <--> TERMINAL
     * 
     * @return male plug (port)
     */
	@Override
	public IDeviceContext getNextContext() { return malePlug; }

	@Override
	public long getHash() { return hash; }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
