/**
 * BrainCPUContext.java
 * 
 * KISS, YAGNI
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
package braincpu.impl;

import java.util.EventObject;
import javax.swing.event.EventListenerList;
import braincpu.interfaces.IBrainCPUContext;
import plugins.device.IDeviceContext;

public class BrainCPUContext implements IBrainCPUContext {
    private EventListenerList listenerList;
    private EventObject cpuEvt;
    private IDeviceContext device;

    public BrainCPUContext() {
        device = null;
        listenerList = new EventListenerList();
        cpuEvt = new EventObject(this);
    }
    
    @Override
    public String getID() { return "brain-cpu-context"; }
    @Override
    public String getHash() { return "d32e73041bf765d98eea1b8664ef6b5e";}

    @Override
    public void addCPUListener(ICPUListener listener) {
        listenerList.add(ICPUListener.class, listener);
    }

    @Override
    public void removeCPUListener(ICPUListener listener) {
        listenerList.remove(ICPUListener.class, listener);
    }

    /**
     * Pripojenie zariadenia do CPU. Procesor BainCPU môže mať
     * pripojené len jedno zariadenie, a to terminál
     * 
     * @param listener
     * @param port
     * @return
     */
    @Override
    public boolean attachDevice(IDeviceContext device) {
    	if (this.device != null) return false;
    	if (device.getDataType() != Short.class) return false;
    	this.device = device;
        return true;
    }
    @Override
    public void detachDevice() {
    	device = null;
    }
    
    /**
     * Túto metódu zavolá BrainCPU, keď sa zmení
     * stav behu procesora. Táto metóda potom zavolá
     * metódu runChanged všetkých listenerov.
     * 
     * @param run_state  nový stav behu procesora
     */
    public void fireCpuRun(int run_state) {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).runChanged(cpuEvt, run_state);
        }
    }

    /**
     * Túto metódu zavolá BrainCPU, keď sa zmení
     * vnútorný stav procesora (zmenia sa registre).
     * Táto metóda potom zavolá metódu stateUpdated
     * všetkých listenerov.
     */
    public void fireCpuState() {
        Object[] listeners = listenerList.getListenerList();
        for (int i=0; i<listeners.length; i+=2) {
            if (listeners[i] == ICPUListener.class)
                ((ICPUListener)listeners[i+1]).stateUpdated(cpuEvt);
        }
    }
    
    /**
     * Metóda zapíše do zariadenia hodnotu val. Táto
     * metóda teda priamo komunikuje so zariadením.
     * @param val hodnota, ktorá sa má zapísať do zariadenia
     */
    public void writeToDevice(short val) {
    	if (device == null) return;
    	device.out(cpuEvt, val);
    }
    
    /**
     * Metóda prečíta hodnotu zo zariadenia. Ak zariadenie
     * nemá nič čo poslať, nech pošle 0.. Táto metóda
     * teda priamo komunikuje so zariadením.
     * @return hodnotu zo zariadenia
     */
    public short readFromDevice() {
    	if (device == null) return 0;
    	return (Short)device.in(cpuEvt);
    }

}
