/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.emulation;

import net.emustudio.application.configuration.ApplicationConfig;
import net.emustudio.application.gui.dialogs.AutoDialog;
import net.emustudio.application.internal.Unchecked;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This class manages the emuStudio automation process. In the process
 * the emulation is started automatically and results are collected.
 */
public class Automation implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger("automation");
    public static final int DONT_WAIT = -1;

    private AutoDialog progressGUI;
    private final File inputFile;

    private final VirtualComputer computer;
    private final ApplicationConfig applicationConfig;
    private final Dialogs dialogs;
    private final int waitForFinishMillis;
    private final int programStart;

    private volatile CPU.RunState resultState;

    public Automation(VirtualComputer computer, String inputFileName, ApplicationConfig applicationConfig,
                      Dialogs dialogs, int waitForFinishMillis, int programStart) throws AutomationException {
        this.computer = Objects.requireNonNull(computer);
        this.applicationConfig = Objects.requireNonNull(applicationConfig);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.waitForFinishMillis = waitForFinishMillis;
        this.programStart = programStart;

        if (inputFileName != null) {
            this.inputFile = new File(Objects.requireNonNull(inputFileName, "Input file must be defined"));
            if (!inputFile.exists()) {
                throw new AutomationException("Input file not found");
            }
        } else {
            this.inputFile = null;
        }

        if (!applicationConfig.noGUI) {
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
            public void onMessage(CompilerMessage message) {

                switch (message.getMessageType()) {
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
            public void onFinish() {
                LOGGER.info("Compilation finished.");
            }
        };

        // Initialize compiler
        compiler.addCompilerListener(reporter);

        String fileName = inputFile.getAbsolutePath();
        if (!compiler.compile(fileName)) {
            throw new AutomationException("Compile failed. Automation cannot continue.");
        }
    }

    private void setProgramLocation(int programLocation) {
        computer.getMemory().ifPresentOrElse(memory -> {
            setProgress("Program start address: " + String.format("%04Xh", programLocation), false);
            memory.setProgramLocation(programLocation);
        }, () -> {
            if (programLocation > 0) {
                setProgress("Ignoring program start address: " + String.format("%04Xh", programLocation), false);
            }
        });
    }

    private void autoEmulate(CPU cpu) {
        setProgress("Running emulation...", true);

        final Object resultStateLock = new Object();

        // Show all devices if GUI is supported
        for (Device device : computer.getDevices()) {
            if (!applicationConfig.noGUI) {
                device.showGUI(null);
            }
        }

        resultState = CPU.RunState.STATE_RUNNING;
        cpu.addCPUListener(new CPU.CPUListener() {
            @Override
            public void runStateChanged(CPU.RunState state) {
                if (state != CPU.RunState.STATE_RUNNING) {
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
                if (waitForFinishMillis == DONT_WAIT) {
                    resultStateLock.wait();
                } else {
                    resultStateLock.wait(waitForFinishMillis);
                }
            } catch (InterruptedException e) {
                LOGGER.error("Emulation has been interrupted");
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
        LOGGER.info("Instruction location = " + String.format("%04Xh", cpu.getInstructionLocation()));

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

        LOGGER.info("Starting emulation automation...");

        computer.getCompiler().ifPresent(
            compiler -> LOGGER.info("Compiler: " + compiler.getTitle() + ", version " + compiler.getVersion())
        );
        computer.getCPU().ifPresent(cpu -> LOGGER.info("CPU: " + cpu.getTitle() + ", version " + cpu.getVersion()));
        computer.getMemory().ifPresent(memory -> {
            LOGGER.info("Memory: " + memory.getTitle() + ", version " + memory.getVersion());
            LOGGER.info("Memory size: {}", memory.getSize());
        });
        computer.getDevices().forEach(
            device -> LOGGER.info("Device: " + device.getTitle() + ", version " + device.getVersion())
        );

        try {
            AtomicReference<Integer> programLocation = new AtomicReference<>(0);
            computer.getCompiler().ifPresent(compiler -> {
                Unchecked.run(() -> autoCompile(compiler));
                programLocation.set(compiler.getProgramLocation());
                setProgramLocation(programLocation.get());
            });
            if (programStart > 0) {
                programLocation.set(programStart);
            }

            computer.getCPU().ifPresent(cpu -> {
                setProgress("Resetting CPU...", false);
                cpu.reset(programLocation.get());
                autoEmulate(cpu);
            });
        } catch (Exception e) {
            LOGGER.error("Error during automation", e);
            dialogs.showError("Error during automation. Please consult log file for details.", "Emulation automation");
        } finally {
            if (progressGUI != null) {
                progressGUI.dispose();
                progressGUI = null;
            }
        }
    }
}
