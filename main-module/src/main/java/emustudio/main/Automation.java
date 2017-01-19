/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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

import emulib.annotations.PluginType;
import emulib.plugins.compiler.Compiler;
import emulib.plugins.compiler.Compiler.CompilerListener;
import emulib.plugins.compiler.Message;
import emulib.plugins.cpu.CPU;
import emulib.plugins.cpu.CPU.CPUListener;
import emulib.plugins.cpu.CPU.RunState;
import emulib.plugins.device.Device;
import emulib.plugins.memory.Memory;
import emustudio.architecture.Computer;
import emustudio.gui.AutoDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.Optional;

/**
 * This class manages the emuStudio automation process. In the process
 * the emulation is started automatically and results are collected.
 */
class Automation implements Runnable {
    private final static Logger LOGGER = LoggerFactory.getLogger("automation");

    private AutoDialog progressGUI;
    private File inputFile;

    private final Computer computer;

    private RunState resultState;
    private final boolean nogui;

    Automation(Computer computer, String inputFileName, boolean nogui) throws AutomationException {
        this.computer = Objects.requireNonNull(computer);
        this.nogui = nogui;
        this.inputFile = new File(Objects.requireNonNull(inputFileName));

        if (!inputFile.exists()) {
            throw new AutomationException("Input file not found");
        }
        if (!nogui) {
            progressGUI = new AutoDialog(computer);
        }
    }

    private void setProgress(String msg, boolean stopEnabled) {
        LOGGER.info(msg);
        if (progressGUI != null) {
            progressGUI.setAction(msg, stopEnabled);
        }
    }

    private void autoCompile(Compiler compiler) throws AutomationException {
        if (inputFile == null) {
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
        if (!compiler.compile(fileName)) {
            throw new AutomationException("Compile failed. Automation cannot continue.");
        }
    }

    private void setProgramStartAddress(int programStart) {
        Optional<Memory> memory = computer.getMemory();
        if (memory.isPresent()) {
            setProgress("Program start address: " + String.format("%04Xh", programStart), false);
            memory.get().setProgramStart(programStart);
        } else {
            if (programStart > 0) {
                setProgress("Ignoring program start address: " + String.format("%04Xh", programStart), false);
            }
        }
    }

    private void autoEmulate(CPU cpu) {
        setProgress("Running emulation...", true);

        final Object resultStateLock = new Object();

        // Show all devices if GUI is supported
        for (Device device : computer.getDevices()) {
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
                Thread.currentThread().interrupt();
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
     * <p/>
     * It assumes that the virtual computer is loaded.
     * <p/>
     * All output is saved into output file.
     */
    @Override
    public void run() {
        if (progressGUI != null) {
            progressGUI.setVisible(true);
        }

        CPU cpu = computer.getCPU().get();
        Optional<Compiler> compiler = computer.getCompiler();
        Optional<Memory> memory = computer.getMemory();

        PluginType compilerType = compiler.isPresent() ? compiler.get().getClass().getAnnotation(PluginType.class) : null;
        PluginType cpuType = cpu.getClass().getAnnotation(PluginType.class);
        PluginType memoryType = memory.isPresent() ? memory.get().getClass().getAnnotation(PluginType.class) : null;

        try {
            LOGGER.info("Starting emulation automatization...");
            LOGGER.info("Compiler: " + ((compilerType == null) ? "none" : compilerType.title()));
            LOGGER.info("CPU: " + ((cpuType == null) ? "none" : cpuType.title()));
            LOGGER.info("Memory: " + ((memoryType == null) ? "none" : memoryType.title()));

            memory.ifPresent(m -> LOGGER.info("Memory size: {}", m.getSize()));

            int i = 0;
            for (Device device : computer.getDevices()) {
                PluginType deviceType = device.getClass().getAnnotation(PluginType.class);
                LOGGER.info("Device #" + String.format("%02d", i++) + ": " + deviceType.title());
            }

            if (compiler.isPresent()) {
                autoCompile(compiler.get());
                int programStart = compiler.get().getProgramStartAddress();
                setProgramStartAddress(programStart);
                setProgress("Resetting CPU...", false);
                cpu.reset(programStart);

            }
            autoEmulate(cpu);
        } catch (AutomationException e) {
            LOGGER.error("Error during automatization", e);
            Main.tryShowErrorMessage(e.getMessage());
        } finally {
            if (progressGUI != null) {
                progressGUI.dispose();
                progressGUI = null;
            }
        }
    }

}
