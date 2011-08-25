/*
 * Memory.java
 *
 * Created on Sobota, 2007, okt�ber 27, 11:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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
package standard_mem;

import interfaces.C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8;
import java.io.File;
import java.util.Collections;
import java.util.Vector;

import standard_mem.gui.frmMemory;
import emuLib8.plugins.ISettingsHandler;
import emuLib8.plugins.memory.IMemoryContext;
import emuLib8.plugins.memory.SimpleMemory;
import emuLib8.runtime.Context;
import emuLib8.runtime.StaticDialogs;

/**
 *
 * @author vbmacher
 */
public class Memory extends SimpleMemory {

    private MemoryContext memContext;
    private frmMemory memGUI;
    private boolean noGUI = false; // whether to support GUI

    private final static int DEFAULT_MEM_SIZE = 65536;

    /** Creates a new instance of Memory */
    public Memory(Long pluginID) {
        super(pluginID);
        memContext = new MemoryContext();
        if (!(Context.getInstance().register(pluginID, memContext,
                C6E60458DB9B6FE7ADE74FC77C927621AD757FBA8.class)
                && Context.getInstance().register(pluginID, memContext,
                IMemoryContext.class))) {
            StaticDialogs.showMessage("Error: Could not register the memory");
        }
    }

    @Override
    public String getDescription() {
        return "Operating memory for most CPUs. Every cell is one byte long."
                + "This plugin supports banking"
                + " controllable via context.";
    }

    @Override
    public String getVersion() {
        return "0.29b";
    }

    @Override
    public String getTitle() {
        return "Standard Operating memory";
    }

    @Override
    public String getCopyright() {
        return "\u00A9 Copyright 2006-2010, P. Jakubčo";
    }

    @Override
    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
        memContext.destroy();
        memContext = null;
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
    public boolean initialize(ISettingsHandler settings) {
        super.initialize(settings);
        String s = settings.readSetting(pluginID, "banksCount");
        int bCount = 0, bCommon = 0;
        if (s != null) {
            try {
                bCount = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        s = settings.readSetting(pluginID, "commonBoundary");
        if (s != null) {
            try {
                bCommon = Integer.parseInt(s);
            } catch (Exception e) {
            }
        }
        this.settings = settings;
        memContext.init(DEFAULT_MEM_SIZE, bCount, bCommon, memGUI);
        noGUI = Boolean.parseBoolean(settings.readSetting(pluginID, "nogui"));
        if (!noGUI)
            memGUI = new frmMemory(pluginID, this, memContext, settings);

        // load images
        int i = 0, adr = 0;
        String r = null;
        while (true) {
            s = settings.readSetting(pluginID, "imageName" + i);
            r = settings.readSetting(pluginID, "imageAddress" + i);
            if (s == null) {
                break;
            }
            if (new File(s).getName().toUpperCase().endsWith(".HEX")) {
                memContext.loadHex(s, 0);
            } else {
                if (r != null) {
                    try {
                        adr = Integer.decode(r);
                    } catch (Exception e) {
                    }
                }
                memContext.loadBin(s, adr, 0);
            }
            i++;
        }

        // load rom ranges
        i = 0;
        int j, k;
        while (true) {
            s = settings.readSetting(pluginID, "ROMfrom" + i);
            r = settings.readSetting(pluginID, "ROMto" + i);
            if ((s == null) || (r == null)) {
                break;
            }
            try {
                j = Integer.parseInt(s);
            } catch (Exception e) {
                break;
            }
            try {
                k = Integer.parseInt(r);
            } catch (Exception e) {
                break;
            }
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
        settings.writeSetting(pluginID, "banksCount", String.valueOf(banksCount));
        settings.writeSetting(pluginID, "commonBoundary", String.valueOf(commonBoundary));

        int i = 0;
        while (true) {
            if (settings.readSetting(pluginID, "imageName" + i) != null) {
                settings.removeSetting(pluginID, "imageName" + i);
                settings.removeSetting(pluginID, "imageAddress" + i);
            } else {
                break;
            }
        }
        for (i = 0; i < imageFullNames.size(); i++) {
            settings.writeSetting(pluginID, "imageName" + i, imageFullNames.get(i));
            settings.writeSetting(pluginID, "imageAddress" + i, String.valueOf(imageAddresses.get(i)));
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
        while (settings.readSetting(pluginID, "ROMfrom" + i) != null) {
            settings.removeSetting(pluginID, "ROMfrom" + i);
            settings.removeSetting(pluginID, "ROMto" + i);
            i++;
        }

        for (i = 0; i < ar.length; i++) {
            settings.writeSetting(pluginID, "ROMfrom" + i, String.valueOf(ar[i]));
            settings.writeSetting(pluginID, "ROMto" + i,
                    String.valueOf(memContext.getROMRanges().get(ar[i])));
        }
    }

    @Override
    public void setProgramStart(int address) {
        super.setProgramStart(address);
        memContext.lastImageStart = address;
    }

    @Override
    public void showSettings() {
        if (memGUI != null) {
            memGUI.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return true;
    }
}
