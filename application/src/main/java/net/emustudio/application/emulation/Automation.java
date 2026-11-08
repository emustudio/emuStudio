/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.helpers.Unchecked;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ExecutionException;

/**
 * This class manages the emuStudio automation process. In the process
 * the emulation is started automatically and results are collected.
 */
public class Automation implements Runnable {
    public static final int DONT_WAIT = -1;
    private static final Logger LOGGER = LoggerFactory.getLogger("automation");

    private final File inputFile;
    private final VirtualComputer computer;
    private final AppSettings appSettings;
    private final Dialogs dialogs;
    private final int waitForFinishMillis;
    private final Integer programLocation;
    private final EmulationProgress progress;
    private volatile CPU.RunState resultState;
    private Path debugScript;
    private Path instructionRecording;
    private boolean recordingStarted;
    private boolean successful=true;
    public void setDebugScript(Path path) { debugScript=path; }
    public void setInstructionRecording(Path path) { instructionRecording = path; }
    public boolean isSuccessful() { return successful; }

    public Automation(VirtualComputer computer, Path inputFile, AppSettings appSettings,
                      Dialogs dialogs, int waitForFinishMillis, Integer programLocation,
                      EmulationProgress progress) throws AutomationException {
        this.computer = Objects.requireNonNull(computer);
        this.appSettings = Objects.requireNonNull(appSettings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.waitForFinishMillis = waitForFinishMillis;
        this.programLocation = programLocation;
        this.progress = Objects.requireNonNull(progress);

        if (inputFile != null) {
            this.inputFile = Objects.requireNonNull(inputFile, "Input file must be defined").toFile();
            if (!this.inputFile.exists()) {
                throw new AutomationException("Input file not found");
            }
        } else {
            this.inputFile = null;
        }
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
        progress.show();

        LOGGER.info("Starting emulation automation...");
        LOGGER.info("Emulating computer: {}", computer.getComputerConfig().getName());

        computer.getCompiler().ifPresent(
                compiler -> LOGGER.info("Compiler: {}, version {}", compiler.getTitle(), compiler.getVersion())
        );
        computer.getCPU().ifPresent(cpu -> LOGGER.info("CPU: {}, version {}", cpu.getTitle(), cpu.getVersion()));
        computer.getMemory().ifPresent(memory -> LOGGER.info("Memory: {}, version {}", memory.getTitle(), memory.getVersion()));
        computer.getDevices().forEach(
                device -> LOGGER.info("Device: {}, version {}", device.getTitle(), device.getVersion())
        );

        try {
            computer.getCompiler().ifPresent(compiler -> Unchecked.run(() -> autoCompile(compiler)));

            if ((debugScript != null || instructionRecording != null) && computer.getCPU().isEmpty())
                throw new IllegalStateException("Debug scripts and recording require a CPU");
            computer.getCPU().ifPresent(cpu -> {
                setProgress("Resetting CPU...", false);
                if (programLocation != null) {
                    setProgress("Program start location: " + String.format("%04Xh", programLocation), false);
                }
                if (inputFile == null || computer.getCompiler().isEmpty())
                    computer.getExecution().orElseThrow().compile(() -> {}, programLocation).join();
                if (instructionRecording != null) {
                    Unchecked.run(() -> computer.getDebugger().orElseThrow().recordTo(instructionRecording));
                    recordingStarted = true;
                }
                if (debugScript != null) {
                    try { net.emustudio.application.debugger.DebugScript.run(computer.getDebugger().orElseThrow(), debugScript); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                    catch (java.io.IOException e) { throw new IllegalStateException(e); }
                } else { autoEmulate(cpu); }
            });
        } catch (Exception e) {
            successful=false;
            LOGGER.error("Error during automation", e);
            dialogs.showError("Error during automation. Please consult log file for details.", "Emulation automation");
        } finally {
            if (recordingStarted) {
                try {
                    if (resultState == CPU.RunState.STATE_RUNNING) computer.getExecution().orElseThrow().pause().join();
                    computer.getDebugger().orElseThrow().stopRecordingFile();
                } catch (Exception e) {
                    successful = false;
                    LOGGER.error("Could not finish instruction recording", e);
                    dialogs.showError("Instruction recording is incomplete. Please consult log file for details.", "Emulation automation");
                }
                recordingStarted = false;
            }
            progress.dispose();
        }
    }

    private void setProgress(String msg, boolean stopEnabled) {
        LOGGER.info(msg);
        progress.setAction(msg, stopEnabled);
    }

    private void autoCompile(Compiler compiler) throws AutomationException {
        if (inputFile == null) {
            return;
        }
        setProgress("Compiling input file: " + inputFile, false);
        AtomicInteger errors = new AtomicInteger();
        CompilerListener reporter = new CompilerListener() {
            @Override
            public void onStart() {
                LOGGER.info("Compiler started working.");
            }

            @Override
            public void onMessage(CompilerMessage message) {

                switch (message.getMessageType()) {
                    case TYPE_ERROR:
                        errors.incrementAndGet();
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
        Runnable compilation = () -> {
            compiler.compile(inputFile.toPath(), Optional.empty());
            if (errors.get() > 0) throw new IllegalStateException("Compilation failed");
        };
        try {
            computer.getExecution().ifPresentOrElse(execution -> execution.compile(compilation, programLocation).join(), compilation);
        } finally {
            compiler.removeCompilerListener(reporter);
        }

        if (errors.get() > 0) {
            throw new AutomationException("Compilation failed. Automation cannot continue.");
        }
    }

    private void autoEmulate(CPU cpu) {
        setProgress("Running emulation...", true);

        // Show all devices if GUI is supported
        for (Device device : computer.getDevices()) {
            if (!appSettings.noGUI && device.isGuiSupported()) {
                device.showGUI(null);
            }
        }

        resultState = CPU.RunState.STATE_RUNNING;
        EmulationController execution = computer.getExecution().orElseThrow();
        execution.start().join();
        CompletableFuture<CPU.RunState> completion = execution.runCompletion();
        try {
            resultState = waitForFinishMillis == DONT_WAIT ? completion.get()
                    : completion.get(waitForFinishMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
            // CLI observation duration is independent of cooperative lifecycle completion.
            resultState = execution.runState();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException failure) {
            throw new IllegalStateException("CPU execution failed", failure.getCause());
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
            case STATE_RUNNING:
                LOGGER.info("Observation duration elapsed; CPU is still running");
                break;
            default:
                LOGGER.error("Invalid state ({})", resultState);
                break;
        }
        LOGGER.info("Instruction location = {}", String.format("0x%04X", cpu.getInstructionLocation()));

        setProgress("Emulation completed", false);
    }
}
