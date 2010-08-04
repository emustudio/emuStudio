/*
 * Automatization.java
 *
 * KISS, YAGNI
 *
 *  Copyright (C) 2010 Peter Jakubčo <pjakubco@gmail.com>
 * 
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.main;

import emustudio.architecture.ArchHandler;
import emustudio.gui.AutoDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.EventObject;
import plugins.compiler.ICompilerContext;
import plugins.compiler.ICompilerContext.ICompilerListener;
import plugins.cpu.ICPU;
import plugins.cpu.ICPU.ICPUListener;
import runtime.StaticDialogs;

/**
 * This class manages the emuStudio automatization process. In the process
 * the emulation is started automatically and results are collected.
 *
 * @author vbmacher
 */
public class Automatization {
    private ArchHandler currentArch;
    private File outputFile;
    private File inputFile;
    private int result_state;

    public Automatization(String inputFileName, String outputFileName) {
        this.outputFile = new File(outputFileName);
        this.inputFile = new File(inputFileName);
        result_state = ICPU.STATE_STOPPED_NORMAL;
    }

    /**
     * This method outputs a message to the output file, if it is opened.
     *
     * @param message message to output
     * @param outw FileWriter object (the opened output file)
     * @throws IOException
     */
    private void output_message(String message, FileWriter outw)
            throws IOException {
        if (outw != null) {
            outw.write(message + "\n");
            outw.flush();
        }
    }

    /**
     * Method performs emulation automatization. It supposes that the virtual
     * configuration is loaded.
     *
     * All output is saved into output file.
     *
     */
    public void runAutomatization() {
	FileWriter outw = null;
        AutoDialog adia = new AutoDialog();

        adia.setVisible(true);

	try {
            if (outputFile != null) {
                outw = new FileWriter(outputFile);
            }
            String currentDate = new Date().toString();

            output_message("<html><head><title>Emulation report</title></head>"
                    + "<body><h1>Configuration</h1><p>" + currentDate + "</p>"
                    + "<table><tr><th>Compiler</th><td>" + 
                    currentArch.getCompiler().getTitle() + "</td></tr>"
                    + "<tr><th>CPU</th><td>" + currentArch.getCPU().getTitle()
                    + "</td></tr><th>Memory</th><td>" +
                    currentArch.getMemory().getTitle() + "</td></tr>", outw);

            for (int i = 0; i < currentArch.getDevices().length; i++) {
                output_message("<tr><th>Device #" + String.format("%02d", i)
                        + "</th><td>" + currentArch.getDevices()[i].getTitle()
                        + "</td></tr>", outw);
            }
            output_message("</table>", outw);

            if (inputFile != null) {
                adia.setAction("Compiling input file...", false);
                output_message("<h1>Compile process</h1><p><strong>File name:"
                        + "</strong>" + inputFile, outw);

                ICompilerListener reporter = new ICompilerListener() {
                    @Override
                    public void onCompileStart(EventObject evt) {}

                    @Override
                    public void onCompileInfo(EventObject evt, int row,
                            int col, String message, int errorCode, int messageType) {
                        String text = "<p>";

                        switch (messageType) {
                            case ICompilerContext.TYPE_ERROR:
                                text += "<strong>Error:</strong> ";
                                break;
                            case ICompilerContext.TYPE_INFO:
                                text += "<strong>Info:</strong> ";
                                break;
                            case ICompilerContext.TYPE_WARNING:
                                text += "<strong>Warning:</strong> ";
                                break;
                        }
                        if (!((row < 0) || (col < 0)))
                            text += "[" + row + "," + col + "] ";
                        if (errorCode >= 0)
                            text += " (" + errorCode + ") ";
                        text += message + "</p>";
                    }

                    @Override
                    public void onCompileFinish(EventObject evt, int errorCode) {}
                };

                // Initialize compiler
                currentArch.getCompiler().initialize(currentArch.getCompilerHash(),
                        currentArch);

                FileReader fileR;
                fileR = new FileReader(inputFile);
                BufferedReader r = new BufferedReader(fileR);

                String fn = inputFile.getAbsolutePath();
                fn = fn.substring(0, fn.lastIndexOf(".")) + ".hex"; // hex is correct?

                boolean succ = currentArch.getCompiler().compile(fn, r);

                if (succ == false) {
                    StaticDialogs.showErrorMessage("Error: compile process failed!");
                    output_message("<p>Compilation failed.</p>", outw);
                    return;
                } else
                    output_message("<p>Compilation was successful.</p>", outw);

                int programStart = currentArch.getCompiler().getProgramStartAddress();
                currentArch.getMemory().setProgramStart(programStart);
                currentArch.getCPU().reset(programStart);
            }
            adia.setAction("Running emulation...", true);
            output_message("<h1>Emulation process</h1>", outw);

            final Thread t = new Thread() {
                @Override
                public void run() {
                    result_state = ICPU.STATE_RUNNING;
                    currentArch.getCPU().execute();
                    // waits till something interrupts this thread
                    try {
                        while (true) {
                            Thread.sleep(0xfffffff);
                        }
                    } catch (InterruptedException e) {
                    }
                }
            };
            currentArch.getCPU().addCPUListener(new ICPUListener() {
                @Override
                public void runChanged(EventObject evt, int state) {
                    if (state != ICPU.STATE_RUNNING) {
                        result_state = state;
                        t.interrupt();
                    }
                }

                @Override
                public void stateUpdated(EventObject evt) {}
            });
            t.start();
            t.join();

            switch (result_state) {
                case ICPU.STATE_STOPPED_ADDR_FALLOUT:
                    output_message("<p>FAILED (address fallout)</p>", outw);
                    break;
                case ICPU.STATE_STOPPED_BAD_INSTR:
                    output_message("<p>FAILED (unknown instruction)</p>", outw);
                    break;
                case ICPU.STATE_STOPPED_BREAK:
                    output_message("<p>DONE (breakpoint stop)</p>", outw);
                    break;
                case ICPU.STATE_STOPPED_NORMAL:
                    output_message("<p>DONE (normal stop)</p>", outw);
                    break;
                default:
                    output_message("<p>FAILED (invalid state)</p>", outw);
                    break;
            }
            output_message("<p>Instruction postion: " + String.format("%04Xh",
                    currentArch.getCPU().getInstrPosition()) + "</p>", outw);

            adia.setAction("Emulation finished.", false);
        } catch (FileNotFoundException e1) {
            StaticDialogs.showErrorMessage("Error: Input file not found!");
        } catch (IOException e2) {
            StaticDialogs.showErrorMessage("Error in writing to output file");
        } catch (Exception e) {
            StaticDialogs.showErrorMessage("Error in compile process:\n" + e.toString());
//            e.printStackTrace();
        } finally {
            adia.dispose();
            adia = null;
            try {
                output_message("<p>" + new Date().toString() + "</p>", outw);
                outw.close();
            } catch (IOException e1) {}
        }
    }

}
