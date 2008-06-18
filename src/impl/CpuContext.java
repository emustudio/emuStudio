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
import java.util.HashSet;
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
    private HashSet breaks; // zoznam breakpointov (mnozina)
    private int clockFrequency = 2000; // kHz

    public CpuContext() {
        devicesList = new Hashtable();
        breaks = new HashSet();
        listenerList = new EventListenerList();
    }
    
    public String getID() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getVersionMajor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getVersionMinor() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getVersionRev() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getInstrPosition() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNextInstrPos(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean setInstrPosition(int pos) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
    }

    public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
    }

    public boolean isBreakpointSupported() {
        return true;
    }

    public void setBreakpoint(int pos, boolean set) {
        if (set) breaks.add(pos);
        else breaks.remove(pos);
    }

    public boolean getBreakpoint(int pos) {
        return breaks.contains(pos);
    }

    // device mapping = only one device can be attached to one port
    public boolean attachDevice(IDeviceContext listener, int port) {
        if (devicesList.containsKey(port)) return false;
        devicesList.put(port, listener);
        return true;
    }
    public void disattachDevice(int port) {
        if (devicesList.containsKey(port))
            devicesList.remove(port);
    }
    
    public void clearDevices() {
        devicesList.clear();
    }
    
    public int getFrequency() { return this.clockFrequency; }
    // frequency in kHz
    public void setFrequency(int freq) { this.clockFrequency = freq; }
    

    public void fireCpuRun(statusGUI status, stateEnum run_state) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] instanceof ICPUListener)
                ((ICPUListener)listeners[i+1]).runChanged(cpuEvt, run_state);
        }
        status.updateGUI();
    }

    public void fireCpuState() {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] instanceof ICPUListener)
                ((ICPUListener)listeners[i+1]).stateUpdated(cpuEvt);
        }
    }

    public void fireFrequencyChanged(float freq) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] instanceof ACpuListener)
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
            if (read == true) return 0;
        }
        if (read == true) 
            return (short)((IDeviceContext)devicesList.get(port)).in(cpuEvt);
        else ((IDeviceContext)devicesList.get(port)).out(cpuEvt,val);
        return 0;
    }

}
