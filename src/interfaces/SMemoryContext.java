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
 * Specific context for this kind of memory
 * 
 * @author vbmacher
 */
public interface SMemoryContext extends IMemoryContext {
    public boolean isRom(int address);
    public Hashtable getROMRanges();
    public void setRAM(int from, int to);
    public void setROM(int from, int to);

    public boolean loadHex(String filename);
    public boolean loadBin(String filename, int address);

    public void clear();
    
}
