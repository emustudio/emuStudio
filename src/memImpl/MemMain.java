/*
 * MemMain.java
 *
 * Created on Sobota, 2007, okt�ber 27, 11:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package memImpl;

import plugins.memory.*;

import java.util.*;
import javax.swing.event.*;
import java.io.*;

/**
 *
 * @author vbmacher
 */
public class MemMain implements IMemory {
    private short[] mem;
    private boolean sizeSet;
    private int lastImageStart = 0;
    private boolean lastStartSet = false;
    
    private frmMemory memGUI;
    
    // this table contains ROM parts of memory
    private Hashtable romBitmap; // keys: low boundary (limit); values: upper boundary

    /* list of devices that wants to get annoucement about memory changes */
    private EventListenerList deviceList;
    private EventObject changeEvent;

    public static void showErrorMessage(String message) {
        javax.swing.JOptionPane.showMessageDialog(null,
                message,"Error",javax.swing.JOptionPane.ERROR_MESSAGE);
    }

    public String getDescription() {
        return "Operating memory for most 8 bit CPUs. This is very simple "
                + "implementation of non-banked memory type.";
    }

    public String getVersion() { return "0.11b"; }

    public String getName() {
        return "Standard non-banked operating memory";
    }

    public String getCopyright() {
        return "\u00A9 Copyright 2006-2007, Peter Jakubčo";
    }

    /** Creates a new instance of MemMain */
    public MemMain() {
        sizeSet = false;
        romBitmap = new Hashtable();
        changeEvent = new EventObject(this);
        deviceList = new EventListenerList(); //new DeviceList(changeEvent);
    }

    public boolean init(int count) {
        if (sizeSet == true) return false;
        mem = new short[count];
        sizeSet = true;
        return true;
    }

    public int getSize() {
        if (sizeSet == false) return 0;
        return mem.length;
    }

    public void clear() {
        if (sizeSet == false) return;
        for (int i = 0; i < mem.length; mem[i] = 0, i++);
        lastImageStart = 0;
        fireChange(-1);
    }
    
    // this can parse classic old data
    // line beginning with ; is ignored
    public boolean loadHex(String filename) {
        if (sizeSet == false) return false;
        lastStartSet = false;
        ArrayList a = new ArrayList();
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
                    mem[address++] = data;
                }
                // checksum - dont care..
                while((i = vstup.read()) != -1) if ((char)i == '\n' || i == 0x0D) break;
            }
            vstup.close();
        }  catch (java.io.FileNotFoundException ex) {
            showErrorMessage("File not found: " + filename);
            fireChange(-1);
            return false;
        }
        catch (Exception e) {
            showErrorMessage("Error opening file: " + filename);
            fireChange(-1);
            return false;
        }
        fireChange(-1);
        return true;
    }
    
    public boolean loadBin(String filename, int address) {
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
                mem[address++] = (short)(i & 0xFF);
                l++;
            }
            vstup.close();
        } catch(EOFException ex) {} 
        catch (java.io.FileNotFoundException ex) {
            showErrorMessage("File not found: " + filename);
            fireChange(-1);
            return false;
        }
        catch (Exception e) {
            showErrorMessage("Error opening file: " + filename);
            fireChange(-1);
            return false;
        }
        fireChange(-1);
        return true;
    }
    
    public boolean bankingSupported() { return false; }
    public void setBanksCount(short count) {}
    public short getBanksCount() { return 0; /*bankCount;*/ }

    public void setActiveBank(short bank) throws IndexOutOfBoundsException {}
    public short getActiveBank() { return 0; /*activeBank;*/ }

    public int getBankCapacity() { return getSize(); /*bankCapacity;*/ }

    // merges all continuous ranges to one
    private void mergeRanges() {
        Vector keys = new Vector(romBitmap.keySet());
        Collections.sort(keys);
        
        Enumeration e = keys.elements();
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
        for (Enumeration e = romBitmap.keys(); e.hasMoreElements();) {
            int key = (Integer)e.nextElement();
            int value = (Integer)romBitmap.get(key);
            
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
    public void setRAM(int from, int to) {
        if (sizeSet == false) return;
        if (from > to) return;
        removeRomRange(from,to);
    }

    public void setROM(int from, int to) {
        if (sizeSet == false) return;
        if (from > to) return;
        addRomRange(from, to);
    }

    public boolean isRom(int address) {
        for (Enumeration e = romBitmap.keys(); e.hasMoreElements();) {
            int key = (Integer)e.nextElement();
            int value = (Integer)romBitmap.get(key);
            if ((key <= address) && (address <= value)) return true;
        }
        return false;
    }
    
    // only for GUI purposes, this have nothing to do with memory emulation
    public Hashtable getROMRanges() {
        return romBitmap;
    }
    
    public void write8(int adr, short by) {
        if (isRom(adr) == true) return;
        mem[adr] = (short)(by & 0xFF);
        fireChange(adr);
    }

    public short read8(int adr) {
        return (short)(mem[adr] & 0xFF);
    }

    public void write16(int adr, int wo) {
        if (isRom(adr) == true) return;
        short low = (short)(wo & 0xFF);
        mem[adr] = low;
        fireChange(adr);
        if (adr < mem.length-1) {
            short high = (short)((wo >>> 8) & 0xFF);
            mem[adr+1] = high;
            fireChange(adr+1);
        }
    }

    public int read16(int adr) {
        if (adr == mem.length-1) return mem[adr];
        int low = mem[adr] & 0xFF;
        int high = mem[adr+1];
        return (int)((high << 8)| low);
    }

    public void registerDeviceDMA(IMemory.IMemListener listener) {
        deviceList.add(IMemListener.class, listener);
    }

    public void unregisterDeviceDMA(IMemory.IMemListener listener) {
        deviceList.remove(IMemListener.class, listener);
    }
    
    private void fireChange(int adr) {
        Object[] listeners = deviceList.getListenerList();
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==IMemListener.class) {
                ((IMemListener)listeners[i+1]).memChange(changeEvent, adr, 0);
            }
        }
    }

    public void showGUI() {
        if (memGUI == null) memGUI = new frmMemory(this);
        memGUI.setVisible(true);
    }

    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
    }

    public int getLastImageStart() {
        return lastImageStart;
    }

}
