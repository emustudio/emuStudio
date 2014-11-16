/*
 * Created on Sobota, 2007, october 27, 11:58
 *
 * Copyright (C) 2007-2014 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.memory.standard.impl;

import emulib.annotations.PLUGIN_TYPE;
import emulib.annotations.PluginType;
import emulib.emustudio.SettingsManager;
import emulib.plugins.PluginInitializationException;
import emulib.plugins.memory.AbstractMemory;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.AlreadyRegisteredException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidContextException;
import emulib.runtime.StaticDialogs;
import net.sf.emustudio.memory.standard.StandardMemoryContext;
import net.sf.emustudio.memory.standard.StandardMemoryContext.AddressRange;
import net.sf.emustudio.memory.standard.gui.MemoryFrame;
import net.sf.emustudio.memory.standard.impl.MemoryContextImpl.AddressRangeImpl;

import java.io.File;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(type=PLUGIN_TYPE.MEMORY,
        title="Standard operating memory",
        copyright="\u00A9 Copyright 2006-2014, Peter Jakubčo",
        description="Operating memory suitable for most of CPUs")
public class MemoryImpl extends AbstractMemory {
    private MemoryContextImpl context;
    private MemoryFrame memGUI;
    private boolean noGUI = false; // whether to support GUI
    private final ContextPool contextPool;

    private final static int DEFAULT_MEM_SIZE = 65536;

    public MemoryImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        this.contextPool = Objects.requireNonNull(contextPool);
        context = new MemoryContextImpl();
        try {
            contextPool.register(pluginID, context, StandardMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            StaticDialogs.showErrorMessage("Could not register the memory",
                    MemoryImpl.class.getAnnotation(PluginType.class).title());
        }
    }

    @Override
    public String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.emustudio.memory.standard.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }

    @Override
    public void destroy() {
        if (this.memGUI != null) {
            memGUI.dispose();
            this.memGUI = null;
        }
        context.destroy();
        context = null;
    }

    @Override
    public int getSize() {
        return context.getSize();
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
    public void initialize(SettingsManager settings) throws PluginInitializationException {
        super.initialize(settings);
        String s = settings.readSetting(pluginID, "banksCount");
        int bCount = 0, bCommon = 0;
        if (s != null) {
            try {
                bCount = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(
                        this, "Could not parse banks count"
                );
            }
        }
        s = settings.readSetting(pluginID, "commonBoundary");
        if (s != null) {
            try {
                bCommon = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(
                        this, "Could not parse common boundary"
                );
            }
        }
        this.settings = settings;
        context.init(DEFAULT_MEM_SIZE, bCount, bCommon, memGUI);
        noGUI = Boolean.parseBoolean(settings.readSetting(pluginID, SettingsManager.NO_GUI));
        if (!noGUI) {
            memGUI = new MemoryFrame(pluginID, this, context, settings);
        }

        // load images
        int i = 0, adr = 0;
        String r;
        while (true) {
            s = settings.readSetting(pluginID, "imageName" + i);
            r = settings.readSetting(pluginID, "imageAddress" + i);
            if (s == null) {
                break;
            }
            if (new File(s).getName().toUpperCase().endsWith(".HEX")) {
                context.loadHex(s, 0);
            } else {
                if (r != null) {
                    try {
                        adr = Integer.decode(r);
                    } catch (NumberFormatException e) {
                        throw new PluginInitializationException(
                                this,
                                "Could not parse address at which the image"
                                        + " should be loaded"
                        );
                    }
                }
                context.loadBin(s, adr, 0);
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
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(
                        this, "Could not parse ROM from (" + s + ")"
                );
            }
            try {
                k = Integer.parseInt(r);
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(
                        this, "Could not parse ROM to (" + r + ")"
                );
            }
            context.setROM(new AddressRangeImpl(j, k));
            i++;
        }
    }

    /**
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to
     * tab0 in frmSettings.
     */
    public void saveCoreSettings(int banksCount, int commonBoundary,
            List<String> imageFullNames, List<Integer> imageAddresses) {
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
    public void saveROMRanges() {
        int i = 0;
        for (AddressRange range : context.getROMRanges()) {
            settings.removeSetting(pluginID, "ROMfrom" + i);
            settings.removeSetting(pluginID, "ROMto" + i);
            settings.writeSetting(pluginID, "ROMfrom" + i, String.valueOf(range.getStartAddress()));
            settings.writeSetting(pluginID, "ROMto" + i, String.valueOf(range.getStopAddress()));
        }
    }

    @Override
    public void setProgramStart(int address) {
        super.setProgramStart(address);
        context.lastImageStart = address;
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
