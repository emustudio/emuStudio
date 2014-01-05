/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2013, Peter Jakubčo
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

import emulib.runtime.InvalidPasswordException;
import emulib.runtime.InvalidPluginException;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.*;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import emustudio.main.CommandLineFactory.CommandLine;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main class of the emuStudio platform.
 *
 * @author vbmacher
 */
public class Main {

    private final static Logger logger = LoggerFactory.getLogger(Main.class);
    /**
     * Loaded computer.
     */
    public static ArchitectureManager architecture = null;
    /**
     * emuStudio password for emuLib identification security mechanism.
     */
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
            logger.error("Unable to set system look and feel", e);
        }

        // parse command line arguments
        try {
            commandLine = CommandLineFactory.parseCommandLine(args);
        } catch (InvalidCommandLineException e) {
            tryShowErrorMessage("Invalid command line: " + e.getMessage());
            logger.error("Invalid command line.", e);
            return;
        }

        // Test if java_cup is loaded
        try {
            java_cup.runtime.Scanner d;
        } catch (NoClassDefFoundError e) {
            logger.error("java_cup library not loaded");
            tryShowErrorMessage("Error: java_cup library not loaded.");
            return;
        }

        try {
            password = emulib.runtime.ContextPool.SHA1(String.valueOf(Math.random()) + new Date().toString());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            logger.error("Could not compute hash.");
            tryShowErrorMessage("Error: Could not compute hash");
            return;
        }
        if (!emulib.emustudio.API.assignPassword(password)) {
            logger.error("Communication with emuLib failed.");
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

        // if configuration name has not been specified, let user
        // to choose the configuration manually
        if (commandLine.getConfigName() == null) {
            if (commandLine.noGUIWanted()) {
                tryShowErrorMessage("Configuration was not specified.");
                logger.error("Configuration was not specified.");
                System.exit(0);
            }
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK()) {
                commandLine.setConfigName(odi.getArchName());
            }
            if (commandLine.getConfigName() == null) {
                logger.error("Configuration was not specified.");
                System.exit(0);
            }
        }

        LoadingDialog splash = null;
        if (!commandLine.noGUIWanted()) {
            // display splash screen, while loading the virtual computer
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else {
            logger.info(new StringBuilder().append("Loading virtual computer: ").append(commandLine.getConfigName())
                    .toString());
        }

        // load the virtual computer
        try {
            architecture = ArchitectureLoader.getInstance().createArchitecture(commandLine.getConfigName());
        } catch (InvalidPasswordException e) {
            logger.error("Wrong emuLib.", e);
            tryShowErrorMessage("Wrong emuLib. Please see log file for details.");
        } catch (InvalidPluginException e) {
            logger.error("Could not load plugin.", e);
            tryShowErrorMessage("Could not load plugin. Please see log file for details.");
        } catch (PluginLoadingException e) {
            logger.error("Could not load virtual computer.", e);
            tryShowErrorMessage("Could not load virtual computer. Please see log file for details.");
        } catch (ReadConfigurationException e) {
            logger.error("Could not read configuration.", e);
            tryShowErrorMessage("Error: Could not read configuration. Please see log file for details.");
        } catch (PluginInitializationException e) {
            logger.error("Could not initialize plug-ins.", e);
            tryShowErrorMessage("Error: Could not initialize plug-ins. Please see log file for details.");
        } catch (IOException e) {
            logger.error("Could not load virtual computer. Unexpected error.", e);
            tryShowErrorMessage("Could not load virtual computer. Please see log file for details.");
        }

        if (splash != null) {
            // hide splash screen
            splash.dispose();
        }

        if (architecture == null) {
            System.exit(1);
        }

        if (!commandLine.autoWanted()) {
            try {
                // if the automatization is turned off, start the emuStudio normally
                if (commandLine.getInputFileName() != null) {
                    new StudioFrame(commandLine.getInputFileName(), commandLine.getConfigName()).setVisible(true);
                } else {
                    new StudioFrame(commandLine.getConfigName()).setVisible(true);
                }
            } catch (Exception e) {
                logger.error("Could not start main window.", e);
                tryShowErrorMessage("Could not start main window.");
                System.exit(1);
            }
        } else {
            try {
              new Automatization(architecture, commandLine.getInputFileName(), commandLine.noGUIWanted()).run();
            } catch (AutomatizationException e) {
                logger.error("Error during automatization.", e);
                tryShowErrorMessage("Error: " + e.getMessage());
                System.exit(1);
            }
            architecture.destroy();
            System.exit(0);
        }
    }
}
