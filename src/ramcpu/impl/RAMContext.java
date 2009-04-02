/**
 * RAMContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 */
package ramcpu.impl;

import interfaces.IAbstractTapeContext;
import interfaces.IRAMCPUContext;

import java.util.EventObject;

import javax.swing.event.EventListenerList;

import plugins.device.IDeviceContext;

public class RAMContext implements IRAMCPUContext {
    private final static String KNOWN_TAPE = "ea9beaff230249da3c2e71d91c469c2a"; 
    private EventListenerList listenerList;
    private EventObject cpuEvt;
    private RAM cpu;
    
    private IAbstractTapeContext[] tapes;

    public RAMContext(RAM cpu) {
    	this.cpu = cpu;
        listenerList = new EventListenerList();
        cpuEvt = new EventObject(this);
        tapes = new IAbstractTapeContext[3];
    }
    
	@Override
	public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
	}

	@Override
	public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
	}

	@Override
	public String getHash() {
		return "ce861f51295b912a76a7df538655ab0f";
	}

	@Override
	public String getID() {
		return "ram-cpu-context";
	}

    public void fireCpuRun(int run_state) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).runChanged(cpuEvt, run_state);
        }
    }

    public IAbstractTapeContext getStorage() { return tapes[0]; }
    public IAbstractTapeContext getInput() { return tapes[1]; }
    public IAbstractTapeContext getOutput() { return tapes[2]; }
    
    public void fireCpuState() {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).stateUpdated(cpuEvt);
        }
    }
    
    @Override
    public String attachTape(IDeviceContext tape) {
    	if (tape.getDataType() != String.class ||
    			!tape.getHash().equals(KNOWN_TAPE) ||
    			!(tape instanceof IAbstractTapeContext)) 
    		return null;
    	for (int i = 0; i < 3; i++) {
    		if (tapes[i] == null) {
    			tapes[i] = (IAbstractTapeContext)tape;
    			switch (i) {
    			case 0:
    				tapes[i].setBounded(true);
    				tapes[i].setEditable(true);
    				tapes[i].setPosVisible(false);
    				tapes[i].setClearAtReset(true);
    				return "Registers (storage tape)";
    			case 1:
    				tapes[i].setBounded(true);
    				tapes[i].setEditable(true);
    				tapes[i].setPosVisible(true);
    				tapes[i].setClearAtReset(false);
    				cpu.loadTape(tapes[i]);
    				return "Input tape";
    			case 2:
    				tapes[i].setBounded(true);
    				tapes[i].setEditable(false);
    				tapes[i].setPosVisible(true);
    				tapes[i].setClearAtReset(true);
    				return "Output tape";
    			}
    			return "Unknown";
    		}
    	}
    	return "?";
    }
    
    public void destroy() {
    	for (int i = 0; i < 3; i++) 
    		tapes[i] = null;
        listenerList = null;
    }
    
    /**
     * Method checks if the machine contains correct all 3 tapes
     *  - input
     *  - output
     *  - storage
     * @return true if yes, false otherwise
     */
    public boolean checkTapes() {
    	for (int i = 0; i < 3; i++) 
    		if (tapes[i] == null) return false;
    	return true;
    }
    
}
