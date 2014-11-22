/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2007-2014, Peter Jakubčo
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

import emulib.plugins.PluginInitializationException;
import emulib.runtime.ContextPool;
import emulib.runtime.InvalidPasswordException;
import emulib.runtime.InvalidPluginException;
import emulib.runtime.PluginLoader;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.Computer;
import emustudio.architecture.ComputerFactory;
import emustudio.architecture.Configuration;
import emustudio.architecture.ConfigurationFactory;
import emustudio.architecture.ReadConfigurationException;
import emustudio.architecture.SettingsManagerImpl;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import emustudio.main.CommandLineFactory.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class Main {
    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static String password = null;
    public static CommandLine commandLine;

    public static void tryShowMessage(String message) {
        if (commandLine != null && !commandLine.noGUIWanted()) {
            StaticDialogs.showMessage(message);
        } else {
            System.out.println(message);
        }
    }

    public static void tryShowMessage(String message, String title) {
        if (commandLine != null && !commandLine.noGUIWanted()) {
            StaticDialogs.showMessage(message, title);
        } else {
            System.out.println("[" + title + "] " + message);
        }
    }

    public static void tryShowErrorMessage(String message) {
        if (commandLine != null && !commandLine.noGUIWanted()) {
            StaticDialogs.showErrorMessage(message);
        } else {
            System.out.println("Error: " + message);
        }
    }

    public static void tryShowErrorMessage(String message, String title) {
        if (commandLine != null && !commandLine.noGUIWanted()) {
            StaticDialogs.showErrorMessage(message, title);
        } else {
            System.out.println("[" + title + "] Error: " + message);
        }
    }

    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (javax.swing.UnsupportedLookAndFeelException | ClassNotFoundException
                | InstantiationException | IllegalAccessException e) {
            LOGGER.error("Unable to set system look and feel", e);
        }

        // parse command line arguments
        try {
            commandLine = CommandLineFactory.parseCommandLine(args);
        } catch (InvalidCommandLineException e) {
            tryShowErrorMessage("Invalid command line: " + e.getMessage());
            LOGGER.error("Invalid command line.", e);
            return;
        }

        try {
            password = emulib.runtime.ContextPool.SHA1(String.valueOf(Math.random()) + new Date().toString());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            LOGGER.error("Could not compute hash.");
            tryShowErrorMessage("Error: Could not compute hash");
            return;
        }
        if (!emulib.emustudio.API.assignPassword(password)) {
            LOGGER.error("Communication with emuLib failed.");
            tryShowErrorMessage("Error: communication with emuLib failed.");
            return;
        }

        if (commandLine.helpWanted()) {
            // only show help and EXIT (ignore other arguments)
            System.out.println("emuStudio will accept the following command line"
                    + " parameters:\n"
                    + "\n--config name : load configuration with file name"
                    + "\n--input name  : use the source code given by the file name"
                    + "\n--output name : output compiler messages into this file name"
                    + "\n--auto        : run the emulation automatization"
                    + "\n--nogui       : try to not show GUI in automatization"
                    + "\n--help        : output this message");
            return;
        }

        ConfigurationFactory configurationManager = new ConfigurationFactory();
        ContextPool contextPool = new ContextPool();
        PluginLoader pluginLoader = new PluginLoader();
        Computer computer = null;
        Configuration configuration = null;
        SettingsManagerImpl settingsManager = null;

        // if configuration name has not been specified, let user
        // to choose the configuration manually
        if (commandLine.getConfigName() == null) {
            if (commandLine.noGUIWanted()) {
                tryShowErrorMessage("Configuration was not specified.");
                LOGGER.error("Configuration was not specified.");
                System.exit(0);
            }
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK()) {
                commandLine.setConfigName(odi.getArchName());
            }
            if (commandLine.getConfigName() == null) {
                LOGGER.error("Configuration was not specified.");
                System.exit(0);
            }
        }

        LoadingDialog splash = null;
        if (!commandLine.noGUIWanted()) {
            // display splash screen, while loading the virtual computer
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else {
            LOGGER.info("Loading virtual computer: {}", commandLine.getConfigName());
        }

        // load the virtual computer
        try {
            computer = new ComputerFactory(configurationManager, pluginLoader)
                    .createComputer(commandLine.getConfigName(), contextPool);
            contextPool.setComputer(password, computer);

            configuration = ConfigurationFactory.read(commandLine.getConfigName());
            settingsManager = new SettingsManagerImpl(computer, configuration);

            computer.initialize(settingsManager);
        } catch (InvalidPluginException e) {
            LOGGER.error("Could not load plugin.", e);
            tryShowErrorMessage("Could not load plugin. Please see log file for details.");
        } catch (ReadConfigurationException e) {
            LOGGER.error("Could not read configuration.", e);
            tryShowErrorMessage("Error: Could not read configuration. Please see log file for details.");
        } catch (PluginInitializationException e) {
            LOGGER.error("Could not initialize plugins.", e);
            tryShowErrorMessage("Error: Could not initialize plugins. Please see log file for details.");
            computer = null;
        } catch (InvalidPasswordException e) {
            LOGGER.error("Could not initialize emuLib.", e);
            tryShowErrorMessage("Error: Could not initialize emuLib. Please see log file for details.");
            computer = null;
        }

        if (splash != null) {
            // hide splash screen
            splash.dispose();
        }

        if (computer == null || settingsManager == null || configuration == null) {
            System.exit(1);
        }

        if (!commandLine.autoWanted()) {
            try {
                // if the automatization is turned off, start the emuStudio normally
                if (commandLine.getInputFileName() != null) {
                    new StudioFrame(contextPool, computer, commandLine.getInputFileName(), settingsManager).setVisible(true);
                } else {
                    new StudioFrame(contextPool, computer, settingsManager).setVisible(true);
                }
            } catch (Exception e) {
                LOGGER.error("Could not start main window.", e);
                tryShowErrorMessage("Could not start main window.");
                System.exit(1);
            }
        } else {
            try {
              new Automatization(settingsManager, computer, commandLine.getInputFileName(), commandLine.noGUIWanted()).run();
            } catch (AutomatizationException e) {
                LOGGER.error("Error during automatization.", e);
                tryShowErrorMessage("Error: " + e.getMessage());
                System.exit(1);
            }
            computer.destroy();
            System.exit(0);
        }
    }
}
