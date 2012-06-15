/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2012, Peter Jakubčo
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

import emulib.runtime.StaticDialogs;
import emustudio.architecture.*;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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
    public static ArchHandler currentArch = null;
    /**
     * emuStudio password for emuLib identification security mechanism.
     */
    public static String password = null;
    private static String inputFileName = null;
    private static String outputFileName = null;
    private static String configName = null;
    private static boolean auto = false;
    private static boolean checkHash = false;
    private static String classToHash = null;
    private static boolean help = false;
    private static boolean noGUI = false;

    public static void tryShowMessage(String message) {
        if (!noGUI) {
            StaticDialogs.showMessage(message);
        }
    }

    public static void tryShowMessage(String message, String title) {
        if (!noGUI) {
            StaticDialogs.showMessage(message, title);
        }
    }

    public static void tryShowErrorMessage(String message) {
        if (!noGUI) {
            StaticDialogs.showErrorMessage(message);
        }
    }

    public static void tryShowErrorMessage(String message, String title) {
        if (!noGUI) {
            StaticDialogs.showErrorMessage(message, title);
        }
    }

    /**
     * This method parsers the command line parameters. It sets internal class data members accordingly.
     *
     * @param args The command line arguments
     */
    private static void parseCommandLine(String[] args) {
        // process arguments
        int size = args.length;
        for (int i = 0; i < size; i++) {
            String arg = args[i].toUpperCase();
            try {
                if (arg.equals("--CONFIG")) {
                    i++;
                    // what configuration to load
                    if (configName != null) {
                        logger.warn(new StringBuilder().append("Config file already defined, ignoring this one: ").append(args[i]).toString());
                    } else {
                        configName = args[i];
                        logger.info(new StringBuilder().append("Loading virtual computer: ").append(configName).toString());
                    }
                } else if (arg.equals("--INPUT")) {
                    i++;
                    // what input file take to compiler
                    if (inputFileName != null) {
                        logger.warn(new StringBuilder().append("Input file already defined, ignoring this one: ").append(args[i]).toString());
                    } else {
                        inputFileName = args[i];
                        logger.info(new StringBuilder().append("Input file name for compiler: ").append(inputFileName).toString());
                    }
                } else if (arg.equals("--OUTPUT")) {
                    i++;
                    // what output file take for emuStudio messages during
                    // automation process. This option has a meaning
                    // only if the "-auto" option is set too.
                    if (outputFileName != null) {
                        logger.warn(new StringBuilder().append("Output file already defined, ignoring this one: ").append(args[i]).toString());
                    } else {
                        outputFileName = args[i];
                        logger.info(new StringBuilder().append("Output file name: ").append(outputFileName).toString());
                    }
                } else if (arg.equals("--AUTO")) {
                    auto = true;
                    logger.info("Turning automatization on.");
                } else if (arg.equals("--HASH")) {
                    i++;
                    checkHash = true;
                    if (classToHash != null) {
                        logger.warn(new StringBuilder().append("Class file already defined, ignoring this one: ").append(args[i]).toString());
                    } else {
                        classToHash = args[i];
                    }

                } else if (arg.equals("--HELP")) {
                    help = true;
                } else if (arg.equals("--NOGUI")) {
                    logger.info("Setting GUI off.");
                    noGUI = true;
                } else {
                    logger.error(new StringBuilder().append("Invalid command line argument (").append(arg).append(")").toString());
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                logger.error(new StringBuilder().append("[").append(arg).append("] Missing argument").toString());
            }
        }

    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (javax.swing.UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }

        // parse command line arguments
        parseCommandLine(args);

        // Test if java_cup is loaded
        try {
            java_cup.runtime.Scanner d;
        } catch (NoClassDefFoundError e) {
            logger.error("java_cup library not loaded");
            tryShowErrorMessage("Error: java_cup library not loaded.");
            return;
        }

        password = emulib.runtime.Context.SHA1(String.valueOf(Math.random())
                + new Date().toString());
        if (!emulib.runtime.Context.assignPassword(password)) {
            logger.error("Communication with emuLib failed.");
            tryShowErrorMessage("Error: communication with emuLib failed.");
            return;
        }

        if (help) {
            // only show help and EXIT (ignore other arguments)
            System.out.println("emuStudio will accept the following command line"
                    + " parameters:\n"
                    + "\n--config name : load configuration with file name"
                    + "\n--input name  : use the source code given by the file name"
                    + "\n--output name : output compiler messages into this file name"
                    + "\n--auto        : run the emulation automatization"
                    + "\n--nogui       : try to not show GUI in automatization"
                    + "\n--hash name   : compute hash for given class or interface name"
                    + "\n--help        : output this message");
            return;
        }

        if (checkHash) {
            // compute hash of a class and exit
            // Create a File object on the root of the directory
            // containing the class file
            File file = new File(System.getProperty("user.dir"));
            try {
                // Convert File to a URL
                URL url = file.toURI().toURL(); // file:/c:/class/
                URL[] urls = new URL[]{url};

                // Create a new class loader with the directory
                ClassLoader loader = new URLClassLoader(urls);

                // Load in the class; Class.childclass should be located in
                // the directory file:/c:/class/user/information
                Class cls = loader.loadClass(classToHash);

                // Prints the hash to the console
                System.out.println(emulib.runtime.Context.getInstance().computeHash(password, cls));
            } catch (MalformedURLException e) {
                logger.error("Could not compute hash.", e);
            } catch (ClassNotFoundException e) {
                logger.error("Given class is not found.", e);
            } catch (NullPointerException e) {
                logger.error("Class name is not specified.", e);
            }
            return;
        }

        // if configuration name has not been specified, let user
        // to choose the configuration manually
        if (configName == null) {
            if (noGUI) {
                logger.error("Configuration was not specified.");
                System.exit(0);
            }
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK()) {
                configName = odi.getArchName();
            }
            if (configName == null) {
                logger.error("Configuration was not specified.");
                System.exit(0);
            }
        }

        LoadingDialog splash = null;
        if (!noGUI) {
            // display splash screen, while loading the virtual computer
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else {
            logger.info(new StringBuilder().append("Loading virtual computer: ").append(configName).toString());
        }

        // load the virtual computer
        try {
            currentArch = ArchLoader.getInstance().loadComputer(configName, auto, noGUI);
        } catch (PluginLoadingException e) {
            currentArch = null;
            logger.error("Could not load virtual computer.", e);
            tryShowErrorMessage(new StringBuilder().append("Could not load virtual computer: ")
                    .append(e.getLocalizedMessage()).toString());
        } catch (ReadConfigurationException e) {
            logger.error("Could not read configuration.", e);
            tryShowErrorMessage(new StringBuilder().append("Error: Could not read configuration: ")
                    .append(e.getLocalizedMessage()).toString());
        } catch (PluginInitializationException e) {
            logger.error("Could not initialize plug-ins.", e);
            tryShowErrorMessage(new StringBuilder().append("Error: Could not initialize plug-ins: ")
                    .append(e.getLocalizedMessage()).toString());
        }

        if (!noGUI) {
            // hide splash screen
            splash.dispose();
        }

        if (currentArch == null) {
            System.exit(1);
        }

        if (!auto) {
            try {
                // if the automatization is turned off, start the emuStudio normally
                if (inputFileName != null) {
                    new StudioFrame(inputFileName, configName).setVisible(true);
                } else {
                    new StudioFrame(configName).setVisible(true);
                }
            } catch (Exception e) {
                logger.error("Could not start main window.", e);
                tryShowErrorMessage("Could not start main window.");
                System.exit(1);
            }
        } else {
            new Automatization(currentArch, inputFileName, outputFileName).runAutomatization(noGUI);
            currentArch.destroy();
            System.exit(0);
        }
    }
}
