/*

 * CpuContext.java
 *
 * Created on 18.6.2008, 8:50:11
 * hold to: KISS, YAGNI
 *
 */

package impl;

import gui.statusGUI;
import interfaces.ACpuContext;
import interfaces.ACpuListener;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.event.EventListenerList;
import plugins.device.IDeviceContext;


/**
 *
 * @author vbmacher
 */
public class CpuContext implements ACpuContext {
    private EventListenerList listenerList;
    private EventObject cpuEvt = new EventObject(this);
    private Hashtable devicesList;
    private int clockFrequency = 2000; // kHz
    private Cpu8080 cpu;

    public CpuContext(Cpu8080 cpu) {
        devicesList = new Hashtable();
        listenerList = new EventListenerList();
        this.cpu = cpu;
    }
    
    public String getID() { return "i8080"; }
    public int getVersionMajor() { return 1; }
    public int getVersionMinor() { return 8; }
    public String getVersionRev() { return "b1"; }

    public int getInstrPosition() { return cpu.getPC(); }
    public int getNextInstrPos(int pos) { return cpu.getNextPC(pos); }
    public boolean setInstrPosition(int pos) { return cpu.setPC(pos); }

    public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
    }

    public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
    }

    // device mapping = only one device can be attached to one port
    public boolean attachDevice(IDeviceContext listener, int port) {
        if (devicesList.containsKey(port)) return false;
        devicesList.put(port, listener);
        return true;
    }
    public void detachDevice(int port) {
        if (devicesList.containsKey(port))
            devicesList.remove(port);
    }
    
    public void clearDevices() { devicesList.clear(); }
    
    public int getFrequency() { return this.clockFrequency; }
    // frequency in kHz
    public void setFrequency(int freq) { this.clockFrequency = freq; }
    
    public void fireCpuRun(statusGUI status, stateEnum run_state) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).runChanged(cpuEvt, run_state);
        }
        status.updateGUI();
    }

    public void fireCpuState() {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).stateUpdated(cpuEvt);
        }
    }

    public void fireFrequencyChanged(float freq) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i+1] instanceof ACpuListener)
                ((ACpuListener)listeners[i+1]).frequencyChanged(cpuEvt,freq);
        }
    }

    /**
     * Performs I/O operation.
     * @param port I/O port
     * @param read whether method should read or write to the port
     * @param val value to be written to the port. if parameter read is set to
     *            true, then val is ignored.
     * @return value from the port if read is true, otherwise 0
     */
    public short fireIO(int port, boolean read, short val) {
        if (devicesList.containsKey(port) == false) {
            // this behavior isn't constant for all situations...
            return 0;
        }
        if (read == true) 
            return (short)((IDeviceContext)devicesList.get(port)).in(cpuEvt);
        else ((IDeviceContext)devicesList.get(port)).out(cpuEvt,val);
        return 0;
    }

}
