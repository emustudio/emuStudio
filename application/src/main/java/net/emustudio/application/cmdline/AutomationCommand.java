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

import net.emustudio.application.settings.ApplicationConfig;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.emulation.Automation;
import net.emustudio.application.gui.ExtendedDialogs;
import net.emustudio.application.gui.GuiDialogsImpl;
import net.emustudio.application.gui.NoGuiDialogsImpl;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.util.Optional;

import static net.emustudio.application.cmdline.Utils.*;
import static net.emustudio.application.gui.GuiUtils.setupLookAndFeel;

@SuppressWarnings("unused")
@CommandLine.Command(name = "automation", aliases = {"auto"}, description = "run emulation automation")
public class AutomationCommand implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("automation");

    @CommandLine.ParentCommand
    private Runner runner;

    @CommandLine.Option(names = {"-w", "--waitmax"}, paramLabel = "MILLIS", description = "limit emulation time to max MILLIS (force kill afterwards)")
    private int waitForFinishMillis = Automation.DONT_WAIT;

    @CommandLine.Option(names = "--gui", negatable = true, description = "show/don't show GUI during automation")
    private boolean gui;

    @CommandLine.Option(names = {"-s", "--start-address"}, description = "program start address", paramLabel = "ADDRESS")
    private String programStart = "0";


    @Override
    public void run() {
        ExtendedDialogs dialogs = new NoGuiDialogsImpl();
        try {
            ApplicationConfig appConfig = loadAppConfig(gui, true);
            if (gui) {
                setupLookAndFeel(appConfig);
                dialogs = new GuiDialogsImpl();
            }

            Optional<ComputerConfig> computerConfigOpt = (runner.exclusive != null) ?
                runner.exclusive.loadConfiguration() :
                (gui ? loadComputerConfigFromGui(appConfig, dialogs) : Optional.empty());

            if (computerConfigOpt.isEmpty()) {
                dialogs.showError("Virtual computer must be selected!");
                LOGGER.error("Virtual computer must be selected!");
                System.exit(1);
            }

            ComputerConfig computerConfig = computerConfigOpt.get();

            Optional<LoadingDialog> splash = gui ? Optional.of(showSplashScreen()) : Optional.empty();

            ContextPoolImpl contextPool = new ContextPoolImpl(EMUSTUDIO_ID);
            DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
            try(VirtualComputer computer = loadComputer(
                appConfig, computerConfig, dialogs, contextPool, debugTableModel
            )) {
                splash.ifPresent(Window::dispose);
                new Automation(
                    computer, runner.inputFile,
                    appConfig,
                    dialogs,
                    waitForFinishMillis,
                    RadixUtils.getInstance().parseRadix(programStart)
                ).run();
            }
            System.exit(0);
        } catch (Exception e) {
            LOGGER.error("Unexpected error during automation", e);
            dialogs.showError("Unexpected error during automation. Please see log file for details.");
            System.exit(1);
        }
    }
}
