/***
 * RAMContext.java
 * 
 *  KISS, YAGNI
 *
 * Copyright (C) 2009-2010 Peter Jakubčo <pjakubco at gmail.com>
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package RAMmemory.impl;

import interfaces.IRAMInstruction;
import interfaces.IRAMMemoryContext;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.event.EventListenerList;

public class RAMContext implements IRAMMemoryContext {
	private ArrayList<IRAMInstruction> memory; 
	   /* list of devices that wants to get annoucement about memory changes */
    private EventListenerList listenersList;
    private EventObject changeEvent;
    private Hashtable<Integer,String> labels;
    private Vector<String> inputs; // not for memory, but for CPU. Memory holds program so...
    
	public RAMContext() {
		memory = new ArrayList<IRAMInstruction>();
        changeEvent = new EventObject(this);
	    listenersList = new EventListenerList();
	    labels = new Hashtable<Integer,String>();
	    inputs = new Vector<String>();
	}
	
	@Override
	public String getID() {	return "ram-memory-context"; }
	
	@Override
	public void clearMemory() { 
		memory.clear();
		labels.clear();
		inputs.clear();
		fireChange(-1);
	}
	
	public void clearInputs() {
		inputs.clear();
	}

	@Override
	public Class<?> getDataType() { return IRAMInstruction.class; }

	public int getSize() { return memory.size(); }
	
	@Override
	public Object read(int pos) {
		if (pos >= memory.size()) return null; 
		return memory.get(pos);
	}

	@Override
	public Object readWord(int pos) {
		if (pos >= memory.size()) return null; 
		return memory.get(pos);
	}
	
	// This method is not and won't be implemented.
	@Override
	public void write(int pos, Object instr) {
		if (pos >= memory.size())
			memory.add(pos,(IRAMInstruction)instr);
		else
			memory.set(pos, (IRAMInstruction)instr);
		fireChange(pos);
	}

	// This method is not and won't be implemented.
	@Override
	public void writeWord(int pos, Object instr) {}

	@Override
	public void addLabel(int pos, String label) {
		labels.put(pos, label);
	}
	
	@Override
	public String getLabel(int pos) {
		return labels.get(pos);
	}

	public Hashtable<String,Integer> getSwitchedLabels() {
		Hashtable<String,Integer> h = new Hashtable<String,Integer>();
		Enumeration<Integer> k = labels.keys();
		while (k.hasMoreElements()) {
			int pos = k.nextElement();
			h.put(labels.get(pos), pos);
		}
	    return h;
	}
	
	@Override
	public void addInputs(Vector<String> inputs) {
		if (inputs == null) return;
		this.inputs.addAll(inputs);
	}

	@Override
	public Vector<String> getInputs() {
		return inputs;
	}	
	
	@Override
	public String getHash() {
		return "894da3cf31d433afcee33c22a64d2ed9";
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
