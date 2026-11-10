/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.ApplicationApiImpl;
import net.emustudio.application.gui.framework.DialogsGui;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.gui.dialogs.OpenComputerDialog;
import net.emustudio.application.gui.dialogs.StudioFrame;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.InvalidPluginException;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.PluginInitializationException;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.GUI;
import net.emustudio.emulib.application.ContextNotFoundException;
import net.emustudio.emulib.application.ContextPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

public class Utils {
    public static final long EMUSTUDIO_ID = 0L;
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public static VirtualComputer loadComputer(
            AppSettings appConfig,
            ComputerConfig computerConfig,
            Dialogs dialogs,
            ContextPoolImpl contextPool,
            GUI gui
    ) throws InvalidPluginException, IOException, PluginInitializationException {
        ApplicationApi applicationApi = new ApplicationApiImpl(contextPool, dialogs, gui);

        VirtualComputer computer = VirtualComputer.create(computerConfig, applicationApi, appConfig);
        try {
            computer.initialize(contextPool);
            computer.reset();
            return computer;
        } catch (PluginInitializationException | RuntimeException | Error failure) {
            try {
                computer.close();
            } catch (Throwable closeError) {
                if (closeError != failure) failure.addSuppressed(closeError);
            }
            throw failure;
        }
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

    public static void showMainWindow(VirtualComputer computer, AppSettings appSettings, DialogsGui dialogs,
                                      ContextPool contextPool, Path inputFile, EmuStudioGui gui)
            throws InterruptedException, InvocationTargetException {
        MemoryContext<?> memoryContext = null;
        try {
            memoryContext = contextPool.get(EMUSTUDIO_ID, MemoryContext.class);
        } catch (ContextNotFoundException e) {
            LOGGER.warn("No memory context is available", e);
        }

        MemoryContext<?> windowMemory = memoryContext;
        Runnable showWindow = () -> {
            StudioFrame mainWindow = new StudioFrame(
                    computer, appSettings, dialogs, windowMemory, inputFile, gui
            );

            dialogs.setParent(mainWindow);
            mainWindow.setVisible(true);
        };
        if (SwingUtilities.isEventDispatchThread()) {
            showWindow.run();
        } else {
            SwingUtilities.invokeAndWait(showWindow);
        }
    }
}
