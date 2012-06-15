/*
 * Automatization.java
 *
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2010-2012, Peter Jakubčo
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

import emulib.plugins.compiler.ICompiler;
import emulib.plugins.compiler.ICompiler.ICompilerListener;
import emulib.plugins.compiler.Message;
import emulib.plugins.cpu.ICPU;
import emulib.plugins.cpu.ICPU.ICPUListener;
import emulib.plugins.cpu.ICPU.RunState;
import emulib.plugins.device.IDevice;
import emulib.plugins.memory.IMemory;
import emustudio.architecture.ArchHandler;
import emustudio.gui.AutoDialog;
import java.io.*;
import java.util.Date;
import java.util.EventObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the emuStudio automatization process. In the process
 * the emulation is started automatically and results are collected.
 *
 * @author vbmacher
 */
public class Automatization {
    private final static Logger logger = LoggerFactory.getLogger(Automatization.class);
    private ArchHandler currentArch;
    private File outputFile;
    private File inputFile;
    private RunState result_state;

    /**
     * Creates new automatization object.
     *
     * @param currentArch loaded computer
     * @param inputFileName input source code file name
     * @param outputFileName output log file name
     */
    public Automatization(ArchHandler currentArch, String inputFileName,
            String outputFileName) {
        this.currentArch = currentArch;
        this.outputFile = new File(outputFileName);
        this.inputFile = new File(inputFileName);
        result_state = RunState.STATE_STOPPED_NORMAL;
    }

    /**
     * This method outputs a message to the output file, if it is opened.
     *
     * @param message message to output
     * @param outw FileWriter object (the opened output file)
     * @throws IOException
     */
    private static void output_message(String message, FileWriter outw) {
        if (outw != null) {
            try {
            outw.write(message + "\n");
            outw.flush();
            } catch (IOException e) {
                logger.error("Could not write message to report:" + message, e);
            }
        }
    }

