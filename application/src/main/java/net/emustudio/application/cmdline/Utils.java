/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.ApplicationApiImpl;
import net.emustudio.application.gui.framework.DialogsGui;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.gui.dialogs.OpenComputerDialog;
import net.emustudio.application.gui.dialogs.StudioFrame;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.InvalidPluginException;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
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
import java.util.function.Supplier;

public class Utils {
    public static final long EMUSTUDIO_ID = UUID.randomUUID().toString().hashCode();
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static AppSettings loadAppSettings(boolean gui, boolean auto) throws IOException {
        Path configFile = Path.of("emuStudio.toml");
        if (Files.notExists(configFile)) {
            LOGGER.warn("No configuration file found; creating empty one");
            Files.createFile(configFile);
        }

        return AppSettings.fromFile(configFile, !gui, auto);
    }

    public static VirtualComputer loadComputer(
            AppSettings appConfig,
            ComputerConfig computerConfig,
            Dialogs dialogs,
            ContextPoolImpl contextPool,
            DebugTableModelImpl debugTableModel,
            GUI gui
    ) throws InvalidPluginException, IOException, PluginInitializationException {
        ApplicationApi applicationApi = new ApplicationApiImpl(debugTableModel, contextPool, dialogs, gui);

        VirtualComputer computer = VirtualComputer.create(computerConfig, applicationApi, appConfig);
        computer.initialize(contextPool);
        computer.reset();

        Optional<Supplier<Integer>> memory = computer.getMemory().map(m -> m::getSize);
        computer.getCPU().ifPresent(cpu -> debugTableModel.setCPU(cpu, memory.orElse(() -> 0)));
        return computer;
    }

    public static Optional<ComputerConfig> loadComputerConfigFromGui(
            AppSettings appSettings, DialogsGui dialogs, EmuStudioGui gui
    ) {
        final AtomicReference<ComputerConfig> computerConfig = new AtomicReference<>();
        OpenComputerDialog dialog = new OpenComputerDialog(appSettings, dialogs, computerConfig::set, gui);
        dialogs.setParent(dialog);
        dialog.setVisible(true);
        dialogs.setParent(null);
        return Optional.ofNullable(computerConfig.get());
    }

    public static LoadingDialog showSplashScreen(GUI gui) {
        LoadingDialog splash = new LoadingDialog(gui);
        splash.setVisible(true);
        return splash;
    }

    @SuppressWarnings("unchecked")
    public static void showMainWindow(VirtualComputer computer, AppSettings appSettings, DialogsGui dialogs,
                                      DebugTableModel debugTableModel, ContextPool contextPool, Path inputFile, EmuStudioGui gui) {
        MemoryContext<?> memoryContext = null;
        try {
            memoryContext = contextPool.getMemoryContext(EMUSTUDIO_ID, MemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            LOGGER.warn("No memory context is available", e);
        }

        StudioFrame mainWindow = new StudioFrame(
                computer, appSettings, dialogs, debugTableModel, memoryContext, inputFile, gui
        );

        dialogs.setParent(mainWindow);
        mainWindow.setVisible(true);
    }
}
