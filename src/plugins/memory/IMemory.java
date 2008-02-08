/*
 * IMemory.java
 *
 * Created on Nedeľa, 2007, august 12, 18:48
 *
 * KEEP IT SIMPLE STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */
package plugins.memory;

import java.util.*;
import plugins.IPlugin;

/**
 *
 * @author vbmacher
 */
public interface IMemory extends IPlugin {
    
    // OM size (can set size only once)
    public boolean init(int size);

    // visual
    public void showGUI();
    
    public int getSize();    
    public boolean loadHex(String filename);
    public boolean loadBin(String filename, int address);
    public int getLastImageStart();

    // banking
    public boolean bankingSupported(); // ci sa da nastavit bankovanie
    public void setBanksCount(short count); // ak je count = 0, bankovanie sa rusi
    public short getBanksCount();
    public void setActiveBank(short bank) throws IndexOutOfBoundsException;
    public short getActiveBank();
    public int getBankCapacity();
    
    // RAM/ROM
    public void setRAM(int from, int to);
    public void setROM(int from, int to);

    // reading/writing
    public void write8(int address, short by);
    public short read8(int address);
    public void write16(int address, int wo);
    public int read16(int address);
    
    // device mapping (for memory mapped devices)
    public interface IMemListener extends EventListener {
        public void memChange(EventObject evt, int adr, int bank);
    }
    /**
     * Method for registering device for memory change annoucement.
     * @param listener a listener that device implements for getting
     *        annoucements from memory
     */
    public void registerDeviceDMA(IMemory.IMemListener listener);
    public void unregisterDeviceDMA(IMemory.IMemListener listener);
    
}
