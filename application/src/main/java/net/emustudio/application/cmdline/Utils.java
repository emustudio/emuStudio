/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.application.cmdline;

import net.emustudio.application.ApplicationApiImpl;
import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.application.gui.ExtendedDialogs;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.gui.dialogs.OpenComputerDialog;
import net.emustudio.application.gui.dialogs.StudioFrame;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.InvalidPluginException;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextNotFoundException;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.InvalidContextException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    public static final long EMUSTUDIO_ID = UUID.randomUUID().toString().hashCode();

    public static ApplicationConfig loadApplicationConfig(boolean gui, boolean auto) throws IOException {
        Path configFile = Path.of("emuStudio.toml");
        if (Files.notExists(configFile)) {
            LOGGER.warn("No configuration file found; creating empty one");
            Files.createFile(configFile);
        }

        return ApplicationConfig.fromFile(configFile, !gui, auto);
    }

    public static VirtualComputer loadVirtualComputer(
        ApplicationConfig appConfig,
        ComputerConfig computerConfig,
        ExtendedDialogs dialogs,
        ContextPoolImpl contextPool,
        DebugTableModelImpl debugTableModel,
        ConfigFiles configFiles
    ) throws InvalidPluginException, IOException, PluginInitializationException {
        ApplicationApi applicationApi = new ApplicationApiImpl(debugTableModel, contextPool, dialogs);

        VirtualComputer computer = VirtualComputer.create(
            computerConfig, applicationApi, appConfig, configFiles
        );
        computer.initialize(contextPool);
        computer.reset();

        final int memorySize = computer.getMemory().map(Memory::getSize).orElse(0);
        computer.getCPU().ifPresent(cpu -> debugTableModel.setCPU(cpu, memorySize));
        return computer;
    }

    public static ComputerConfig loadComputerConfig(
        String configName,
        ApplicationConfig appConfig,
        ExtendedDialogs dialogs,
        ConfigFiles configFiles
    ) throws IOException {
        final AtomicReference<ComputerConfig> computerConfig = new AtomicReference<>();
        if ((configName == null || configName.isEmpty()) && dialogs.isGui()) {
            OpenComputerDialog dialog = new OpenComputerDialog(configFiles, appConfig, dialogs, computerConfig::set);
            dialogs.setParent(dialog);
            dialog.setVisible(true);
            dialogs.setParent(null);
        } else if (configName != null && !configName.isEmpty()) {
            Optional<ComputerConfig> optionalConfig = configFiles.loadConfiguration(configName);
            if (optionalConfig.isEmpty()) {
                System.err.println("Configuration '" + configName + "' does not exist");
                System.exit(1);
            }
            computerConfig.set(optionalConfig.get());
        }

        if (computerConfig.get() == null) {
            System.err.println("Virtual computer must be selected!");
            System.exit(1);
        }
        return computerConfig.get();
    }

    public static LoadingDialog showSplashScreen() {
        LoadingDialog splash = new LoadingDialog();
        splash.setVisible(true);
        return splash;
    }

    @SuppressWarnings("unchecked")
    public static void showMainWindow(VirtualComputer computer, ApplicationConfig applicationConfig, ExtendedDialogs dialogs,
                                      DebugTableModel debugTableModel, ContextPool contextPool, Optional<Path> inputFile) {
        MemoryContext<?> memoryContext = null;
        try {
            memoryContext = contextPool.getMemoryContext(EMUSTUDIO_ID, MemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            LOGGER.warn("No memory context is available", e);
        }

        StudioFrame mainWindow = new StudioFrame(
            computer, applicationConfig, dialogs, debugTableModel, memoryContext, inputFile
        );

        dialogs.setParent(mainWindow);
        mainWindow.setVisible(true);
    }
}
