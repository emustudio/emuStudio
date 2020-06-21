/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.application;

import net.emustudio.application.api.ApplicationApiImpl;
import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.configuration.ComputerConfig;
import net.emustudio.application.configuration.ConfigFiles;
import net.emustudio.application.emulation.Automation;
import net.emustudio.application.gui.ExtendedDialogs;
import net.emustudio.application.gui.GuiDialogsImpl;
import net.emustudio.application.gui.NoGuiDialogsImpl;
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
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class Runner {
    private static final Logger LOGGER = LoggerFactory.getLogger(Runner.class);
    private static final long emustudioId = UUID.randomUUID().toString().hashCode();

    public static void main(String[] args) {
        ExtendedDialogs dialogs = new NoGuiDialogsImpl();
        try {
            CommandLine commandLine = CommandLine.parse(args);

            setupLookAndFeel();

            Path configFile = Path.of("emuStudio.toml");
            if (Files.notExists(configFile)) {
                Files.createFile(configFile);
            }
            ApplicationConfig applicationConfig = ApplicationConfig.fromFile(
                configFile, commandLine.isNoGUI(), commandLine.isAuto()
            );

            if (!commandLine.isNoGUI()) {
                dialogs = new GuiDialogsImpl();
            }
            ConfigFiles configFiles = new ConfigFiles();

            final AtomicReference<ComputerConfig> computerConfig = new AtomicReference<>();
            if (commandLine.getConfigFileName().isEmpty() && !commandLine.isNoGUI()) {
                OpenComputerDialog dialog = new OpenComputerDialog(configFiles, applicationConfig, dialogs, computerConfig::set);
                dialogs.setParent(dialog);
                dialog.setVisible(true);
                dialogs.setParent(null);
            } else {
                computerConfig.set(commandLine.getConfigFileName().map(configFiles::loadConfiguration).orElseThrow());
            }

            if (computerConfig.get() == null) {
                System.err.println("Virtual computer must be selected!");
                System.exit(1);
            }

            Optional<LoadingDialog> splash = showSplashScreen(
                commandLine.isNoGUI(), commandLine.getConfigFileName().map(Path::toString).orElse("")
            );

            ContextPoolImpl contextPool = new ContextPoolImpl(emustudioId);
            DebugTableModelImpl debugTableModel = new DebugTableModelImpl();
            ApplicationApi applicationApi = new ApplicationApiImpl(debugTableModel, contextPool, dialogs);

            VirtualComputer computer = VirtualComputer.create(
                computerConfig.get(), applicationApi, applicationConfig, configFiles
            );
            computer.initialize(contextPool);
            computer.reset();

            final int memorySize = computer.getMemory().map(Memory::getSize).orElse(0);
            computer.getCPU().ifPresent(cpu -> debugTableModel.setCPU(cpu, memorySize));

            splash.ifPresent(Window::dispose);

            if (commandLine.isAuto()) {
                System.exit(runAutomation(
                    computer, commandLine.getInputFileName(), applicationConfig, dialogs, commandLine.getWaitForFinishMillis()
                ));
            } else if (!commandLine.isNoGUI()) {
                showMainWindow(
                    computer, applicationConfig, dialogs, debugTableModel, contextPool, commandLine.getInputFileName()
                );
            } else {
                System.err.println("No GUI is available; and no automatic emulation was set either. Exiting.");
            }
        } catch (CmdLineException | IOException | NoSuchElementException | InvalidPluginException | PluginInitializationException e) {
            LOGGER.error("Could not start emuStudio", e);
            dialogs.showError("Could not start emuStudio. Please see log file for more details", "emuStudio");
            System.exit(1);
        }
    }

    private static int runAutomation(VirtualComputer computer, String inputFileName, ApplicationConfig applicationConfig,
                                     Dialogs dialogs, int waitForFinishMillis) {
        try {
            new Automation(computer, inputFileName, applicationConfig, dialogs, waitForFinishMillis).run();
            return 0;
        } catch (Exception e) {
            LOGGER.error("Unexpected error during automation.", e);
            dialogs.showError("Unexpected error during automation. Please see log file for details.");
            return 1;
        } finally {
            computer.close();
        }
    }

    @SuppressWarnings("unchecked")
    private static void showMainWindow(VirtualComputer computer, ApplicationConfig applicationConfig, ExtendedDialogs dialogs,
                                       DebugTableModel debugTableModel, ContextPool contextPool, String inputFileName) {
        MemoryContext<?> memoryContext = null;
        try {
            memoryContext = contextPool.getMemoryContext(emustudioId, MemoryContext.class);
        } catch (ContextNotFoundException | InvalidContextException e) {
            LOGGER.warn("No memory context is available", e);
        }

        StudioFrame mainWindow = (inputFileName != null)
            ? new StudioFrame(computer, applicationConfig, dialogs, debugTableModel, memoryContext, inputFileName)
            : new StudioFrame(computer, applicationConfig, dialogs, debugTableModel, memoryContext);

        dialogs.setParent(mainWindow);
        mainWindow.setVisible(true);
    }

    private static Optional<LoadingDialog> showSplashScreen(boolean noGUI, String computerName) {
        LoadingDialog splash = null;
        if (!noGUI) {
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else {
            LOGGER.info("Loading virtual computer: {}", computerName);
        }
        return Optional.ofNullable(splash);
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                LOGGER.warn("Unable to set system look and feel", e);
            }
        }

        UIManager.put("Button.background", UIManager.get("Panel.background"));
        UIManager.put("Button.font", Constants.FONT_COMMON);
        UIManager.put("Button.opaque", true);

        UIManager.put("CheckBox.font", Constants.FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.font", Constants.FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", Constants.FONT_COMMON);

        UIManager.put("ColorChooser.font", Constants.FONT_COMMON);
        UIManager.put("ComboBox.font", Constants.FONT_COMMON);

        UIManager.put("TabbedPane.selected", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.background", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentAreaColor", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentOpaque", true);
        UIManager.put("TabbedPane.opaque", true);
        UIManager.put("TabbedPane.tabsOpaque", true);
        UIManager.put("TabbedPane.font", Constants.FONT_TITLE_BORDER);
        UIManager.put("TabbedPane.smallFont", Constants.FONT_COMMON);

        UIManager.put("EditorPane.font", Constants.FONT_MONOSPACED);
        UIManager.put("FormattedTextField.font", Constants.FONT_COMMON);
        UIManager.put("IconButton.font", Constants.FONT_COMMON);

        UIManager.put("InternalFrame.optionDialogTitleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.paletteTitleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.titleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.opaque", true);

        UIManager.put("Label.font", Constants.FONT_COMMON);
        UIManager.put("Label.opaque", true);

        UIManager.put("List.font", Constants.FONT_MONOSPACED);
        UIManager.put("List.rendererUseUIBorder", true);
        UIManager.put("List.focusCellHighlightBorder", null);

        UIManager.put("Menu.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("Menu.font", Constants.FONT_COMMON);

        UIManager.put("MenuBar.font", Constants.FONT_COMMON);

        UIManager.put("MenuItem.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("MenuItem.font", Constants.FONT_COMMON);

        UIManager.put("OptionPane.buttonFont", Constants.FONT_COMMON);
        UIManager.put("OptionPane.font", Constants.FONT_COMMON);
        UIManager.put("OptionPane.messageFont", Constants.FONT_COMMON);

        UIManager.put("Panel.font", Constants.FONT_COMMON);
        UIManager.put("Panel.opaque", true);

        UIManager.put("PasswordField.font", Constants.FONT_COMMON);
        UIManager.put("PopupMenu.font", Constants.FONT_COMMON);
        UIManager.put("ProgressBar.font", Constants.FONT_COMMON);
        UIManager.put("RadioButton.font", Constants.FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.font", Constants.FONT_COMMON);
        UIManager.put("ScrollPane.font", Constants.FONT_COMMON);
        UIManager.put("Slider.font", Constants.FONT_COMMON);
        UIManager.put("Spinner.font", Constants.FONT_COMMON);

        UIManager.put("Table.font", Constants.FONT_COMMON);
        UIManager.put("Table.focusCellHighlightBorder", null);

        UIManager.put("TableHeader.font", Constants.FONT_TITLE_BORDER);

        UIManager.put("TextArea.font", Constants.FONT_MONOSPACED);
        UIManager.put("TextField.font", Constants.FONT_MONOSPACED);
        UIManager.put("TextPane.font", Constants.FONT_MONOSPACED);
        UIManager.put("TitledBorder.font", Constants.FONT_TITLE_BORDER);
        UIManager.put("ToggleButton.font", Constants.FONT_COMMON);
        UIManager.put("ToolBar.font", Constants.FONT_COMMON);
        UIManager.put("ToolTip.font", Constants.FONT_COMMON);
        UIManager.put("Tree.font", Constants.FONT_COMMON);
        UIManager.put("Viewport.font", Constants.FONT_COMMON);
    }
}
