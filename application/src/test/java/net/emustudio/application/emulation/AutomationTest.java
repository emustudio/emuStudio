/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.compiler.Compiler;
import net.emustudio.emulib.plugins.compiler.CompilerListener;
import net.emustudio.emulib.plugins.compiler.CompilerMessage;
import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AutomationTest {
    private static final AppSettings NO_GUI_SETTINGS = new AppSettings(Config.inMemory(), true, false);

    private Compiler compiler;
    private CPU cpu;
    private Dialogs dialogs;
    private Path input;
    private final List<EmulationController> executions = new ArrayList<>();

    @Before
    public void setUp() throws Exception {
        compiler = mock(Compiler.class);
        cpu = mock(CPU.class);
        when(cpu.execute()).thenReturn(CPU.RunState.STATE_STOPPED_NORMAL);
        dialogs = mock(Dialogs.class);
        input = Files.createTempFile("automation", ".asm");
    }

    @After
    public void tearDown() throws Exception {
        for (EmulationController execution : executions) execution.close();
        Files.deleteIfExists(input);
    }

    @Test(expected = AutomationException.class)
    public void constructorRejectsMissingInputFile() throws Exception {
        new Automation(
                mockComputer(), Path.of("missing-" + System.nanoTime() + ".asm"),
                NO_GUI_SETTINGS, dialogs, 10, null, EmulationProgress.NONE
        );
    }

    @Test
    public void runReportsCompilationFailureAndSkipsCpuExecution() throws Exception {
        stubCompilation(CompilerMessage.MessageType.TYPE_ERROR, "Broken source");

        new Automation(mockComputer(compiler, cpu), input, NO_GUI_SETTINGS, dialogs, 50, null, EmulationProgress.NONE).run();

        verify(cpu, never()).reset();
        verify(cpu, never()).execute();
        verify(dialogs).showError("Error during automation. Please consult log file for details.", "Emulation automation");
    }

    @Test
    public void runCompilesResetsAtProgramLocationAndWaitsForStopState() throws Exception {
        stubCompilation(CompilerMessage.MessageType.TYPE_INFO, "Compiled");
        stubCpuExecution(CPU.RunState.STATE_STOPPED_NORMAL);
        when(cpu.getInstructionLocation()).thenReturn(0x1234);
        Device device = mock(Device.class);

        new Automation(
                mockComputer(compiler, cpu, mock(Memory.class), Collections.singletonList(device)),
                input, NO_GUI_SETTINGS, dialogs, 200, 0x20, EmulationProgress.NONE
        ).run();

        verify(cpu).reset(0x20);
        verify(cpu).execute();
        verify(device, never()).showGUI(null);
        verify(dialogs, never()).showError(anyString(), anyString());
    }

    @Test
    public void runAcceptsWarningsAndUsesDefaultReset() throws Exception {
        stubCompilation(CompilerMessage.MessageType.TYPE_WARNING, "Careful");
        stubCpuExecution(CPU.RunState.STATE_STOPPED_BREAK);
        when(cpu.getInstructionLocation()).thenReturn(0x0088);

        new Automation(mockComputer(compiler, cpu), input, NO_GUI_SETTINGS, dialogs, 200, null, EmulationProgress.NONE).run();

        verify(cpu).reset();
        verify(cpu, never()).reset(anyInt());
        verify(cpu).execute();
        verify(dialogs, never()).showError(anyString(), anyString());
    }

    @Test
    public void runWithoutInputSkipsCompilation() throws Exception {
        stubCpuExecution(CPU.RunState.STATE_STOPPED_NORMAL);
        when(cpu.getInstructionLocation()).thenReturn(0x0042);
        Compiler unusedCompiler = mock(Compiler.class);

        new Automation(mockComputer(unusedCompiler, cpu), null, NO_GUI_SETTINGS, dialogs, 20, null, EmulationProgress.NONE).run();

        verify(cpu).reset();
        verify(cpu).execute();
        verify(unusedCompiler, never()).compile(any(), any());
    }

    @Test
    public void interruptedWaitRestoresThreadInterruptFlag() throws Exception {
        EmulationControllerTest.RunGate gate = new EmulationControllerTest.RunGate(CPU.RunState.STATE_RUNNING);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        when(cpu.execute()).thenAnswer(inv -> gate.run());
        when(cpu.getInstructionLocation()).thenReturn(0);

        Automation automation = new Automation(mockComputer(cpu), null, NO_GUI_SETTINGS, dialogs, Automation.DONT_WAIT, null, EmulationProgress.NONE);

        Thread thread = new Thread(() -> {
            automation.run();
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        try {
            thread.start();
            gate.awaitEntered();
            thread.interrupt();
            thread.join(1000);

            assertTrue(interrupted.get());
        } finally {
            gate.release.countDown();
            thread.join(1000);
        }
    }

    @Test
    public void runUsesProgressDialogWhenPresent() throws Exception {
        when(cpu.getInstructionLocation()).thenReturn(0x0010);
        EmulationProgress progress = mock(EmulationProgress.class);

        Automation automation = new Automation(mockComputer(cpu), null, NO_GUI_SETTINGS, dialogs, 10, null, progress);

        automation.run();

        verify(progress).show();
        verify(progress).setAction("Resetting CPU...", false);
        verify(progress).setAction("Running emulation...", true);
        verify(progress).setAction("Emulation completed", false);
        verify(progress).dispose();
    }

    @Test
    public void runReturnsWhenCpuStopsBeforeWaitBegins() throws Exception {
        stubCompilation(CompilerMessage.MessageType.TYPE_INFO, "Compiled");
        stubCpuExecutionImmediately(CPU.RunState.STATE_STOPPED_NORMAL);
        when(cpu.getInstructionLocation()).thenReturn(0x0042);

        new Automation(mockComputer(compiler, cpu), input, NO_GUI_SETTINGS, dialogs, Automation.DONT_WAIT, null, EmulationProgress.NONE).run();

        verify(cpu).execute();
        verify(dialogs, never()).showError(anyString(), anyString());
    }

    private void stubCompilation(CompilerMessage.MessageType messageType, String message) throws Exception {
        AtomicReference<CompilerListener> listener = new AtomicReference<>();
        doAnswer(inv -> { listener.set(inv.getArgument(0)); return null; })
                .when(compiler).addCompilerListener(any());
        doAnswer(inv -> {
            listener.get().onStart();
            listener.get().onMessage(new CompilerMessage(messageType, message, SourceCodePosition.of(1, 1, input.toString())));
            listener.get().onFinish();
            return null;
        }).when(compiler).compile(eq(input), eq(Optional.empty()));
    }

    private void stubCpuExecution(CPU.RunState stopState) throws Exception {
        when(cpu.execute()).thenReturn(stopState);
    }

    private void stubCpuExecutionImmediately(CPU.RunState stopState) throws Exception {
        when(cpu.execute()).thenReturn(stopState);
    }

    private VirtualComputer mockComputer(Compiler compiler, CPU cpu, Memory memory, java.util.List<Device> devices) {
        VirtualComputer c = mock(VirtualComputer.class);
        ComputerConfig config = mock(ComputerConfig.class);
        when(config.getName()).thenReturn("Test computer");
        when(c.getComputerConfig()).thenReturn(config);
        when(c.getCompiler()).thenReturn(Optional.ofNullable(compiler));
        when(c.getCPU()).thenReturn(Optional.ofNullable(cpu));
        when(c.getMemory()).thenReturn(Optional.ofNullable(memory));
        when(c.getDevices()).thenReturn(devices);
        if (cpu == null) {
            when(c.getExecution()).thenReturn(Optional.empty());
        } else {
            EmulationController execution = new EmulationController(cpu, memory, devices, null);
            executions.add(execution);
            when(c.getExecution()).thenReturn(Optional.of(execution));
        }
        return c;
    }

    private VirtualComputer mockComputer(Compiler compiler, CPU cpu) {
        return mockComputer(compiler, cpu, null, Collections.emptyList());
    }

    private VirtualComputer mockComputer(CPU cpu) {
        return mockComputer(null, cpu, null, Collections.emptyList());
    }

    private VirtualComputer mockComputer() {
        return mockComputer(null, null, null, Collections.emptyList());
    }
}
