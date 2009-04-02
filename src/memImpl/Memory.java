/*
 * Memory.java
 *
 * Created on Sobota, 2007, okt�ber 27, 11:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package memImpl;

import java.io.File;
import java.util.Collections;
import java.util.Vector;

import memImpl.gui.frmMemory;
import plugins.ISettingsHandler;
import plugins.memory.IMemory;
import plugins.memory.IMemoryContext;


/**
 *
 * @author vbmacher
 */
public class Memory implements IMemory {
    private MemoryContext memContext;
    private long hash;
    private frmMemory memGUI;
    private ISettingsHandler settings;

    @Override
    public String getDescription() {
        return "Operating memory for most CPUs. Every cell is one byte long." +
        		"This plugin supports banking" +
                " controllable via context.";
    }

    @Override
    public String getVersion() { return "0.28b"; }
    
    @Override
    public long getHash() { return hash; }

    @Override
    public String getTitle() {
        return "Operating memory";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2006-2009, P. Jakubčo";
    }

    /** Creates a new instance of Memory */
    public Memory(Long hash) {
    	this.hash = hash;
        memContext = new MemoryContext();
    }

    @Override
    public void showGUI() {
        if (memGUI != null) memGUI.setVisible(true);
    }

    @Override
    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
    }

    @Override
    public IMemoryContext getContext() {
        return memContext;
    }

    @Override
    public int getProgramStart() {
        return memContext.getProgramStart();
    }
    
    @Override
    public int getSize() {
    	return memContext.getSize();
    }

    /**
     * Initialize memory:
     *     1. load settings as: banks count, common boundary
     *     2. create memory context (create memory with loaded settings)
     *     3. load images from settings
     *     4. load these images into memory in order as they appear in config file
     *     5. load rom ranges from settings
     *     6. set rom ranges to memory
     */
    @Override
    public boolean initialize(int size, ISettingsHandler sHandler) {
        String s = sHandler.readSetting(hash, "banksCount");
        int bCount = 0, bCommon = 0;        
        if (s != null) { try { bCount = Integer.parseInt(s); } catch(Exception e) {} }
        s = sHandler.readSetting(hash, "commonBoundary");
        if (s != null) { try { bCommon = Integer.parseInt(s); } catch(Exception e) {} }        
        this.settings = sHandler;
        memContext.init(size, bCount,bCommon,memGUI);
        memGUI = new frmMemory(this,settings);
    
        // load images
        int i = 0, adr = 0;
        String r = null;
        while (true) {
            s = sHandler.readSetting(hash, "imageName" + i);
            r = sHandler.readSetting(hash, "imageAddress" + i);
            if (s == null) break;
            if (new File(s).getName().toUpperCase().endsWith(".HEX"))
                memContext.loadHex(s, 0);
            else {
                if (r != null) try { adr = Integer.decode(r); } catch(Exception e) {}
                memContext.loadBin(s, adr, 0);
            }
            i++;
        }

        // load rom ranges
        i = 0; int j,k;
        while (true) {
            s = sHandler.readSetting(hash, "ROMfrom" + i);
            r = sHandler.readSetting(hash, "ROMto" + i);
            if ((s == null) || (r == null)) break;            
            try { j = Integer.parseInt(s); } catch(Exception e) { break; }
            try { k = Integer.parseInt(r); } catch(Exception e) { break; }
            memContext.setROM(j, k);
            i++;
        }
        return true;
    }

    /**
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to
     * tab0 in frmSettings.
     */
    public void saveSettings0(int banksCount, int commonBoundary, 
            Vector<String> imageFullNames, Vector<Integer> imageAddresses) {
        settings.writeSetting(hash, "banksCount", String.valueOf(banksCount));
        settings.writeSetting(hash, "commonBoundary", String.valueOf(commonBoundary));

        int i = 0;
        while (true) {
            if (settings.readSetting(hash, "imageName"+i) != null) {
                settings.removeSetting(hash, "imageName"+i);
                settings.removeSetting(hash, "imageAddress"+i);
            } else break;
        }
        for (i = 0; i < imageFullNames.size(); i++)  {
            settings.writeSetting(hash, "imageName"+i, imageFullNames.get(i));
            settings.writeSetting(hash, "imageAddress"+i, String.valueOf(imageAddresses.get(i)));
        }
    }

    /**
     * Save only ROM ranges to load after start of the emulator. These
     * settings correspond to tab1 in frmSettings. ROM ranges are taken
     * directly from memory context.
     */
    public void saveSettings1() {
        Vector<Integer> keys = new Vector<Integer>(memContext.getROMRanges().keySet());
        Collections.sort(keys);
        Object[] ar = keys.toArray();

        int i = 0;
        while (settings.readSetting(hash, "ROMfrom"+i) != null) {
            settings.removeSetting(hash, "ROMfrom"+i);
            settings.removeSetting(hash, "ROMto"+i);
            i++;
        }
        
        for (i = 0; i < ar.length; i++) {
            settings.writeSetting(hash, "ROMfrom" + i, String.valueOf(ar[i]));
            settings.writeSetting(hash, "ROMto" + i, 
                    String.valueOf(memContext.getROMRanges().get(ar[i])));
        }
    }
    
    /**
     * Clear memory? no ..not.
     */
    @Override
    public void reset() { }

    @Override
    public void setProgramStart(int address) {
        memContext.lastImageStart = address;
    }

	@Override
	public void showSettings() {
		// TODO Auto-generated method stub
		
	}

}
