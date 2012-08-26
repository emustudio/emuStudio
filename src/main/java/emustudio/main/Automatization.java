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

import emulib.annotations.PluginType;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Compiler.CompilerListener;
import emulib.plugins.compiler.Message;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.CPUListener;
import emulib.plugins.cpu.CPU.RunState;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.ArchitectureManager;
import emustudio.gui.AutoDialog;
import java.io.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the emuStudio automatization process. In the process
 * the emulation is started automatically and results are collected.
 *
 * @author vbmacher
 */
public class Automatization implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger("automatization");
    private ArchitectureManager currentArch;
    private File inputFile;
    private RunState resultState;
    private boolean nogui;

    /**
     * Creates new automatization object.
     *
     * @param currentArch loaded computer
     * @param inputFileName input source code file name
     * @param nogui if true, GUI will not be shown.
     */
    public Automatization(ArchitectureManager currentArch, String inputFileName, boolean nogui) {
        this.currentArch = currentArch;
        this.inputFile = new File(inputFileName);
        resultState = RunState.STATE_STOPPED_NORMAL;
        this.nogui = nogui;
    }

    /**
     * Executes automatic emulation.
     * 
     * It assumes that the virtual computer is loaded.
     *
     * All output is saved into output file.
     */
    @Override
    public void run() {
        AutoDialog progressGUI = null;
        if (!nogui) {
            progressGUI = new AutoDialog();
            progressGUI.setVisible(true);
        }

        Compiler compiler = currentArch.getComputer().getCompiler();
        Memory memory = currentArch.getComputer().getMemory();
        final CPU cpu = currentArch.getComputer().getCPU();
        Device[] devices = currentArch.getComputer().getDevices();

        PluginType compilerType = (compiler != null) ? compiler.getClass().getAnnotation(PluginType.class) : null;
        PluginType cpuType = (cpu != null) ? cpu.getClass().getAnnotation(PluginType.class) : null;
        PluginType memoryType = (memory != null) ? memory.getClass().getAnnotation(PluginType.class) : null;
        
	try {
            logger.info("Emulation automatization started");
            logger.info("Compiler: " + ((compilerType == null) ? "none" : compilerType.title()));
            logger.info("CPU: " + ((cpuType == null) ? "none" : cpuType.title()));
            logger.info("Memory: " + ((memoryType == null) ? "none" : memoryType.title()));

            int size = devices.length;
            for (int i = 0; i < size; i++) {
                PluginType deviceType = devices[i].getClass().getAnnotation(PluginType.class);
                logger.info("Device #" + String.format("%02d", i)  + ": " + deviceType.title());
            }

            if ((compiler != null) && (inputFile != null)) {
                logger.info("Compiling input file: " + inputFile);
                if (!nogui) {
                    progressGUI.setAction("Compiling input file...", false);
                }

                CompilerListener reporter = new CompilerListener() {
                    @Override
                    public void onStart() {
                        logger.info("Compiler started working.");
                    }

                    @Override
                    public void onMessage(Message message) {
                        
                        switch (message.getMessageType()) {
                            case TYPE_INFO:
                                logger.info(message.getForrmattedMessage());
                            case TYPE_ERROR:
                                logger.error(message.getForrmattedMessage());
                            case TYPE_WARNING:
                                logger.warn(message.getForrmattedMessage());
                            default:
                                logger.info(message.getForrmattedMessage());
                        }
                    }

                    @Override
                    public void onFinish(int errorCode)
                    {
                        if (errorCode != 0) {
                            logger.error("Compiler finished with error code: " + errorCode);
                            return;
                        }
                        logger.info("Compiler finished successfully.");
                    }
                };

                // Initialize compiler
                compiler.addCompilerListener(reporter);

                FileReader fileR;
                fileR = new FileReader(inputFile);
                BufferedReader bufferedReader = new BufferedReader(fileR);

                String fileName = inputFile.getAbsolutePath();

                boolean succ = compiler.compile(fileName, bufferedReader);

                if (succ == false) {
                    logger.error("Compile process failed. Emulation process cannot continue.");
                    Main.tryShowErrorMessage("Error: compile process failed. Emulation process cannot continue.");
                    return;
                } else {
                    logger.info("Source code was compiled successfully.");
                }

                int programStart = compiler.getProgramStartAddress();
                if (memory != null) {
                    logger.info("Setting program start to: " + String.format("%04Xh", programStart));
                    memory.setProgramStart(programStart);
                } else {
                    if (programStart > 0) {
                        logger.warn("Ignoring program start: " + String.format("%04Xh", programStart));
                    }
                }
                logger.info("Resetting CPU...");
                if (!nogui) {
                    progressGUI.setAction("Resetting CPU...", false);
                } 
                cpu.reset(programStart);
            }
            if (!nogui) {
                progressGUI.setAction("Running emulation...", true);
            } 
            logger.info("Running automatic emulation (executing CPU)...");

            final Object resultStateLock = new Object();

            resultState = RunState.STATE_RUNNING;
            cpu.addCPUListener(new CPUListener() {
                @Override
                public void runChanged(RunState state) {
                    if (state != RunState.STATE_RUNNING) {
                        synchronized(resultStateLock) {
                            resultState = state;
                            resultStateLock.notify();
                        }
                    }
                }
                @Override
                public void stateUpdated() {}
            });
            cpu.execute();

            synchronized(resultStateLock) {
                try {
                    resultStateLock.wait();
                } catch (InterruptedException e) {}
            }

            switch (resultState) {
                case STATE_STOPPED_ADDR_FALLOUT:
                    logger.error("FAILED: Address fallout.");
                    break;
                case STATE_STOPPED_BAD_INSTR:
                    logger.error("FAILED: Unknown instruction.");
                    break;
                case STATE_STOPPED_BREAK:
                    logger.info("SUCCESS: Breakpoint.");
                    break;
                case STATE_STOPPED_NORMAL:
                    logger.info("SUCCESS: Normal stop.");
                    break;
                default:
                    logger.error("FAILED: Invalid state. (" + resultState + ")");
                    break;
            }
            logger.info("Instruction position after finish = " + String.format("%04Xh", cpu.getInstrPosition()));
            
            if (!nogui) {
                progressGUI.setAction("Emulation finished.", false);
            }
            logger.info("Emulation finished.");
        } catch (FileNotFoundException e) {
            logger.error("Input file not found", e);
            Main.tryShowErrorMessage("Error: Input file not found!");
        } catch (Exception e) {
            logger.error("Unknown error during emulation.", e);
            Main.tryShowErrorMessage("Error in compile process:\n" + e.toString());
        }
        // Set "auto" setting back to "false" to all plugins
        currentArch.writeSetting("auto", "false");
        currentArch.writeSetting("nogui", "false");

        currentArch.destroy();
        if (!nogui) {
            progressGUI.dispose();
        }
    }

}
