/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import net.emustudio.application.emulation.debugger.DebuggerSession;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.Unchecked.RunnableWhichCanThrow;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.plugin.Plugin;
import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.CompilerListener;
import net.emustudio.emulib.plugin.compiler.CompilerMessage;
import net.emustudio.emulib.plugin.cpu.CPU;
import net.emustudio.emulib.plugin.device.Device;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

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
    private final CompletableFuture<Reason> termination = new CompletableFuture<>();
    private final CompletableFuture<Result> completion = new CompletableFuture<>();
    private BiConsumer<String, Boolean> progressListener;

    public enum Reason {
        NORMAL_STOP, BREAKPOINT, ADDRESS_FALLOUT, BAD_INSTRUCTION, CANCELLED, TIMED_OUT, FAILED
    }

    public static final class Result {
        public final Reason reason;
        public final List<Throwable> failures;

        private Result(Reason reason, List<Throwable> failures) {
            this.reason = reason;
            this.failures = List.copyOf(failures);
        }

        public boolean isSuccessful() {
            return failures.isEmpty() && (reason == Reason.NORMAL_STOP
                    || reason == Reason.BREAKPOINT || reason == Reason.TIMED_OUT);
        }
    }

    public Automation(VirtualComputer computer, Path inputFile, AppSettings appSettings,
                      Dialogs dialogs, int waitForFinishMillis, Integer programLocation) throws AutomationException {
        this.computer = Objects.requireNonNull(computer);
        this.appSettings = Objects.requireNonNull(appSettings);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.waitForFinishMillis = waitForFinishMillis;
        this.programLocation = programLocation;

        if (inputFile != null) {
            this.inputFile = inputFile.toFile();
            if (!this.inputFile.exists()) {
                throw new AutomationException("Input file not found");
            }
        } else {
            this.inputFile = null;
        }
    }

    public Result result() {
        return completion.join();
    }

    public synchronized void cancel() {
        termination.complete(Reason.CANCELLED);
    }

    public void setProgressListener(BiConsumer<String, Boolean> listener) {
        progressListener = Objects.requireNonNull(listener);
    }

    /** Executes one session and closes its virtual computer after CPU and plugin work finishes. */
    @Override
    public void run() {
        List<Plugin> plugins = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();
        EmulationController execution = null;
        CompletableFuture<CPU.RunState> cpuCompletion = null;
        boolean interrupted = false;
        try {
            logComputer();
            Compiler compiler = computer.getCompiler().orElse(null);
            if (!termination.isDone() && compiler != null) {
                autoCompile(compiler);
            }
            CPU cpu = computer.getCPU().orElse(null);
            if (!termination.isDone() && cpu != null) {
                execution = computer.getExecution().orElseThrow();
                prepareCpu(execution, compiler);
                List<Device> devices = computer.getDevices();
                if (compiler != null) plugins.add(compiler);
                computer.getMemory().ifPresent(plugins::add);
                plugins.add(cpu);
                plugins.addAll(devices);

                setProgress("Running emulation...", true);
                showDevices(devices);
                awaitPluginStartup(plugins);

                CompletableFuture<CPU.RunState> started = startCpu(execution);
                if (started != null) {
                    started.join();
                    cpuCompletion = execution.runCompletion();
                    cpuCompletion.whenComplete((state, failure) ->
                            termination.complete(failure == null ? reasonFor(state) : Reason.FAILED));
                    awaitTermination();
                }
            }
            termination.complete(Reason.NORMAL_STOP);
        } catch (InterruptedException failure) {
            interrupted = true;
            cancel();
        } catch (Throwable failure) {
            addFailure(failures, failure);
            termination.complete(Reason.FAILED);
        }

        Reason reason = termination.join();
        List<CompletableFuture<?>> stops = new ArrayList<>();
        if (cpuCompletion != null) {
            if (reason == Reason.CANCELLED || reason == Reason.TIMED_OUT) {
                stops.add(execution.stopAutomationRun());
            }
            stops.add(cpuCompletion);
        }
        for (Plugin plugin : plugins) {
            recordFailure(failures, () -> stops.add(plugin.stopAutomation()));
        }
        for (CompletableFuture<?> stop : stops) recordFailure(failures, stop::join);
        if (cpuCompletion != null) {
            EmulationController stopped = execution;
            recordFailure(failures, () -> LOGGER.info("Instruction location = {}",
                    String.format("0x%04X", stopped.getInstructionLocation())));
        }
        recordFailure(failures, () -> computer.closeAndReportFailures().forEach(failure -> addFailure(failures, failure)));

        Result result = new Result(reason, failures);
        try {
            reportResult(result);
        } finally {
            completion.complete(result);
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private synchronized CompletableFuture<CPU.RunState> startCpu(EmulationController execution) {
        if (termination.isDone()) return null;
        if (appSettings.noGUI) {
            computer.getDebugger().ifPresent(DebuggerSession::ignoreBreakpoints);
            LOGGER.info("Ignoring breakpoints in non-interactive automation.");
        }
        return execution.start();
    }

    private void awaitPluginStartup(List<Plugin> plugins)
            throws InterruptedException, ExecutionException {
        List<CompletableFuture<Void>> readiness = new ArrayList<>();
        CompletableFuture<Void> ready = new CompletableFuture<>();
        for (Plugin plugin : plugins) {
            if (termination.isDone() || ready.isDone()) break;
            CompletableFuture<Void> started = plugin.startAutomation();
            readiness.add(started);
            started.exceptionally(failure -> {
                ready.completeExceptionally(failure);
                return null;
            });
        }
        CompletableFuture.allOf(readiness.toArray(new CompletableFuture<?>[0])).thenRun(() -> ready.complete(null));
        CompletableFuture.anyOf(ready, termination).get();
    }

    private void awaitTermination() throws InterruptedException, ExecutionException {
        try {
            if (waitForFinishMillis == DONT_WAIT) termination.get();
            else termination.get(waitForFinishMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException elapsed) {
            termination.complete(Reason.TIMED_OUT);
        }
    }

    private static void recordFailure(List<Throwable> failures, RunnableWhichCanThrow action) {
        try {
            action.run();
        } catch (Throwable failure) {
            addFailure(failures, failure);
        }
    }

    private static void addFailure(List<Throwable> failures, Throwable failure) {
        while ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null) failure = failure.getCause();
        if (!failures.contains(failure)) failures.add(failure);
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

    private void prepareCpu(EmulationController execution, Compiler compiler) {
        setProgress("Resetting CPU...", false);
        if (programLocation != null) {
            setProgress("Program start location: " + String.format("%04Xh", programLocation), false);
        }
        if (inputFile == null || compiler == null) {
            execution.compile(() -> {}, programLocation).join();
        }
    }

    private void setProgress(String msg, boolean stopEnabled) {
        LOGGER.info(msg);
        if (progressListener != null) progressListener.accept(msg, stopEnabled);
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

    private void showDevices(List<Device> devices) {
        if (appSettings.noGUI) return;
        for (Device device : devices) {
            if (termination.isDone()) break;
            if (device.isGuiSupported()) device.showGUI(null);
        }
    }

    private static Reason reasonFor(CPU.RunState state) {
        switch (state) {
            case STATE_STOPPED_ADDR_FALLOUT: return Reason.ADDRESS_FALLOUT;
            case STATE_STOPPED_BAD_INSTR: return Reason.BAD_INSTRUCTION;
            case STATE_STOPPED_BREAK: return Reason.BREAKPOINT;
            case STATE_STOPPED_NORMAL: return Reason.NORMAL_STOP;
            default: return Reason.FAILED;
        }
    }

    private void reportResult(Result result) {
        LOGGER.info("Run result: {}", result.reason);
        for (Throwable failure : result.failures) LOGGER.error("Error during automation", failure);
        setProgress(result.reason == Reason.CANCELLED ? "Emulation cancelled" : "Emulation completed", false);
        if (!result.failures.isEmpty()) {
            dialogs.showError("Error during automation. Please consult log file for details.", "Emulation automation");
        }
    }
}
