/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.dialogs.AutoDialog;
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
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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

    @Before
    public void setUp() throws Exception {
        compiler = mock(Compiler.class);
        cpu = mock(CPU.class);
        dialogs = mock(Dialogs.class);
        input = Files.createTempFile("automation", ".asm");
    }

    @Test(expected = AutomationException.class)
    public void constructorRejectsMissingInputFile() throws Exception {
        new Automation(
                mockComputer(), Path.of("missing-" + System.nanoTime() + ".asm"),
                NO_GUI_SETTINGS, dialogs, 10, Optional.empty(), null
        );
    }

    @Test
    public void runReportsCompilationFailureAndSkipsCpuExecution() throws Exception {
        stubCompilation(CompilerMessage.MessageType.TYPE_ERROR, "Broken source");

        new Automation(mockComputer(compiler, cpu), input, NO_GUI_SETTINGS, dialogs, 50, Optional.empty(), null).run();

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
                input, NO_GUI_SETTINGS, dialogs, 200, Optional.of(0x20), null
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

        new Automation(mockComputer(compiler, cpu), input, NO_GUI_SETTINGS, dialogs, 200, Optional.empty(), null).run();

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

        new Automation(mockComputer(unusedCompiler, cpu), null, NO_GUI_SETTINGS, dialogs, 20, Optional.empty(), null).run();

        verify(cpu).reset();
        verify(cpu).execute();
        verify(unusedCompiler, never()).compile(any(), any());
    }

    @Test
    public void interruptedWaitRestoresThreadInterruptFlag() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);

        doAnswer(inv -> { inv.<CPU.CPUListener>getArgument(0); return null; }).when(cpu).addCPUListener(any());
        doAnswer(inv -> { started.countDown(); return null; }).when(cpu).execute();
        when(cpu.getInstructionLocation()).thenReturn(0);

        Automation automation = new Automation(mockComputer(cpu), null, NO_GUI_SETTINGS, dialogs, Automation.DONT_WAIT, Optional.empty(), null);

        Thread thread = new Thread(() -> {
            automation.run();
            interrupted.set(Thread.currentThread().isInterrupted());
        });
        thread.start();
        assertTrue(started.await(1, TimeUnit.SECONDS));
        thread.interrupt();
        thread.join(1000);

        assertTrue(interrupted.get());
    }

    @Test
    public void runUsesProgressDialogWhenPresent() throws Exception {
        when(cpu.getInstructionLocation()).thenReturn(0x0010);
        AutoDialog progressDialog = mock(AutoDialog.class);

        Automation automation = new Automation(mockComputer(cpu), null, NO_GUI_SETTINGS, dialogs, 10, Optional.empty(), null);
        automation.progressGUI = progressDialog;

        automation.run();

        verify(progressDialog).setVisible(true);
        verify(progressDialog).setAction("Resetting CPU...", false);
        verify(progressDialog).setAction("Running emulation...", true);
        verify(progressDialog).setAction("Emulation completed", false);
        verify(progressDialog).dispose();
    }

    // --- Helpers ---

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

    private void stubCpuExecution(CPU.RunState stopState) {
        AtomicReference<CPU.CPUListener> listener = new AtomicReference<>();
        doAnswer(inv -> { listener.set(inv.getArgument(0)); return null; })
                .when(cpu).addCPUListener(any());
        doAnswer(inv -> {
            new Thread(() -> {
                try { Thread.sleep(25); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
                listener.get().runStateChanged(stopState);
            }).start();
            return null;
        }).when(cpu).execute();
    }

    private static VirtualComputer mockComputer(Compiler compiler, CPU cpu, Memory memory, java.util.List<Device> devices) {
        VirtualComputer c = mock(VirtualComputer.class);
        ComputerConfig config = mock(ComputerConfig.class);
        when(config.getName()).thenReturn("Test computer");
        when(c.getComputerConfig()).thenReturn(config);
        when(c.getCompiler()).thenReturn(Optional.ofNullable(compiler));
        when(c.getCPU()).thenReturn(Optional.ofNullable(cpu));
        when(c.getMemory()).thenReturn(Optional.ofNullable(memory));
        when(c.getDevices()).thenReturn(devices);
        return c;
    }

    private static VirtualComputer mockComputer(Compiler compiler, CPU cpu) {
        return mockComputer(compiler, cpu, null, Collections.emptyList());
    }

    private static VirtualComputer mockComputer(CPU cpu) {
        return mockComputer(null, cpu, null, Collections.emptyList());
    }

    private static VirtualComputer mockComputer() {
        return mockComputer(null, null, null, Collections.emptyList());
    }
}
