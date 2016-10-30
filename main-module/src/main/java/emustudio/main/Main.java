/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
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
package emustudio.main;

import emulib.emustudio.API;
import emulib.plugins.cpu.CPU;
import emulib.plugins.memory.Memory;
import emulib.runtime.ContextPool;
import emulib.runtime.PluginLoader;
import emulib.runtime.StaticDialogs;
import emulib.runtime.exceptions.InvalidPasswordException;
import emustudio.Constants;
import emustudio.architecture.Computer;
import emustudio.architecture.SettingsManagerImpl;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import emustudio.gui.debugTable.DebugTableImpl;
import emustudio.gui.debugTable.DebugTableModel;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.UIManager;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Optional;

public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private static final String ERR_CONFIG_WAS_NOT_SPECIFIED = "Virtual computer configuration was not specified.";

    public static String emulibToken;
    public static CommandLine commandLine;

    public static void main(String[] args) throws MalformedURLException {
        setupLookAndFeel();

        try {
            commandLine = CommandLine.parse(args);
        } catch (CmdLineException e) {
            System.exit(1);
        }
        if (!setupEmuLibToken()) {
            System.exit(1);
        }

        Optional<String> computerName = determineComputerName();
        if (!computerName.isPresent()) {
            System.exit(1);
        }

        Optional<LoadingDialog> splash = showSplashScreen();

        ContextPool contextPool = new ContextPool();
        PluginLoader pluginLoader = new PluginLoader();

        Optional<Emulator> emulator = Emulator.load(computerName.get(), contextPool, pluginLoader);
        if (!emulator.isPresent()) {
            System.exit(1);
        }
        SettingsManagerImpl settingsManager = emulator.get().getSettingsManager();
        Computer computer = emulator.get().getComputer();

        if (splash.isPresent()) {
            splash.get().dispose();
        }

        if (!commandLine.isAuto()) {
            showMainWindow(contextPool, settingsManager, computer);
        } else {
            runAutomation(settingsManager, computer);
            System.exit(0);
        }
    }

    private static void runAutomation(SettingsManagerImpl settingsManager, Computer computer) {
        try {
          new Automatization(
                  settingsManager, computer, commandLine.getInputFileName(), commandLine.isNoGUI()
          ).run();
        } catch (AutomationException e) {
            LOGGER.error("Unexpected error during automation.", e);
            tryShowErrorMessage("Error: " + e.getMessage());
            System.exit(1);
        }
        computer.destroy();
    }

    private static void showMainWindow(ContextPool contextPool, SettingsManagerImpl settingsManager, Computer computer) {
        try {
            int memorySize = 0;

            Optional<Memory> memory = computer.getMemory();
            Optional<CPU> cpu = computer.getCPU();

            if (memory.isPresent()) {
                memorySize = memory.get().getSize();
            }

            DebugTableImpl debugTable = new DebugTableImpl(new DebugTableModel(cpu.get(), memorySize));
            API.getInstance().setDebugTable(debugTable, Main.emulibToken);

            String inputFileName = commandLine.getInputFileName();
            if (inputFileName != null) {
                new StudioFrame(contextPool, computer, inputFileName, settingsManager, debugTable).setVisible(true);
            } else {
                new StudioFrame(contextPool, computer, settingsManager, debugTable).setVisible(true);
            }
        } catch (InvalidPasswordException e) {
            LOGGER.error("Could not register debug table", e);
        } catch (Exception e) {
            LOGGER.error("Could not start main window.", e);
            tryShowErrorMessage("Could not start main window.");
            System.exit(1);
        }
    }

    private static Optional<LoadingDialog> showSplashScreen() {
        LoadingDialog splash = null;
        if (!commandLine.isNoGUI()) {
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else {
            LOGGER.info("Loading virtual computer: {}", commandLine.getConfigName());
        }
        return Optional.ofNullable(splash);
    }

    private static Optional<String> determineComputerName() {
        if (commandLine.getConfigName() != null) {
            return Optional.of(commandLine.getConfigName());
        } else if (commandLine.isNoGUI() || commandLine.isAuto()) {
            tryShowErrorMessage(ERR_CONFIG_WAS_NOT_SPECIFIED);
        } else {
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK()) {
                return Optional.ofNullable(odi.getArchName());
            }
        }
        return Optional.empty();
    }

    private static boolean setupEmuLibToken() {
        try {
            emulibToken = ContextPool.SHA1(String.valueOf(Math.random()) + new Date().toString());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            tryShowErrorMessage("Error: Could not compute hash");
            return false;
        }
        if (!API.assignPassword(emulibToken)) {
            tryShowErrorMessage("Error: communication with emuLib failed.");
            return false;
        }
        return true;
    }

    private static void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (javax.swing.UnsupportedLookAndFeelException | ClassNotFoundException
                | InstantiationException | IllegalAccessException e) {
            LOGGER.warn("Unable to set system look and feel", e);
        }
        UIManager.put("TabbedPane.selected", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.background", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentAreaColor", UIManager.get("Panel.background"));
        UIManager.put("TextPane.font", Constants.MONOSPACED_PLAIN_12);
        UIManager.put("TextArea.font", Constants.MONOSPACED_PLAIN_12);
        UIManager.put("List.font", Constants.MONOSPACED_PLAIN_12);
    }

    public static void tryShowMessage(String message) {
        if (commandLine != null && !commandLine.isNoGUI()) {
            StaticDialogs.showMessage(message);
        } else {
            LOGGER.info(message);
        }
    }

    public static void tryShowMessage(String message, String title) {
        if (commandLine != null && !commandLine.isNoGUI()) {
            StaticDialogs.showMessage(message, title);
        } else {
            LOGGER.info("[{}] {}", title, message);
        }
    }

    public static void tryShowErrorMessage(String message) {
        if (commandLine != null && !commandLine.isNoGUI()) {
            StaticDialogs.showErrorMessage(message);
        } else {
            LOGGER.error("Error: {}", message);
        }
    }

    public static void tryShowErrorMessage(String message, String title) {
        if (commandLine != null && !commandLine.isNoGUI()) {
            StaticDialogs.showErrorMessage(message, title);
        } else {
            LOGGER.error("[{}] Error: {}", title, message);
        }
    }

}
