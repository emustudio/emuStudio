/*
 * Memory.java
 *
 * Created on Sobota, 2007, okt�ber 27, 11:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package memImpl;

import gui.frmMemory;
import java.io.File;
import java.util.Collections;
import java.util.Vector;
import plugins.ISettingsHandler;
import plugins.ISettingsHandler.pluginType;
import plugins.memory.IMemory;
import plugins.memory.IMemoryContext;


/**
 *
 * @author vbmacher
 */
public class Memory implements IMemory {
    private MemoryContext memContext;
    private frmMemory memGUI;
    private ISettingsHandler settings;

    public String getDescription() {
        return "Operating memory for most CPUs. This plugin supports banking" +
                " controllable via SMemoryContext class.";
    }

    public String getVersion() { return "0.27b"; }

    public String getName() {
        return "Standard linear-byte operating memory with variable size";
    }

    public String getCopyright() {
        return "\u00A9 Copyright 2006-2008, Peter Jakubčo";
    }

    /** Creates a new instance of Memory */
    public Memory() {
        memContext = new MemoryContext();
    }

    public void showGUI() {
        if (memGUI == null) memGUI = new frmMemory(this,settings);
        memGUI.setVisible(true);
    }

    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
    }

    public IMemoryContext getContext() {
        return memContext;
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
    public void initialize(int size, ISettingsHandler sHandler) {
        String s = sHandler.readSetting(pluginType.memory, null, "banksCount");
        int bCount = 0, bCommon = 0;        
        if (s != null) { try { bCount = Integer.parseInt(s); } catch(Exception e) {} }
        s = sHandler.readSetting(pluginType.memory, null, "commonBoundary");
        if (s != null) { try { bCommon = Integer.parseInt(s); } catch(Exception e) {} }        
        memContext.init(size, bCount,bCommon);
        this.settings = sHandler;
    
        // load images
        int i = 0, adr = 0;
        String r = null;
        while (true) {
            s = sHandler.readSetting(pluginType.memory, null, "imageName" + i);
            r = sHandler.readSetting(pluginType.memory, null, "imageAddress" + i);
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
            s = sHandler.readSetting(pluginType.memory, null, "ROMfrom" + i);
            r = sHandler.readSetting(pluginType.memory, null, "ROMto" + i);
            if ((s == null) || (r == null)) break;            
            try { j = Integer.parseInt(s); } catch(Exception e) { break; }
            try { k = Integer.parseInt(r); } catch(Exception e) { break; }
            memContext.setROM(j, k);
            i++;
        }
    }

    /**
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to
     * tab0 in frmSettings.
     */
    public void saveSettings0(int banksCount, int commonBoundary, 
            Vector<String> imageFullNames, Vector<Integer> imageAddresses) {
        settings.writeSetting(pluginType.memory, null, "banksCount", String.valueOf(banksCount));
        settings.writeSetting(pluginType.memory, null, "commonBoundary", String.valueOf(commonBoundary));

        int i = 0;
        while (true) {
            if (settings.readSetting(pluginType.memory, null, "imageName"+i) != null) {
                settings.removeSetting(pluginType.memory, null, "imageName"+i);
                settings.removeSetting(pluginType.memory, null, "imageAddress"+i);
            } else break;
        }
        for (i = 0; i < imageFullNames.size(); i++)  {
            settings.writeSetting(pluginType.memory, null, "imageName"+i, imageFullNames.get(i));
            settings.writeSetting(pluginType.memory, null, "imageAddress"+i, String.valueOf(imageAddresses.get(i)));
        }
    }

    /**
     * Save only ROM ranges to load after start of the emulator. These
     * settings correspond to tab1 in frmSettings. ROM ranges are taken
     * directly from memory context.
     */
    public void saveSettings1() {
        Vector keys = new Vector(memContext.getROMRanges().keySet());
        Collections.sort(keys);
        Object[] ar = keys.toArray();

        for (int i = 0; i < ar.length; i++) {
            settings.writeSetting(pluginType.memory, null, "ROMfrom" + i, String.valueOf(ar[i]));
            settings.writeSetting(pluginType.memory, null, "ROMto" + i, 
                    String.valueOf(memContext.getROMRanges().get(ar[i])));
        }
    }
    
    /**
     * Clear memory? no.. not
     */
    public void reset() {}

    public void setProgramStart(int address) {
        memContext.lastImageStart = address;
    }

}
