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
    private final static String KNOWN_TAPE = "c642e5f1dc280113ccd8739f3c01a06d"; 
    private EventListenerList listenerList;
    private EventObject cpuEvt;
    
    private IAbstractTapeContext[] tapes;

    public RAMContext() {
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
    				return "Registers (storage tape)";
    			case 1:
    				tapes[i].setBounded(true);
    				tapes[i].setEditable(true);
    				tapes[i].setPosVisible(true);
    				return "Input tape";
    			case 2:
    				tapes[i].setBounded(true);
    				tapes[i].setEditable(false);
    				tapes[i].setPosVisible(true);
    				return "Output tape";
    			}
    			return "Unknown";
    		}
    	}
    	return null;
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
