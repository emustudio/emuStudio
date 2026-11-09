/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem;

import net.emustudio.emulib.plugin.cpu.CPUContext;

import net.emustudio.emulib.plugin.AbstractPlugin;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugin.annotations.PluginRoot;
import net.emustudio.emulib.plugin.memory.Memory;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import net.emustudio.plugins.memory.bytemem.gui.MemoryGui;
import net.emustudio.plugins.memory.bytemem.loaders.MemoryImageLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@PluginRoot(
        type = PLUGIN_TYPE.MEMORY,
        title = "Byte-cell based operating memory",
        description = "Operating memory suitable for most of modern CPUs. One memory cell is a byte."
)
@SuppressWarnings("unused")
public class MemoryImpl extends AbstractPlugin implements Memory {
    private final static Logger LOGGER = LoggerFactory.getLogger(MemoryImpl.class);

    private final MemoryContextImpl context;
    private final boolean guiNotSupported;
    private MemoryGui gui;

    public MemoryImpl(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
        super(pluginID, applicationApi, settings);

        this.context = new MemoryContextImpl();
        this.guiNotSupported = settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI).orElse(false);
        ContextPool contextPool = applicationApi.getContextPool();
        contextPool.register(pluginID, context, ByteMemoryContext.class);
        contextPool.register(pluginID, context, MemoryContext.class);
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
    public void initialize() throws PluginInitializationException {
        try {
            int banksCount = settings.getInt("banksCount").orElse(1);
            if (banksCount <= 0) {
                LOGGER.warn("Banks count <= 0. Resetting to 1");
                banksCount = 1;
            }
            int bankCommon = settings.getInt("commonBoundary").orElse(0);
            if (bankCommon < 0) {
                LOGGER.warn("Common boundary < 0. Resetting to 0");
                bankCommon = 0;
            }

            int memorySize = readMemorySize();
            if (memorySize < 0) {
                LOGGER.warn("Memory size < 0. Resetting to 0");
            }

            context.init(memorySize, banksCount, bankCommon);
        } catch (NumberFormatException e) {
            throw new PluginInitializationException("Could not parse settings: Invalid number format ", e);
        }

        loadImages();
        loadRomRanges();
    }

    private int readMemorySize() {
        for (String key : List.of("size", "memorySize")) {
            Optional<String> size = settings.getString(key);
            if (size.isPresent()) {
                return parseSizeString(size.get());
            }
            Optional<Integer> intSize = settings.getInt(key);
            if (intSize.isPresent()) {
                return intSize.get();
            }
        }
        return MemoryContextImpl.DEFAULT_MEM_SIZE;
    }

    static int parseSizeString(String size) {
        String value = size.trim();
        if (value.isEmpty()) {
            throw new NumberFormatException("Memory size cannot be empty");
        }

        long multiplier = 1;
        char suffix = value.charAt(value.length() - 1);
        if (suffix == 'K' || suffix == 'k') {
            multiplier = 1024;
            value = value.substring(0, value.length() - 1);
        } else if (suffix == 'M' || suffix == 'm') {
            multiplier = 1024 * 1024;
            value = value.substring(0, value.length() - 1);
        }

        try {
            long parsed = Math.multiplyExact(Long.parseLong(value), multiplier);
            if (parsed < 0 || parsed > Integer.MAX_VALUE) {
                throw new NumberFormatException("Memory size must be between 0 and " + Integer.MAX_VALUE + " bytes");
            }
            return (int) parsed;
        } catch (ArithmeticException e) {
            throw new NumberFormatException("Memory size is too large: " + size);
        }
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
            throw new PluginInitializationException("Could not parse ROM range", e);
        }
    }

    private void loadImages() {
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
                LOGGER.error("Could not parse image address or bank", e);
            } catch (FileNotFoundException e) {
                LOGGER.error("Could not load image {}", settings.getString("imageName" + i), e);
            } catch (Exception e) {
                LOGGER.error("Could not load image due to unknown reason", e);
            }
        }
    }

    public void loadImage(Path imagePath, int address, int bank) throws IOException {
        MemoryImageLoader.MemoryBank memoryBank = MemoryImageLoader.MemoryBank.of(bank, address);
        MemoryImageLoader.load(imagePath, context, memoryBank);
    }

    /*
     * Save only banks (count, common) and images to load
     * after start of the emulator. These settings correspond to tab0 in frmSettings.
     */
    public void saveCoreSettings(int banksCount, int commonBoundary, List<String> imageFullNames,
                                 List<Integer> imageAddresses, List<Integer> imageBanks) {

        settings.set("banksCount", banksCount);
        settings.set("commonBoundary", commonBoundary);

        for (int i = 0; settings.getString("imageName" + i).isPresent(); i++) {
            settings.set("imageName" + i, null);
            settings.set("imageAddress" + i, null);
        }
        for (int i = 0; i < imageFullNames.size(); i++) {
            settings.set("imageName" + i, imageFullNames.get(i));
            settings.set("imageAddress" + i, imageAddresses.get(i));
            settings.set("imageBank" + i, imageBanks.get(i));
        }
    }

    /*
     * Save only ROM ranges to load after start of the emulator. These
     * settings correspond to tab1 in frmSettings. ROM ranges are taken
     * directly from memory context.
     */
    public void saveROMRanges() {
        for (int i = 0; settings.getInt("ROMfrom" + i).isPresent(); i++) {
            settings.set("ROMfrom" + i, null);
            settings.set("ROMto" + i, null);
        }

        int i = 0;
        for (ByteMemoryContext.AddressRange range : context.getReadOnly()) {
            settings.set("ROMfrom" + i, range.getStartAddress());
            settings.set("ROMto" + i, range.getStopAddress());
            i++;
        }
    }

    @Override
    public void showSettings(JFrame parent) {
        if (!guiNotSupported) {
            if (gui == null) {
                gui = new MemoryGui(parent, this, context, settings, applicationApi.getDialogs(), applicationApi.getGUI(),
                        applicationApi.getAnnotations(), findCPUContext());
            }
            gui.setVisible(true);
        }
    }

    private CPUContext findCPUContext() {
        try {
            return applicationApi.getContextPool().get(pluginID, CPUContext.class);
        } catch (net.emustudio.emulib.application.ContextNotFoundException absent) {
            return null;
        }
    }

    @Override
    public boolean isShowSettingsSupported() {
        return !guiNotSupported;
    }
}
