/*
 * Main.java
 *
 * Created on Nedeľa, 2007, august 5, 13:08
 *
 * KISS, YAGNI
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

import emustudio.architecture.ArchHandler;
import emustudio.architecture.ArchLoader;
import runtime.StaticDialogs;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenComputerDialog;
import emustudio.gui.StudioFrame;
import java.util.Date;

/**
 * Main class of the emuStudio platform.
 *
 * @author vbmacher
 */
public class Main {
    public static ArchHandler currentArch = null;    
    private static String inputFileName = null;
    private static String outputFileName = null;

    private static String configName = null;
    private static boolean auto = false;

    private static String password = null;

    /**
     * This method parsers the command line parameters. It sets
     * internal class data members accordingly.
     * 
     * @param args The command line arguments
     */
    private static void parseCommandLine(String[] args) {
        // process arguments
        if (args != null && args.length > 1) {
            int i = 0;
            while (i < args.length) {
                String arg = args[i++].toUpperCase();
                if (arg.equals("-CONFIG")) {
                    // what configuration to load
                    if (configName != null) {
                        System.out.println("Error: Config file already defined!");
                    } else
                        configName = args[i++];
                } else if (arg.equals("-INPUT")) {
                    // what input file take to compiler
                    if (inputFileName != null) {
                        System.out.println("Error: Input file already defined!");
                    } else
                        inputFileName = args[i++];
                } else if (arg.equals("-OUTPUT")) {
                    // what output file take for emuStudio messages during
                    // automation process. This option has a meaning
                    // only if the "-auto" option is set too.
                    if (outputFileName != null) {
                        System.out.println("Error: Output file already defined!");
                    } else
                        outputFileName = args[i++];
                } else if (arg.equals("-AUTO")) {
                    auto = true;
                } else {
                    System.out.println("Error: Invalid command line argument " +
                        "(" + arg + ")!");
                }
            }
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager
                    .getSystemLookAndFeelClassName());
        } catch (javax.swing.UnsupportedLookAndFeelException e) {
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
        }

        password = runtime.Context.SHA1(String.valueOf(Math.random()) +
                new Date().toString());
        if (!runtime.Context.assignPassword(password)) {
            StaticDialogs.showErrorMessage("Error: communication with emuLib failed.");
            return;
        }


        // parse command line arguments
        parseCommandLine(args);

        // if configuration name has not been specified, let user
        // to choose the configuration manually
        if (configName == null) {
            OpenComputerDialog odi = new OpenComputerDialog();
            odi.setVisible(true);
            if (odi.getOK())
                configName = odi.getArchName();
            if (configName == null)
                return;
        }

        // display splash screen, while loading the virtual computer
        LoadingDialog splash = new LoadingDialog();
        splash.setVisible(true);

        // load the virtual computer
        try { currentArch = ArchLoader.load(configName, auto); }
        catch (Error er) {
            String h = er.getLocalizedMessage();
            if (h == null || h.equals("")) {
                h = "Unknown error";
            }
            StaticDialogs.showErrorMessage("Error with computer loading : " + h);
            currentArch = null;
        }

        // hide splash screen
        splash.dispose();
        splash = null;

        if (currentArch == null) {
            System.exit(0);
        }

        runtime.Context.getInstance().assignComputer(password, 
                currentArch.getComputer());

        if (!auto) {
            // if the automatization is turned off, start the emuStudio normally
            if (inputFileName != null)
                new StudioFrame(inputFileName).setVisible(true);
            else
                new StudioFrame(configName).setVisible(true);
        } else {
            new Automatization(currentArch,inputFileName,outputFileName)
                    .runAutomatization();
            currentArch.destroy();
            System.exit(0);
        }
    }
}