    /**
     * Method performs emulation automatization. It supposes that the virtual
     * configuration is loaded.
     *
     * All output is saved into output file.
     *
     * @param nogui if true, GUI will not be shown.
     */
    public void runAutomatization(boolean nogui) {
        AutoDialog adia = null;
        if (!nogui) {
            adia = new AutoDialog();
            adia.setVisible(true);
        }

        ICompiler compiler = currentArch.getComputer().getCompiler();
        IMemory memory = currentArch.getComputer().getMemory();
        final ICPU cpu = currentArch.getComputer().getCPU();
        IDevice[] devices = currentArch.getComputer().getDevices();

	try {
            final FileWriter outw = (outputFile == null) ? null : new FileWriter(outputFile);
            String currentDate = new Date().toString();

            output_message("<html><head><title>Emulation report</title></head>"
                    + "<body><h1>Configuration</h1><p>" + currentDate + "</p>"
                    + "<table><tr><th>Compiler</th><td>" + 
                    ((compiler == null) ? "none" : compiler.getTitle())
                    + "</td></tr><tr><th>CPU</th><td>" + cpu.getTitle()
                    + "</td></tr><th>Memory</th><td>" +
                    ((memory == null) ? "none" : memory.getTitle())
                    + "</td></tr>", outw);

            int size = devices.length;
            for (int i = 0; i < size; i++) {
                output_message("<tr><th>Device #" + String.format("%02d", i)
                        + "</th><td>" + devices[i].getTitle()
                        + "</td></tr>", outw);
            }
            output_message("</table>", outw);

            if ((compiler != null) && (inputFile != null)) {
                if (!nogui) {
                    adia.setAction("Compiling input file...", false);
                }
                logger.info("Compiling input file: " + inputFile);

                output_message("<h1>Compile process</h1><p><strong>File name:"
                        + "</strong>" + inputFile, outw);

                ICompilerListener reporter = new ICompilerListener() {
                    @Override
                    public void onStart(EventObject evt) {}

                    @Override
                    public void onMessage(EventObject evt, Message message) {
                        String text = "<p>" + message.getForrmattedMessage() + "</p>";
                        output_message(text, outw);
                    }

                    @Override
                    public void onFinish(EventObject evt, int errorCode)
                    {}
                };

                // Initialize compiler
                compiler.addCompilerListener(reporter);

                FileReader fileR;
                fileR = new FileReader(inputFile);
                BufferedReader r = new BufferedReader(fileR);

                String fn = inputFile.getAbsolutePath();

                boolean succ = compiler.compile(fn, r);

                if (succ == false) {
                    logger.error("Compile process failed.");
                    Main.tryShowErrorMessage("Error: compile process failed!");
                    output_message("<p>Compilation failed.</p>", outw);
                    return;
                } else {
                    logger.info("Source code was compiled successfully.");
                    output_message("<p>Source code was compiled successfully.</p>", outw);
                }

                int programStart = compiler.getProgramStartAddress();
                if (memory != null) {
                    logger.info("Setting program start to: " + programStart);
                    output_message("<p>Setting program start to: " + programStart + ".</p>", outw);
                    memory.setProgramStart(programStart);
                }
                cpu.reset(programStart);
            }
            if (!nogui) {
                adia.setAction("Running emulation...", true);
            } 
            logger.info("Running emulation...");
            output_message("<h1>Emulation process</h1>", outw);

            final Object lock = new Object();

            result_state = RunState.STATE_RUNNING;
            cpu.addCPUListener(new ICPUListener() {
                @Override
                public void runChanged(EventObject evt, RunState state) {
                    if (state != RunState.STATE_RUNNING) {
                        synchronized(lock) {
                            result_state = state;
                        }
                    }
                }
                @Override
                public void stateUpdated(EventObject evt) {}
            });
            cpu.execute();

            boolean stop = false;
            do {
                synchronized(lock) {
                    if (result_state != RunState.STATE_RUNNING)
                        stop = true ;
                }
            } while (!stop);

            switch (result_state) {
                case STATE_STOPPED_ADDR_FALLOUT:
                    logger.error("CPU run failed: Address fallout.");
                    output_message("<p>FAILED (address fallout)</p>", outw);
                    break;
                case STATE_STOPPED_BAD_INSTR:
                    logger.error("CPU run failed: Unknown instruction.");
                    output_message("<p>FAILED (unknown instruction)</p>", outw);
                    break;
                case STATE_STOPPED_BREAK:
                    logger.info("CPU stop: Breakpoint.");
                    output_message("<p>DONE (breakpoint stop)</p>", outw);
                    break;
                case STATE_STOPPED_NORMAL:
                    logger.info("CPU stop: Normal stop.");
                    output_message("<p>DONE (normal stop)</p>", outw);
                    break;
                default:
                    logger.error("CPU run failed: Invalid state: " + result_state);
                    output_message("<p>FAILED (invalid state): " +
                            result_state + "</p>", outw);
                    break;
            }
            logger.info("Current instruction position: " + String.format("%04Xh",
                    cpu.getInstrPosition()));
            output_message("<p>Instruction postion: " + String.format("%04Xh",
                    cpu.getInstrPosition()) + "</p>", outw);

            if (!nogui) {
                adia.setAction("Emulation finished.", false);
            }
            logger.info("Emulation finished.");
            output_message("<p>" + new Date().toString() + "</p>", outw);
            outw.close();
        } catch (FileNotFoundException e) {
            logger.error("Input file not found", e);
            Main.tryShowErrorMessage("Error: Input file not found!");
        } catch (IOException e) {
            logger.error("Could not write to report file", e);
            Main.tryShowErrorMessage("Could not write to report file");
        } catch (Exception e) {
            logger.error("Unknown error during emulation.", e);
            Main.tryShowErrorMessage("Error in compile process:\n" + e.toString());
        }
        // Set "auto" setting back to "false" to all plugins
        currentArch.writeSettingToAll("auto", "false");
        currentArch.writeSettingToAll("nogui", "false");

        currentArch.destroy();
        if (!nogui) {
            adia.dispose();
        }
    }

}
