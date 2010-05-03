/*
 * MemoryContext.java
 *
 * Created on 18.6.2008, 8:00:16
 * hold to: KISS, YAGNI
 *
 */

package standard_mem;

import interfaces.SMemoryContext;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.Hashtable;
import java.util.Vector;
import javax.swing.event.EventListenerList;

import standard_mem.gui.frmMemory;
import runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class MemoryContext implements SMemoryContext {
    public int lastImageStart = 0;
    private boolean lastStartSet = false;
    private short[][] mem;
    private boolean sizeSet; // whether memory was initialized (created)
    private int banksCount;
    private short bankSelect = 0;
    private int bankCommon = 0;
    private int b;
    private frmMemory gui;

    // this table contains ROM parts of memory
    private Hashtable<Integer,Integer> romBitmap; // keys: low boundary (limit); values: upper boundary
    /* list of devices that wants to get annoucement about memory changes */
    private EventListenerList deviceList;
    private EventObject changeEvent;

    public MemoryContext() {
        sizeSet = false;
        romBitmap = new Hashtable<Integer,Integer>();
        changeEvent = new EventObject(this);
        deviceList = new EventListenerList();
    }
    
    public boolean init(int size, int banks, int bankCommon,frmMemory gui) {
        this.gui = gui;
        if (sizeSet == true) return false;
        this.bankCommon = bankCommon;
        if (banks <= 0) banks=1;
        this.banksCount = banks;
        mem = new short[size][banks];
        sizeSet = true;
        return true;
    }

    @Override
    public String getID() { return "byte_simple_variable"; }

    /**
     * Clears memory content.
     */
    @Override
    public void clearMemory() {
        if (sizeSet == false) return;
        for (int i = 0; i < mem.length; i++) 
            for (int j = 0; j < banksCount;j++) mem[i][j] = 0;
        lastImageStart = 0;
        fireChange(-1);
    }

    @Override
    public int getBanksCount() { return banksCount; }
    
    @Override
    public short getSelectedBank() { return bankSelect; }
    
    @Override
    public void setSeletedBank(short bankSelect) {
        if (bankSelect < banksCount) {
            this.bankSelect = bankSelect;
            if (gui != null) gui.updateBank(bankSelect);
        }
    }
    
    @Override
    public int getCommonBoundary() { return bankCommon; }
    
    // this can parse classic old data
    // line beginning with ; is ignored
    public boolean loadHex(String filename, int bank) {
        if (sizeSet == false) return false;
        lastStartSet = false;
        try {
            FileReader vstup = new FileReader(filename);
            int i =0,j =0;
            while((i = vstup.read()) != -1) {
                while ((char)i == ' ') i = (char)vstup.read();
                if ((char)i == ';') {
                    while((i = vstup.read()) != -1) if ((char)i == '\n' || i == 0x0D) break;
                    continue;
                }
                char c = (char)i;
                if (c == ':') i = vstup.read();
                if (i == -1) { vstup.close(); return false; }
                c = (char)i;
                j = vstup.read();
                if (j == -1) { vstup.close(); return false; }
                char d = (char)j;
                // data bytes count
                int byteCount = Integer.decode(String.format("0x%c%c",c,d));
                if (byteCount == 0) {
                    while((i = vstup.read()) != -1) if ((char)i == '\n' || i == 0x0D) break;
                    continue;
                }
                // address
                i = vstup.read();
                if (i == -1) { vstup.close(); return false; }
                c = (char)i;
                j = vstup.read();
                if (j == -1) { vstup.close(); return false; }
                d = (char)j;
                int k = vstup.read();
                if (k == -1) { vstup.close(); return false; }
                char e = (char)k;
                int l = vstup.read();
                if (l == -1) { vstup.close(); return false; }
                char f = (char)l;
                int address = Integer.decode(String.format("0x%c%c%c%c",c,d,e,f));
                if (lastStartSet == false) {
                    lastImageStart = address;
                    lastStartSet = true;
                }
                
                // data type
                i = vstup.read();
                if (i == -1) { vstup.close(); return false; }
                c = (char)i;
                j = vstup.read();
                if (j == -1) { vstup.close(); return false; }
                d = (char)j;
                int dataType = Integer.decode(String.format("0x%c%c",c,d));
                if (dataType == 1) { vstup.close(); return true; }
                if (dataType != 0) { vstup.close(); return false; } // doesnt support other data types
                
                // data...
                for (int y = 0; y < byteCount; y++) {
                    i = vstup.read();
                    if (i == -1) { vstup.close(); return false; }
                    c = (char)i;
                    j = vstup.read();
                    if (j == -1) { vstup.close(); return false; }
                    d = (char)j;
                    short data = Short.decode(String.format("0x%c%c",c,d));
                    mem[address++][bank] = data;
                }
                // checksum - dont care..
                while((i = vstup.read()) != -1) if ((char)i == '\n' || i == 0x0D) break;
            }
            vstup.close();
        }  catch (java.io.FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            fireChange(-1);
            return false;
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            fireChange(-1);
            return false;
        }
        fireChange(-1);
        return true;
    }
    
    /**
     * Get starting address (load address) from HEX file
     */
//    public int getHexStartAddress(File file) {
  //      return 0;
   // }
    
    /**
     * Method loads a binary file into memory.
     * @param filename Binary file name (has to be readable and has to exist)
     * @param address an address where the file should be loaded.
     * @return true if file was successfully loaded, false if not.
     */
    public boolean loadBin(String filename, int address, int bank) {
        if (sizeSet == false) return false;
        lastImageStart = 0;
        lastStartSet = true;
        try {
            File f = new File(filename);
            if (f.isFile() == false)
                throw new IOException("Specified file name doesn't point to a file");
            RandomAccessFile vstup = new RandomAccessFile(f, "r");
            int i=0;
            long r=0, l = vstup.length();
            while(r < l) {
                i = vstup.readUnsignedByte();
                mem[address++][bank] = (short)(i & 0xFF);
                l++;
            }
            vstup.close();
        } catch(EOFException ex) {} 
        catch (java.io.FileNotFoundException ex) {
            StaticDialogs.showErrorMessage("File not found: " + filename);
            fireChange(-1);
            return false;
        }
        catch (Exception e) {
            StaticDialogs.showErrorMessage("Error opening file: " + filename);
            fireChange(-1);
            return false;
        }
        fireChange(-1);
        return true;
    }
    

    @Override
    public Object read(int from) {
        if (from < bankCommon) return mem[from][bankSelect];
        else return mem[from][0];
    }
    
    public Object read(int from, int bank) {
        if (from < bankCommon) return mem[from][bank];
        else return mem[from][0];
    }

    @Override
    public Object readWord(int from) {
        b = (from < bankCommon)?bankSelect:0;
        if (from == mem.length-1) return mem[from][b];
        int low = mem[from][b] & 0xFF;
        int high = mem[from+1][b];
        return (int)((high << 8)| low);
    }

    @Override
    public void write(int to, Object val) {
        if (isRom(to) == true) return;
        b = (to < bankCommon)?bankSelect:0;
        if (val instanceof Integer)
            mem[to][b] = (short)((Integer)val & 0xFF);
        else
            mem[to][b] = (short)((Short)val & 0xFF);
        fireChange(to);
    }

    public void write(int to, Object val,int bank) {
        if (isRom(to) == true) return;
        b = (to < bankCommon)?bank:0;
        if (val instanceof Integer)
            mem[to][b] = (short)((Integer)val & 0xFF);
        else
            mem[to][b] = (short)((Short)val & 0xFF);
        fireChange(to);
    }

    @Override
    public void writeWord(int to, Object val) {
        if (isRom(to) == true) return;
        b = (to < bankCommon)?bankSelect:0;
        short low = (short)((Integer)val & 0xFF);
        mem[to][b] = low;
        fireChange(to);
        if (to < mem.length-1) {
            short high = (short)(((Integer)val >>> 8) & 0xFF);
            mem[to+1][b] = high;
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

    public int getSize() {
        if (sizeSet == false) return 0;
        return mem.length;
    }

    public int getProgramStart() {
        return lastImageStart;
    }
    
    /* ROM */
    // merges all continuous ranges to one
    private void mergeRanges() {
        Vector<Integer> keys = new Vector<Integer>(romBitmap.keySet());
        Collections.sort(keys);
        
        Enumeration<Integer> e = keys.elements();
        if (e.hasMoreElements() == false) return;
        int key1 = (Integer)e.nextElement();
        int value1 = (Integer)romBitmap.get(key1);

        for (; e.hasMoreElements();) {
            int key2 = (Integer)e.nextElement();
            int value2 = (Integer)romBitmap.get(key2);
            if (value1 == key2+1) {
                // merge
                romBitmap.remove(key1);
                romBitmap.remove(key2);
                romBitmap.put(key1,value2);
                value1 = value2;
            } else {
                key1 = key2;
                value1 = value2;
            }
        }
    }
    
    private void removeRomRange(int from, int to) {
        for (Enumeration<Integer> e = romBitmap.keys(); e.hasMoreElements();) {
            int key = e.nextElement();
            int value = romBitmap.get(key);
            
            if ((key >= from) && (value <= to)) {
                romBitmap.remove(key);
                continue;
            }
            if ((key < from) && (value >= from) && (value <= to)) {
                romBitmap.remove(key);
                romBitmap.put(key,from-1);
                continue;
            }
            if ((key >= from) && (key <= to) && (value > to)) {
                romBitmap.remove(key);
                romBitmap.put(to+1,value);
                continue;
            }
            if ((key < from) && (value > to)) {
                romBitmap.remove(key);
                romBitmap.put(key,from-1);
                romBitmap.put(to+1,value);
            }
        }
        mergeRanges();
    }

    private void addRomRange(int from, int to) {
        removeRomRange(from, to);
        romBitmap.put(from,to);
    }
    
    // remove range from romBitmap
    @Override
    public void setRAM(int from, int to) {
        if (sizeSet == false) return;
        if (from > to) return;
        removeRomRange(from,to);
    }

    @Override
    public void setROM(int from, int to) {
        if (sizeSet == false) return;
        if (from > to) return;
        addRomRange(from, to);
    }

    @Override
    public boolean isRom(int address) {
        for (Enumeration<Integer> e = romBitmap.keys(); e.hasMoreElements();) {
            int key = e.nextElement();
            int value = romBitmap.get(key);
            if ((key <= address) && (address <= value)) return true;
        }
        return false;
    }
    
    // only for GUI purposes, this have nothing to do with memory emulation
    @Override
    public Hashtable<Integer,Integer> getROMRanges() {
        return romBitmap;
    }

    private void fireChange(int adr) {
        Object[] listeners = deviceList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==IMemListener.class) {
                ((IMemListener)listeners[i+1]).memChange(changeEvent, adr);
            }
        }
    }

	@Override
	public Class<?> getDataType() {
		return Short.class;
	}

	@Override
	public String getHash() {
		return "a93730cef0f15c6ea9d6b5e9e5d7f05f";
	}
    
}
