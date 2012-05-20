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
import emustudio.architecture.ArchHandler;
import emustudio.architecture.ArchLoader;
import emustudio.architecture.PluginLoadingException;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

/**
 * Main class of the emuStudio platform.
 *
 * @author vbmacher
 */
public class Main {

    /**
     * Loaded computer.
     */
    public static ArchHandler currentArch = null;
    private static String inputFileName = null;
    private static String outputFileName = null;
    private static String configName = null;
    private static boolean auto = false;
    private static boolean checkHash = false;
    private static String classToHash = null;
    private static boolean help = false;
    private static String password = null;
    private static boolean noGUI = false;

    /**
     * Get emuStudio password for emuLib identification security mechanism.
     * 
     * @return emuStudio password
     */
    public static String getPassword() {
        return password;
    }

    /**
     * This method parsers the command line parameters. It sets
     * internal class data members accordingly.
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
                        System.out.println("Config file already defined,"
                                + " ignoring this one: " + args[i]);
                    } else {
                        configName = args[i];
                        System.out.println("Loading virtual computer: "
                                + configName);
                    }
                } else if (arg.equals("--INPUT")) {
                    i++;
                    // what input file take to compiler
                    if (inputFileName != null) {
                        System.out.println("Input file already defined,"
                                + " ignoring this one: " + args[i]);
                    } else {
                        inputFileName = args[i];
                        System.out.println("Input file name: "
                                + inputFileName);
                    }
                } else if (arg.equals("--OUTPUT")) {
                    i++;
                    // what output file take for emuStudio messages during
                    // automation process. This option has a meaning
                    // only if the "-auto" option is set too.
                    if (outputFileName != null) {
                        System.out.println("Output file already defined,"
                                + " ignoring this one: " + args[i]);
                    } else {
                        outputFileName = args[i];
                        System.out.println("Output file name: "
                                + outputFileName);
                    }
                } else if (arg.equals("--AUTO")) {
                    auto = true;
                    System.out.println("Turning automatization on.");
                } else if (arg.equals("--HASH")) {
                    i++;
                    checkHash = true;
                    if (classToHash != null) {
                        System.out.println("Class file already defined,"
                                + " ignoring this one: " + args[i]);
                    } else {
                        classToHash = args[i];
                    }

                } else if (arg.equals("--HELP")) {
                    help = true;
                } else if (arg.equals("--NOGUI")) {
                    noGUI = true;
                } else {
                    System.out.println("Error: Invalid command line argument "
                            + "(" + arg + ")!");
                }
            } catch (ArrayIndexOutOfBoundsException e) {
            }
        }

    }

    /**
     * Compute hash of a plug-in context interface. Uses SHA-1 method.
     *
     * @param inter  Interface to computer hash of
     * @return SHA-1 hash string
     */
    private static String computeHash(Class<?> inter) {
        int i;
        Method[] methods, met;
        String hash = "";

        met = inter.getDeclaredMethods(); //  .getMethods();
        ArrayList me = new ArrayList();
        for (i = 0; i < met.length; i++)
            me.add(met[i]);
        Collections.sort(me, new Comparator() {

            @Override
            public int compare(Object o1, Object o2) {
                Method m1 = (Method)o1;
                Method m2 = (Method)o2;

                return m1.getName().compareTo(m2.getName());
            }

        });
        methods = (Method[])me.toArray(new Method[0]);
        me.clear();

        for (i = 0; i < methods.length; i++) {
            hash += methods[i].getGenericReturnType().toString() + " ";
            hash += methods[i].getName() + "(";
            Class<?>[] params = methods[i].getParameterTypes();
            for (int j = 0; j < params.length; j++)
                hash += params[j].getName() + ",";
            hash += ");";
        }
        try {
            return emulib.runtime.Context.SHA1(hash);
        } catch(Exception e) {
            return null;
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

        // Test if java_cup is loaded
        try {
            java_cup.runtime.Scanner d;
        } catch (NoClassDefFoundError e) {
            StaticDialogs.showErrorMessage("Error: java_cup library not loaded!");
            return;
        }
        
        password = emulib.runtime.Context.SHA1(String.valueOf(Math.random())
                + new Date().toString());
        if (!emulib.runtime.Context.assignPassword(password)) {
            StaticDialogs.showErrorMessage("Error:"
                    + " communication with emuLib failed.");
            return;
        }

        // parse command line arguments
        parseCommandLine(args);

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
                System.out.println(computeHash(cls));
            } catch (MalformedURLException e) {
            } catch (ClassNotFoundException e) {
                System.out.println("Error: Class is not found!");
            } catch (NullPointerException np) {
                System.out.println("Error: Class name is not specified!");
            }
            return;
        }

        // if configuration name has not been specified, let user
        // to choose the configuration manually
        if (configName == null) {
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK()) {
                configName = odi.getArchName();
            }
            if (configName == null) {
                System.exit(0);
            }
        }

        LoadingDialog splash = null;
        if (!noGUI) {
            // display splash screen, while loading the virtual computer
            splash = new LoadingDialog();
            splash.setVisible(true);
        } else
            System.out.println("Loading virtual computer: " + configName);

        // load the virtual computer
        try {
            currentArch = ArchLoader.getInstance().loadComputer(configName, auto, noGUI);
        } catch (PluginLoadingException e) {
            String h = e.getLocalizedMessage();
            if (h == null || h.equals("")) {
                h = "Unknown error";
            }
            if (!noGUI)
                StaticDialogs.showErrorMessage("Error with computer loading : " + h);
            else
                System.out.println("Error with computer loading : " + h);
            currentArch = null;
        } catch (Error er) {
            String h = er.getLocalizedMessage();
            if (h == null || h.equals("")) {
                h = "Unknown error";
            }
            if (!noGUI)
                StaticDialogs.showErrorMessage("Error with computer loading : " + h);
            else
                System.out.println("Error with computer loading : " + h);
            currentArch = null;
        }

        if (!noGUI) {
            // hide splash screen
            splash.dispose();
        }

        if (currentArch == null) {
            System.out.println("Error: Cannot load the virtual computer.");
            System.exit(0);
        }

        if (!auto) {
            try {
                // if the automatization is turned off, start the emuStudio normally
                if (inputFileName != null) {
                    new StudioFrame(inputFileName,configName).setVisible(true);
                } else {
                    new StudioFrame(configName).setVisible(true);
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else {
            new Automatization(currentArch, inputFileName, outputFileName)
                    .runAutomatization(noGUI);
            currentArch.destroy();
            System.exit(0);
        }
    }
}
