/*
 * CpuContext.java
 *
 * Created on 18.6.2008, 8:50:11
 * hold to: KISS, YAGNI
 *
 */

package impl;

import interfaces.IICpuListener;
import interfaces.ACpuContext;
import gui.statusGUI;
import java.util.EventObject;
import java.util.Hashtable;
import javax.swing.event.EventListenerList;
import plugins.device.IDeviceContext;


/**
 *
 * @author vbmacher
 */
public final class CpuContext implements ACpuContext {
    private EventListenerList listenerList;
    private EventObject cpuEvt = new EventObject(this);
    private Hashtable<Integer,IDeviceContext> devicesList;
    private volatile int clockFrequency = 20000; // kHz
    private Object frequencyLock = new Object(); // synchronize lock

    public CpuContext() {
        devicesList = new Hashtable<Integer,IDeviceContext>();
        listenerList = new EventListenerList();
    }
    
    @Override
    public String getID() { return "Z80Context"; }

    @Override
    public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
    }

    @Override
    public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
    }

    // device mapping = only one device can be attached to one port
    @Override
    public boolean attachDevice(IDeviceContext device, int port) {
        if (devicesList.containsKey(port)) return false;
        if (!device.getDataType().equals(Short.class)) return false;
        devicesList.put(port, device);
        return true;
    }
    @Override
    public void detachDevice(int port) {
        if (devicesList.containsKey(port))
            devicesList.remove(port);
    }
    
    public void clearDevices() { devicesList.clear(); }
    
    public int getFrequency() {
        synchronized(frequencyLock) {
            return this.clockFrequency;
        }
    }
    
    // frequency in kHz
    public void setFrequency(int freq) {
        synchronized(frequencyLock) {
            this.clockFrequency = freq;
        }
    }
    
    public void fireCpuRun(statusGUI status, int run_state) {
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
            if (listeners[i+1] instanceof IICpuListener)
                ((IICpuListener)listeners[i+1]).frequencyChanged(cpuEvt,freq);
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
            // on ALTAIR computer it depends on setting of one switch on front
            // panel (called IR or what..)
            return 0;
        }
        if (read == true) 
            return (Short)devicesList.get(port).in(cpuEvt);
        else devicesList.get(port).out(cpuEvt,val);
        return 0;
    }

    // TODO implement, please...
    public void interrupt(byte[] instr) {
        
    }

	/**
	 * Method returns hash of method names of implemented interface.
	 * It is an variance of signing.
	 * @return hash of the context
	 */
	@Override
	public String getHash() {
		return "4bb574accc0ed96b5ed84b5832127289";
    }


}
