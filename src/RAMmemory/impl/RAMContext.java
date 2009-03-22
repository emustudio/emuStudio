/***
 * RAMContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */

package RAMmemory.impl;

import java.util.ArrayList;
import java.util.EventObject;

import javax.swing.event.EventListenerList;

import plugins.memory.IMemoryContext;

public class RAMContext implements IMemoryContext {
	private ArrayList<RAMInstruction> memory; 
	   /* list of devices that wants to get annoucement about memory changes */
    private EventListenerList listenersList;
    private EventObject changeEvent;

    
	public RAMContext() {
		memory = new ArrayList<RAMInstruction>();
        changeEvent = new EventObject(this);
	    listenersList = new EventListenerList();
	}
	
	@Override
	public String getID() {	return "ram-memory-context"; }
	
	@Override
	public void clearMemory() { 
		memory.clear();
		fireChange(-1);
	}

	@Override
	public Class<?> getDataType() { return RAMInstruction.class; }

	public int getSize() { return memory.size(); }
	
	@Override
	public Object read(int pos) {
		if (pos > memory.size()) return null; 
		return memory.get(pos);
	}

	@Override
	public Object readWord(int pos) {
		if (pos > memory.size()) return null; 
		return memory.get(pos);
	}

	@Override
	// RAM memory is readonly! It has to be ensured
	// that this method is called only by a compiler!
	public void write(int pos, Object instr) {
		if (memory.size() <= pos)
			memory.add(pos, (RAMInstruction)instr);
		else memory.set(pos, (RAMInstruction)instr);
		fireChange(pos);
	}

	@Override
	// This method is not and won't be implemented.
	public void writeWord(int pos, Object instr) {}

	@Override
	public String getHash() {
		return "949fe1a163b65ae72a06aeb09976cb47";
	}

	@Override
	public void addMemoryListener(IMemListener listener) {
        listenersList.add(IMemListener.class, listener);
	}

	@Override
	public void removeMemoryListener(IMemListener listener) {
        listenersList.remove(IMemListener.class, listener);		
	}
	
	public void destroy() {
		memory.clear();
		memory = null;
	}
	
   private void fireChange(int adr) {
        Object[] listeners = listenersList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==IMemListener.class) {
                ((IMemListener)listeners[i+1]).memChange(changeEvent, adr);
            }
        }
    }

	
}
