/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import net.emustudio.application.emulation.Automation;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.framework.DialogsGui;
import net.emustudio.application.gui.framework.DialogsNoGui;
import net.emustudio.application.gui.debugtable.DebugTableModelImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.ContextPoolImpl;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.awt.*;
import java.util.Optional;

import static net.emustudio.application.cmdline.Utils.*;

@SuppressWarnings("unused")
@CommandLine.Command(name = "automation", aliases = {"auto"}, description = "run emulation automation")
public class AutomationCommand implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("automation");

    @CommandLine.ParentCommand
    private Runner runner;

    @CommandLine.Option(names = {"-w", "--waitmax"}, paramLabel = "MILLIS", description = "limit emulation time to max MILLIS (force kill afterwards)")
    private int waitForFinishMillis = Automation.DONT_WAIT;

    @CommandLine.Option(names = "--gui", negatable = true, defaultValue = "true", fallbackValue = "true",
            description = "show/don't show GUI during automation")
    private boolean gui;

    @CommandLine.Option(names = {"-p", "--program-location"}, description = "program start location", paramLabel = "LOCATION")
    private String programLocation = "-1";


    @Override
    public void run() {
        Dialogs dialogs = new DialogsNoGui();
        DialogsGui guiDialogs = null;
        try {
            AppSettings appConfig = loadAppSettings(this.gui, true);
            EmuStudioGui gui = null;
            if (this.gui) {
                gui = new EmuStudioGui();
                gui.initialize(appConfig);
                guiDialogs = new DialogsGui(gui);
                dialogs = guiDialogs;
            }

            Optional<ComputerConfig> computerConfigOpt = (runner.exclusive != null) ?
                    runner.exclusive.loadConfiguration() :
                    (this.gui ? loadComputerConfigFromGui(appConfig, guiDialogs, gui) : Optional.empty());

            if (computerConfigOpt.isEmpty()) {
                dialogs.showError("Virtual computer must be selected!");
                LOGGER.error("Virtual computer must be selected!");
                System.exit(1);
            }

            ComputerConfig computerConfig = computerConfigOpt.get();

            Optional<LoadingDialog> splash = this.gui ? Optional.of(showSplashScreen(gui)) : Optional.empty();

            ContextPoolImpl contextPool = new ContextPoolImpl(EMUSTUDIO_ID);
            DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
            try (VirtualComputer computer = loadComputer(
                    appConfig, computerConfig, dialogs, contextPool, debugTableModel, gui
            )) {
                Optional<Integer> programLocation = this.programLocation.equals("-1") ? Optional.empty() :
                        Optional.of(RadixUtils.getInstance().parseRadix(this.programLocation));

                Automation automation = new Automation(
                        computer, runner.inputFile,
                        appConfig,
                        dialogs,
                        waitForFinishMillis,
                        programLocation,
                        gui
                );
                splash.ifPresent(Window::dispose);
                automation.run();
            }
            if (!this.gui) {
                // Let GUI live!
                System.exit(0);
            }
        } catch (Exception e) {
            LOGGER.error("Unexpected error during automation", e);
            dialogs.showError("Unexpected error during automation. Please see log file for details.");
            System.exit(1);
        }
    }
}
