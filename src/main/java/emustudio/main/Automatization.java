/*
 * KISS, YAGNI, DRY
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
import static emulib.plugins.compiler.Message.MessageType.TYPE_ERROR;
import static emulib.plugins.compiler.Message.MessageType.TYPE_INFO;
import static emulib.plugins.compiler.Message.MessageType.TYPE_WARNING;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.CPUListener;
import emulib.plugins.cpu.CPU.RunState;
import static emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_ADDR_FALLOUT;
import static emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_BAD_INSTR;
import static emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_BREAK;
import static emulib.plugins.cpu.CPU.RunState.STATE_STOPPED_NORMAL;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.ArchitectureManager;
import emustudio.gui.AutoDialog;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages the emuStudio automatization process. In the process
 * the emulation is started automatically and results are collected.
 */
public class Automatization implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger("automatization");
    private final ArchitectureManager currentArch;

    private AutoDialog progressGUI;
    private Reader inputFileReader;
    private File inputFile;

    private Compiler compiler;
    private Memory memory;
    private CPU cpu;
    private Device[] devices;

    private RunState resultState;
    private final boolean nogui;

    public Automatization(ArchitectureManager currentArch, String inputFileName, boolean nogui) throws AutomatizationException {
        this.currentArch = currentArch;
        this.nogui = nogui;

        if (currentArch == null) {
            throw new AutomatizationException("Computer architecture must be set");
        }
        if (inputFileName == null) {
            throw new AutomatizationException("Input file name cannot be empty");
        }
        this.inputFile = new File(inputFileName);
        try {
            inputFileReader = new BufferedReader(new FileReader(inputFile));
        } catch( FileNotFoundException e) {
            throw new AutomatizationException("Input file not found", e);
        }

        cpu = currentArch.getComputer().getCPU();
        if (cpu == null) {
            throw new AutomatizationException("CPU must be set");
        }
        compiler = currentArch.getComputer().getCompiler();
        memory = currentArch.getComputer().getMemory();
        devices = currentArch.getComputer().getDevices();

        if (!nogui) {
            progressGUI = new AutoDialog();
        }
    }

    private void setProgress(String msg, boolean stopEnabled) {
        LOGGER.info(msg);
        if (progressGUI != null) {
            progressGUI.setAction(msg, stopEnabled);
        }
    }

    private void autoCompile() throws AutomatizationException {
        if ((compiler == null) || (inputFile == null)) {
            return;
        }
        setProgress("Compiling input file: " + inputFile, false);
        CompilerListener reporter = new CompilerListener() {
            @Override
            public void onStart() {
                LOGGER.info("Compiler started working.");
            }

            @Override
            public void onMessage(Message message) {

                switch (message.getMessageType()) {
                    case TYPE_INFO:
                        LOGGER.info(message.getFormattedMessage());
                        break;
                    case TYPE_ERROR:
                        LOGGER.error(message.getFormattedMessage());
                        break;
                    case TYPE_WARNING:
                        LOGGER.warn(message.getFormattedMessage());
                        break;
                    default:
                        LOGGER.info(message.getFormattedMessage());
                        break;
                }
            }

            @Override
            public void onFinish(int errorCode) {
                if (errorCode != 0) {
                    LOGGER.error("Compiler finished with error code: " + errorCode);
                } else {
                    LOGGER.info("Compiler finished successfully.");
                }
            }
        };

        // Initialize compiler
        compiler.addCompilerListener(reporter);

        String fileName = inputFile.getAbsolutePath();
        if (!compiler.compile(fileName, inputFileReader)) {
            throw new AutomatizationException("Error: compile failed. Automatization cannot continue.");
        }
    }

    private void setProgramStartAddress(int programStart) {
        if (memory != null) {
            setProgress("Program start address: " + String.format("%04Xh", programStart), false);
            memory.setProgramStart(programStart);
        } else {
            if (programStart > 0) {
                setProgress("Ignoring program start address: " + String.format("%04Xh", programStart), false);
            }
        }
        setProgress("Resetting CPU...", false);
    }

    private void autoEmulate() {
        setProgress("Running emulation...", true);

        final Object resultStateLock = new Object();

        // Show all devices if GUI is supported
        for (Device device : devices) {
            if (!nogui) {
                device.showGUI();
            }
        }

        resultState = RunState.STATE_RUNNING;
        cpu.addCPUListener(new CPUListener() {
            @Override
            public void runStateChanged(RunState state) {
                if (state != RunState.STATE_RUNNING) {
                    synchronized (resultStateLock) {
                        resultState = state;
                        resultStateLock.notify();
                    }
                }
            }

            @Override
            public void internalStateChanged() {
            }
        });
        cpu.execute();

        synchronized (resultStateLock) {
            try {
                resultStateLock.wait();
            } catch (InterruptedException e) {
            }
        }

        switch (resultState) {
            case STATE_STOPPED_ADDR_FALLOUT:
                LOGGER.error("Address fallout");
                break;
            case STATE_STOPPED_BAD_INSTR:
                LOGGER.error("Unknown instruction");
                break;
            case STATE_STOPPED_BREAK:
                LOGGER.info("Breakpoint");
                break;
            case STATE_STOPPED_NORMAL:
                LOGGER.info("Normal stop");
                break;
            default:
                LOGGER.error("Invalid state (" + resultState + ")");
                break;
        }
        LOGGER.info("Instruction position = " + String.format("%04Xh", cpu.getInstructionPosition()));

        setProgress("Emulation completed", false);
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
        if (progressGUI != null) {
            progressGUI.setVisible(true);
        }

        PluginType compilerType = (compiler != null) ? compiler.getClass().getAnnotation(PluginType.class) : null;
        PluginType cpuType = (cpu != null) ? cpu.getClass().getAnnotation(PluginType.class) : null;
        PluginType memoryType = (memory != null) ? memory.getClass().getAnnotation(PluginType.class) : null;

	try {
            LOGGER.info("Starting emulation automatization...");
            LOGGER.info("Compiler: " + ((compilerType == null) ? "none" : compilerType.title()));
            LOGGER.info("CPU: " + ((cpuType == null) ? "none" : cpuType.title()));
            LOGGER.info("Memory: " + ((memoryType == null) ? "none" : memoryType.title()));

            int size = devices.length;
            for (int i = 0; i < size; i++) {
                PluginType deviceType = devices[i].getClass().getAnnotation(PluginType.class);
                LOGGER.info("Device #" + String.format("%02d", i)  + ": " + deviceType.title());
            }

            if (compiler != null) {
                autoCompile();
                int programStart = compiler.getProgramStartAddress();
                setProgramStartAddress(programStart);
                cpu.reset(programStart);

            }
            autoEmulate();
        } catch (AutomatizationException e) {
            LOGGER.error("Error during automatization", e);
            Main.tryShowErrorMessage("Error: " + e.getMessage());
        } finally {
            currentArch.destroy();
            if (progressGUI != null) {
                progressGUI.dispose();
                progressGUI = null;
            }
        }
    }

}
