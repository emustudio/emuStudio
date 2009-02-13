/**
 * BrainMemContext.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 * KISS, YAGNI
 */

package brainduckmem.impl;

import java.util.EventObject;
import javax.swing.event.EventListenerList;

import plugins.memory.IMemoryContext;

public class BrainMemContext implements IMemoryContext {
    private short[] mem; // toto je operačná pamäť

    // zoznam listenerov, ktorí budú informovaní
    // o zmenách OP
    private EventListenerList deviceList; 
    private EventObject changeEvent;

    // Konštruktor
    public BrainMemContext() {
        changeEvent = new EventObject(this);
        deviceList = new EventListenerList();
    }
    
    /**
     * Inicializuje kontext pamäte. Metóda je
     * volaná z implementácie hlavného rozhrania.
     * 
     * @param size  Veľkosť pamäte
     * @return      vráti true ak inicializácia bola OK
     */
    public boolean init(int size) {
        mem = new short[size];
        return true;
    }
    
	@Override
	public void clearMemory() {
        for (int i = 0; i < mem.length; i++) 
            mem[i] = 0;
        fireChange(-1); // informuj o zmene
	}

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public Object read(int from) { 
		return mem[from];
	}
	
	@Override
	public Object readWord(int from) {
        if (from == mem.length-1) return mem[from];
        int low = mem[from] & 0xFF;
        int high = mem[from+1];
        return (int)((high << 8)| low);
	}

	@Override
	public void write(int to, Object val) {
        if (val instanceof Integer)
            mem[to] = (short)((Integer)val & 0xFF);
        else
            mem[to] = (short)((Short)val & 0xFF);
        fireChange(to);
	}

	@Override
	public void writeWord(int to, Object val) {
        short low = (short)((Integer)val & 0xFF);
        mem[to] = low;
        fireChange(to);
        if (to < mem.length-1) {
            short high = (short)(((Integer)val >>> 8) & 0xFF);
            mem[to+1] = high;
            fireChange(to+1);
        }
	}

	@Override
	public void addMemoryListener(IMemListener listener) {
		deviceList.add(IMemListener.class, listener);
	}

	@Override
	public void removeMemoryListener(IMemListener listener) {
		deviceList.remove(IMemListener.class, listener);
	}

	@Override
	public String getID() {
		return "brainduck_memory";
	}

	/**
	 * Metóda vráti jednoznačný hash tohto kontextu.
	 * Hash je vypočítaný podľa špecifického algoritmu.
     *
	 * @return hash kontextu
	 */
	@Override
	public String getHash() {
		return "949fe1a163b65ae72a06aeb09976cb47";
	}

	/***
	 * Táto metóda notifikuje všetkých listenerov,
	 * že nastala zmena v bunke operačnej pamäti.
	 * @param adr Adresa, na ktorej nastala zmena
	 */
    private void fireChange(int adr) {
        Object[] listeners = deviceList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==IMemListener.class) {
                ((IMemListener)listeners[i+1]).memChange(changeEvent, adr);
            }
        }
    }
	
}
