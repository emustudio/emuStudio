/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
import emulib.plugins.memory.AbstractMemory;
import emulib.plugins.memory.MemoryContext;
import emulib.runtime.ContextPool;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.AlreadyRegisteredException;
import emulib.runtime.exceptions.InvalidContextException;
import emulib.runtime.exceptions.PluginInitializationException;
import net.sf.emustudio.memory.standard.StandardMemoryContext;
import net.sf.emustudio.memory.standard.StandardMemoryContext.AddressRange;
import net.sf.emustudio.memory.standard.gui.MemoryDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@PluginType(
        type=PLUGIN_TYPE.MEMORY,
        title="Standard operating memory",
        copyright="\u00A9 Copyright 2006-2017, Peter Jakubčo",
        description="Operating memory suitable for most of CPUs"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context = new MemoryContextImpl();

    private SettingsManager settings;
    private MemoryDialog gui;
    private boolean emuStudioNoGUI = false;

    public MemoryImpl(Long pluginID, ContextPool contextPool) {
        super(pluginID);
        try {
            contextPool.register(pluginID, context, StandardMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (AlreadyRegisteredException | InvalidContextException e) {
            LOGGER.error("Could not register memory", e);
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
        if (this.gui != null) {
            gui.dispose();
            this.gui = null;
        }
        context.destroy();
    }

    @Override
    public int getSize() {
        return context.getSize();
    }

    /*
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
        this.settings = Objects.requireNonNull(settings);

        int banksCount = 0, bankCommon = 0, memorySize = MemoryContextImpl.DEFAULT_MEM_SIZE;
        String tmpSetting = settings.readSetting(pluginID, "banksCount");
        try {
            if (tmpSetting != null) {
                banksCount = Integer.decode(tmpSetting);
                if (banksCount < 0) {
                    throw new PluginInitializationException("Banks count must be >= 0");
                }
            }
        } catch (NumberFormatException e) {
            throw new PluginInitializationException(this, "Could not parse banks count", e);
        }

        tmpSetting = settings.readSetting(pluginID, "commonBoundary");
        try {
            if (tmpSetting != null) {
                bankCommon = Integer.decode(tmpSetting);
                if (bankCommon < 0) {
                    throw new PluginInitializationException("Common boundary must be >= 0");
                }
            }
        } catch (NumberFormatException e) {
            throw new PluginInitializationException(this, "Could not parse common boundary", e);
        }

        if (banksCount == 0) {
            banksCount = 1;
        }

        tmpSetting = settings.readSetting(pluginID, "memorySize");
        if (tmpSetting != null) {
            try {
                memorySize = Integer.decode(tmpSetting);
                if (memorySize <= 0) {
                    throw new PluginInitializationException(this, "Memory size must be > 0");
                }
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(this, "Could not parse memory size", e);
            }
        }

        context.init(memorySize, banksCount, bankCommon);

        emuStudioNoGUI = Boolean.parseBoolean(settings.readSetting(pluginID, SettingsManager.NO_GUI));
        if (!emuStudioNoGUI) {
            gui = new MemoryDialog(pluginID, this, context, settings);
        }

        loadImages(settings);
        loadRomRanges(settings);
    }

    private void loadRomRanges(SettingsManager settings) throws PluginInitializationException {
        String romFrom;
        String romTo;

        int i = 0;
        try {
            for (; ; i++) {
                romFrom = settings.readSetting(pluginID, "ROMfrom" + i);
                romTo = settings.readSetting(pluginID, "ROMto" + i);
                if ((romFrom == null) || (romTo == null)) {
                    break;
                }
                context.setROM(new RangeTree.Range(Integer.decode(romFrom), Integer.decode(romTo)));
            }
        } catch (NumberFormatException e) {
            throw new PluginInitializationException(this, "Could not parse ROM range", e);
        }
    }

    private void loadImages(SettingsManager settings) throws PluginInitializationException {
        String imageName;
        String imageAddress;
        int i = 0;
        for (;; i++) {
            imageName = settings.readSetting(pluginID, "imageName" + i);
            imageAddress = settings.readSetting(pluginID, "imageAddress" + i);
            if (imageName == null) {
                break;
            }
            int address = 0;
            if (imageAddress != null) {
                try {
                    address = Integer.decode(imageAddress);
                } catch (NumberFormatException e) {
                    throw new PluginInitializationException(
                            this, "Could not parse address at which the image should be loaded", e
                    );
                }
            }
            loadImage(imageName, address);
        }
    }
    
    public void loadImage(String fileName, int address) {
        if (fileName.toUpperCase().endsWith(".HEX")) {
            context.loadHex(fileName, 0);
        } else {
            context.loadBin(fileName, address, 0);
        }
    }

    /*
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to tab0 in frmSettings.
     */
    public void saveCoreSettings(int banksCount, int commonBoundary,
            List<String> imageFullNames, List<Integer> imageAddresses) {
        settings.writeSetting(pluginID, "banksCount", String.valueOf(banksCount));
        settings.writeSetting(pluginID, "commonBoundary", String.valueOf(commonBoundary));

        int i = 0;
        for (; settings.readSetting(pluginID, "imageName" + i) != null; i++) {
            settings.removeSetting(pluginID, "imageName" + i);
            settings.removeSetting(pluginID, "imageAddress" + i);
        }
        for (i = 0; i < imageFullNames.size(); i++) {
            settings.writeSetting(pluginID, "imageName" + i, imageFullNames.get(i));
            settings.writeSetting(pluginID, "imageAddress" + i, String.valueOf(imageAddresses.get(i)));
        }
    }

    /*
     * Save only ROM ranges to load after start of the emulator. These
     * settings correspond to tab1 in frmSettings. ROM ranges are taken
     * directly from memory context.
     */
    public void saveROMRanges() {
        int i = 0;
        while (settings.readSetting(pluginID, "ROMfrom" + i) != null) {
            settings.removeSetting(pluginID, "ROMfrom" + i);
            settings.removeSetting(pluginID, "ROMto" + i);
            i++;
        }
        
        i = 0;
        for (AddressRange range : context.getROMRanges()) {
            settings.writeSetting(pluginID, "ROMfrom" + i, String.valueOf(range.getStartAddress()));
            settings.writeSetting(pluginID, "ROMto" + i, String.valueOf(range.getStopAddress()));
            i++;
        }
    }

    @Override
    public void setProgramStart(int address) {
        super.setProgramStart(address);
        context.lastImageStart = address;
    }

    @Override
    public void showSettings() {
        if (gui != null) {
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !emuStudioNoGUI;
    }
}
