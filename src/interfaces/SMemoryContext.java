/*
 * SMemoryContext.java
 * (interface)
 *
 * Created on 18.6.2008, 8:29:36
 * hold to: KISS, YAGNI
 *
 */

package interfaces;

import java.util.Hashtable;
import plugins.memory.IMemoryContext;

/**
 * Specific context for this kind of memory.
 * Supports banking, ROM ranges, loading HEX/BIN files.
 * 
 * @author vbmacher
 */
public interface SMemoryContext extends IMemoryContext {
    public boolean isRom(int address);
    public Hashtable<Integer,Integer> getROMRanges();
    public void setRAM(int from, int to);
    public void setROM(int from, int to);

    public int getBanksCount();
    public short getSelectedBank();
    public void setSeletedBank(short bankSelect);
    public int getCommonBoundary();

    public boolean loadHex(String filename, int bank);
    public boolean loadBin(String filename, int address, int bank);
    
}
