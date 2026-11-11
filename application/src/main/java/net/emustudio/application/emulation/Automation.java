/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.device.Device;
import net.emustudio.emulib.Unchecked;
import net.emustudio.emulib.application.ui.Dialogs;
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
    private boolean successful = true;

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
            this.inputFile = inputFile.toFile();
            if (!this.inputFile.exists()) {
                throw new AutomationException("Input file not found");
            }
        } else {
            this.inputFile = null;
        }
    }

    public boolean isSuccessful() { return successful; }

    /** Executes automatic emulation on an already loaded virtual computer. */
    @Override
    public void run() {
        progress.show();
        logComputer();

        try {
            computer.getCompiler().ifPresent(compiler -> Unchecked.run(() -> autoCompile(compiler)));
            computer.getCPU().ifPresent(cpu -> {
                prepareCpu();
                autoEmulate();
            });
        } catch (Exception failure) {
            successful = false;
            LOGGER.error("Error during automation", failure);
            dialogs.showError("Error during automation. Please consult log file for details.", "Emulation automation");
        } finally {
            progress.dispose();
        }
    }

    private void logComputer() {
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
    }

    private void prepareCpu() {
        setProgress("Resetting CPU...", false);
        if (programLocation != null) {
            setProgress("Program start location: " + String.format("%04Xh", programLocation), false);
        }
        if (inputFile == null || computer.getCompiler().isEmpty()) {
            computer.getExecution().orElseThrow().compile(() -> {}, programLocation).join();
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
        CompilerListener reporter = compilationReporter(errors);
        compiler.addCompilerListener(reporter);
        Runnable compilation = () -> {
            compiler.compile(inputFile.toPath(), Optional.empty());
            if (errors.get() > 0) throw new IllegalStateException("Compilation failed");
        };
        try {
            computer.getExecution().ifPresentOrElse(
                    execution -> execution.compile(compilation, programLocation).join(), compilation);
        } finally {
            compiler.removeCompilerListener(reporter);
        }

        if (errors.get() > 0) {
            throw new AutomationException("Compilation failed. Automation cannot continue.");
        }
    }

    private CompilerListener compilationReporter(AtomicInteger errors) {
        return new CompilerListener() {
            @Override
            public void onStart() {
                LOGGER.info("Compiler started working.");
            }

            @Override
            public void onMessage(CompilerMessage message) {
                reportCompilerMessage(message, errors);
            }

            @Override
            public void onFinish() {
                LOGGER.info("Compilation finished.");
            }
        };
    }

    private void reportCompilerMessage(CompilerMessage message, AtomicInteger errors) {
        switch (message.getType()) {
            case ERROR:
                errors.incrementAndGet();
                LOGGER.error(message.getFormattedMessage());
                break;
            case WARNING:
                LOGGER.warn(message.getFormattedMessage());
                break;
            default:
                LOGGER.info(message.getFormattedMessage());
                break;
        }
    }

    private void autoEmulate() {
        setProgress("Running emulation...", true);
        showDevices();
        EmulationController execution = computer.getExecution().orElseThrow();
        execution.start().join();
        reportResult(awaitCompletion(execution));
        LOGGER.info("Instruction location = {}", String.format("0x%04X", execution.cpuContext().getInstructionLocation()));
        setProgress("Emulation completed", false);
    }

    private void showDevices() {
        for (Device device : computer.getDevices()) {
            if (!appSettings.noGUI && device.isGuiSupported()) {
                device.showGUI(null);
            }
        }
    }

    private CPU.RunState awaitCompletion(EmulationController execution) {
        CompletableFuture<CPU.RunState> completion = execution.runCompletion();
        try {
            return waitForFinishMillis == DONT_WAIT ? completion.get()
                    : completion.get(waitForFinishMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException expected) {
            // CLI observation duration is independent of cooperative lifecycle completion.
            return execution.getRunState();
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            return CPU.RunState.STATE_RUNNING;
        } catch (ExecutionException failure) {
            throw new IllegalStateException("CPU execution failed", failure.getCause());
        }
    }

    private void reportResult(CPU.RunState resultState) {
        switch (resultState) {
            case STATE_STOPPED_ADDR_FALLOUT:
                successful = false;
                LOGGER.error("Address fallout");
                break;
            case STATE_STOPPED_BAD_INSTR:
                successful = false;
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
    }
}
