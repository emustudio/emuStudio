/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.memory.AbstractMemory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextAlreadyRegisteredException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import net.emustudio.plugins.memory.bytemem.gui.MemoryGui;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "Byte-cell based operating memory"
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractMemory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false);
        this.context = new MemoryContextImpl(applicationApi.getDialogs());
        try {
            ContextPool contextPool = applicationApi.getContextPool();
            contextPool.register(pluginID, context, ByteMemoryContext.class);
            contextPool.register(pluginID, context, MemoryContext.class);
        } catch (InvalidContextException | ContextAlreadyRegisteredException e) {
            LOGGER.error("Could not register memory context", e);
            applicationApi.getDialogs().showError(
                    "Could not register memory. Please see log file for more details", getTitle()
            );
        }
    }

    @Override
    public String getVersion() {
        return getResourceBundle().map(b -> b.getString("version")).orElse("(unknown)");
    }

    @Override
    public String getCopyright() {
        return getResourceBundle().map(b -> b.getString("copyright")).orElse("(unknown)");
    }

    @Override
    public String getDescription() {
        return "Operating memory suitable for most of modern CPUs. One memory cell is a byte.";
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

    @Override
    public void initialize() throws PluginInitializationException {
        try {
            int banksCount = settings.getInt("banksCount", 1);
            if (banksCount <= 0) {
                LOGGER.warn("Banks count <= 0. Resetting to 1");
                banksCount = 1;
            }
            int bankCommon = settings.getInt("commonBoundary", 0);
            if (bankCommon < 0) {
                LOGGER.warn("Common boundary < 0. Resetting to 0");
                bankCommon = 0;
            }

            int memorySize = settings.getInt("memorySize", MemoryContextImpl.DEFAULT_MEM_SIZE);
            if (memorySize < 0) {
                LOGGER.warn("Memory size < 0. Resetting to 0");
            }

            context.init(memorySize, banksCount, bankCommon);
        } catch (NumberFormatException e) {
            throw new PluginInitializationException(this, "Could not parse settings: Invalid number format ", e);
        }

        loadImages();
        loadRomRanges();
    }

    private void loadRomRanges() throws PluginInitializationException {
        try {
            for (int i = 0; ; i++) {
                Optional<Integer> from = settings.getInt("ROMfrom" + i);
                Optional<Integer> to = settings.getInt("ROMto" + i);

                if (from.isPresent() && to.isPresent()) {
                    RangeTree.Range range = new RangeTree.Range(from.get(), to.get());
                    context.setReadOnly(range);
                } else {
                    break;
                }
            }
        } catch (NumberFormatException e) {
            throw new PluginInitializationException(this, "Could not parse ROM range", e);
        }
    }

    private void loadImages() throws PluginInitializationException {
        for (int i = 0; ; i++) {
            try {
                Optional<Path> imageName = settings.getString("imageName" + i).map(Path::of);
                Optional<Integer> imageAddress = settings.getInt("imageAddress" + i);
                Optional<Integer> imageBank = settings.getInt("imageBank" + i);

                if (imageName.isPresent() && imageAddress.isPresent()) {
                    loadImage(imageName.get(), imageAddress.get(), imageBank.orElse(0));
                } else {
                    break;
                }
            } catch (NumberFormatException e) {
                throw new PluginInitializationException(this, "Could not parse image address", e);
            }
        }
    }

    public void loadImage(Path imagePath, int address, int bank) {
        if (imagePath.toString().toLowerCase().endsWith(".hex")) {
            context.loadHex(imagePath, bank);
        } else {
            context.loadBin(imagePath, address, bank);
        }
    }

    /*
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to tab0 in frmSettings.
     */
    public void saveCoreSettings(int banksCount, int commonBoundary, List<String> imageFullNames,
                                 List<Integer> imageAddresses, List<Integer> imageBanks) {

        settings.setInt("banksCount", banksCount);
        settings.setInt("commonBoundary", commonBoundary);

        for (int i = 0; settings.contains("imageName" + i); i++) {
            settings.remove("imageName" + i);
            settings.remove("imageAddress" + i);
        }
        for (int i = 0; i < imageFullNames.size(); i++) {
            settings.setString("imageName" + i, imageFullNames.get(i));
            settings.setInt("imageAddress" + i, imageAddresses.get(i));
            settings.setInt("imageBank" + i, imageBanks.get(i));
        }
    }

    /*
     * Save only ROM ranges to load after start of the emulator. These
     * settings correspond to tab1 in frmSettings. ROM ranges are taken
     * directly from memory context.
     */
    public void saveROMRanges() {
        for (int i = 0; settings.contains("ROMfrom" + i); i++) {
            settings.remove("ROMfrom" + i);
            settings.remove("ROMto" + i);
        }

        int i = 0;
        for (ByteMemoryContext.AddressRange range : context.getReadOnly()) {
            settings.setInt("ROMfrom" + i, range.getStartAddress());
            settings.setInt("ROMto" + i, range.getStopAddress());
            i++;
        }
    }

    @Override
    public void setProgramLocation(int location) {
        super.setProgramLocation(location);
        context.lastImageStart = location;
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new MemoryGui(parent, this, context, settings, applicationApi.getDialogs());
            }
            gui.setVisible(true);
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !guiNotSupported;
    }

    private Optional<ResourceBundle> getResourceBundle() {
        try {
            return Optional.of(ResourceBundle.getBundle("net.emustudio.plugins.memory.bytemem.version"));
        } catch (MissingResourceException e) {
            return Optional.empty();
        }
    }
}
