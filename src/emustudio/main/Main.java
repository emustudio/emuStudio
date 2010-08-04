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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.EventObject;

import plugins.compiler.IMessageReporter;
import plugins.cpu.ICPU;
import plugins.cpu.ICPUContext.ICPUListener;

import runtime.StaticDialogs;
import emustudio.gui.AutoDialog;
import emustudio.gui.LoadingDialog;
import emustudio.gui.OpenArchDialog;
import emustudio.gui.StudioFrame;

/**
 *
 * @author vbmacher
 */
public class Main {
    public static ArchLoader aloader;
    public static ArchHandler currentArch = null;    
    private static String inputFile = null;
	private static int result_state;
    
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
                        StaticDialogs.showErrorMessage("Error: Config file already defined!");
                        return;
                    }
                    configName = args[i++];
                } else if (arg.equals("-INPUT")) {
                    // what input file take to compiler
                    if (inputFile != null) {
                        StaticDialogs.showErrorMessage("Error: Input file already defined!");
                        return;
                    }
                    inputFile = args[i++];
                } else if (arg.equals("-OUTPUT")) {
                    // what output file take for emuStudio messages during
                    // automation process. This option has a meaning
                    // only if the "-auto" option is set too.
                    if (outputFile != null) {
                        StaticDialogs.showErrorMessage("Error: Output file already defined!");
                        return;
                    }
                    outputFile = args[i++];
                } else if (arg.equals("-AUTO")) {
                    auto = true;
                } else {
                    StaticDialogs.showErrorMessage("Error: Invalid command line argument!\n" +
                        "(" + arg + ")");
                    return;
                }
            }
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try { javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName()); }
        catch (javax.swing.UnsupportedLookAndFeelException e) {}
        catch (ClassNotFoundException e) {}
        catch (InstantiationException e) {}
        catch (IllegalAccessException e) {}

        aloader = new ArchLoader();
        String configName = null;
        boolean auto = false;
        
        
        if (configName == null) {
        	OpenArchDialog odi = new OpenArchDialog();
        	odi.setVisible(true);
        	if (odi.getOK()) configName = odi.getArchName();
        	if (configName == null) return;
        }

        LoadingDialog splash = new LoadingDialog();
        splash.setVisible(true);
        try {
        	if (auto)
        		currentArch = aloader.load(configName, true);
        	else
            	currentArch = aloader.load(configName, false);
        } catch(Error er) {
            String h = er.getLocalizedMessage();
            if (h == null || h.equals("")) h = "Unknown error";
            StaticDialogs.showErrorMessage("Fatal Error: " + h);
            currentArch = null;
        }
        splash.dispose();
        splash = null;
        
        if (currentArch != null) {
        	if (!auto) {
        		if (inputFile != null)
        			new StudioFrame(inputFile).setVisible(true);
        		else
        			new StudioFrame().setVisible(true);
        	} else {
        		auto();
            	currentArch.destroy();
            	System.exit(0);
        	}
        }
        else {
        	System.exit(0);
        }        
    }
    
}
