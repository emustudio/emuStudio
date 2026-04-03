/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions.emulator;

import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.plugins.memory.Memory;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Test;

import javax.swing.*;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EmulatorActionsTest {

    @Test
    public void jumpActionShowsInfoWhenCpuIsMissing() {
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCPU()).thenReturn(Optional.empty());

        new JumpAction(computer, dialogs, () -> {}).actionPerformed(null);

        verify(dialogs).showInfo("CPU is not set", "Jump to address");
    }

    @Test
    public void jumpActionShowsErrorOnInvalidAddress() {
        Dialogs dialogs = mock(Dialogs.class);
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenReturn(Optional.of(42));
        when(cpu.setInstructionLocation(42)).thenReturn(false);

        new JumpAction(computer, dialogs, () -> {}).actionPerformed(null);

        verify(dialogs).showError("Invalid memory address (please check memory size)");
    }

    @Test
    public void jumpActionShowsErrorOnBadNumberFormat() {
        Dialogs dialogs = mock(Dialogs.class);
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenThrow(new NumberFormatException("bad"));

        new JumpAction(computer, dialogs, () -> {}).actionPerformed(null);

        verify(dialogs).showError("Invalid address format", "Jump to address");
    }

    @Test
    public void jumpActionJumpsSuccessfully() {
        Dialogs dialogs = mock(Dialogs.class);
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        AtomicInteger refreshCount = new AtomicInteger();
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenReturn(Optional.of(7));
        when(cpu.setInstructionLocation(7)).thenReturn(true);

        new JumpAction(computer, dialogs, refreshCount::incrementAndGet).actionPerformed(null);

        verify(cpu).setInstructionLocation(7);
        assertTrue(refreshCount.get() > 0);
    }

    @Test
    public void jumpToBeginningSetsProgramCounterToZero() {
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        AtomicInteger refreshCount = new AtomicInteger();
        when(computer.getCPU()).thenReturn(Optional.of(cpu));

        new JumpToBeginningAction(computer, refreshCount::incrementAndGet).actionPerformed(null);

        verify(cpu).setInstructionLocation(0);
        assertEquals(1, refreshCount.get());
    }

    @Test
    public void stepBackUsesDebugTableToGuessPreviousLocation() {
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        DebugTableModel debugTableModel = mock(DebugTableModel.class);
        AtomicInteger refreshCount = new AtomicInteger();
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(cpu.getInstructionLocation()).thenReturn(10);
        when(debugTableModel.guessPreviousInstructionLocation()).thenReturn(6);

        new StepBackAction(computer, debugTableModel, refreshCount::incrementAndGet).actionPerformed(null);

        verify(cpu).setInstructionLocation(6);
    }

    @Test
    public void stepBackDoesNothingAtLocationZero() {
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        DebugTableModel debugTableModel = mock(DebugTableModel.class);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(cpu.getInstructionLocation()).thenReturn(0);

        new StepBackAction(computer, debugTableModel, () -> {}).actionPerformed(null);

        verify(debugTableModel, never()).guessPreviousInstructionLocation();
    }

    @Test
    public void runActionDelegatesToControllerAndDisablesDebugTable() {
        EmulationController controller = mock(EmulationController.class);
        JTable debugTable = new JTable();

        new RunAction(controller, debugTable).actionPerformed(null);

        assertFalse(debugTable.isEnabled());
        verify(controller).start();
    }

    @Test
    public void pauseActionDelegatesToController() {
        EmulationController controller = mock(EmulationController.class);
        AtomicInteger statusUpdates = new AtomicInteger();
        when(controller.isTimedRunning()).thenReturn(false);

        new PauseAction(controller, statusUpdates::incrementAndGet).actionPerformed(null);

        verify(controller).pause();
    }

    @Test
    public void pauseActionUpdatesStatusWhenTimedRunning() {
        EmulationController controller = mock(EmulationController.class);
        AtomicInteger statusUpdates = new AtomicInteger();
        when(controller.isTimedRunning()).thenReturn(true);

        new PauseAction(controller, statusUpdates::incrementAndGet).actionPerformed(null);

        verify(controller).pause();
        assertEquals(1, statusUpdates.get());
    }

    @Test
    public void resetStepStopActionsDelegateToController() {
        EmulationController controller = mock(EmulationController.class);

        new ResetAction(controller).actionPerformed(null);
        new StepAction(controller).actionPerformed(null);
        new StopAction(controller).actionPerformed(null);

        verify(controller).reset();
        verify(controller).step();
        verify(controller).stop();
    }

    @Test
    public void runTimedActionStartsTimedRun() {
        EmulationController controller = mock(EmulationController.class);
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.readInteger("Enter time slice in milliseconds:", "Timed emulation", 500))
                .thenReturn(Optional.of(250));

        new RunTimedAction(controller, dialogs).actionPerformed(null);

        verify(controller).step(250, TimeUnit.MILLISECONDS);
    }

    @Test
    public void runTimedActionShowsErrorOnBadFormat() {
        EmulationController controller = mock(EmulationController.class);
        Dialogs dialogs = mock(Dialogs.class);
        when(dialogs.readInteger("Enter time slice in milliseconds:", "Timed emulation", 500))
                .thenThrow(new NumberFormatException("bad"));

        new RunTimedAction(controller, dialogs).actionPerformed(null);

        verify(dialogs).showError("Invalid number format", "Timed emulation");
    }

    @Test
    public void showDeviceGuiRequiresSelection() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getDevices()).thenReturn(Collections.singletonList(mock(Device.class)));

        ShowDeviceGuiAction action = new ShowDeviceGuiAction(parent, computer, dialogs, () -> -1);
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
        verify(dialogs).showError("Device has to be selected!", "Show device");
    }

    @Test
    public void showDeviceSettingsRequiresSelection() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getDevices()).thenReturn(Collections.singletonList(mock(Device.class)));

        ShowDeviceSettingsAction action = new ShowDeviceSettingsAction(parent, computer, dialogs, () -> -1);
        action.actionPerformed(null);

        assertFalse(action.isEnabled());
        verify(dialogs).showError("Device has to be selected!", "Show device settings");
    }

    @Test
    public void showDeviceGuiShowsGuiWhenSelected() {
        JFrame parent = new JFrame();
        Device device = mock(Device.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getDevices()).thenReturn(Collections.singletonList(device));

        ShowDeviceGuiAction action = new ShowDeviceGuiAction(parent, computer, mock(Dialogs.class), () -> 0);
        action.actionPerformed(null);

        assertTrue(action.isEnabled());
        verify(device).showGUI(parent);
    }

    @Test
    public void showDeviceSettingsHandlesRuntimeError() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        Device device = mock(Device.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getDevices()).thenReturn(Collections.singletonList(device));
        doThrow(new IllegalStateException("boom")).when(device).showSettings(parent);

        new ShowDeviceSettingsAction(parent, computer, dialogs, () -> 0).actionPerformed(null);

        verify(dialogs).showError("Unexpected error. Please see log file for details", "Show device settings");
    }

    @Test
    public void showMemoryActionShowsSettingsWhenSupported() {
        JFrame parent = new JFrame();
        Memory memory = mock(Memory.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(memory.isShowSettingsSupported()).thenReturn(true);

        new ShowMemoryAction(parent, computer, mock(Dialogs.class)).actionPerformed(null);

        verify(memory).showSettings(parent);
    }

    @Test
    public void showMemoryActionShowsFallbackWhenUnsupported() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        Memory memory = mock(Memory.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getMemory()).thenReturn(Optional.of(memory));
        when(memory.isShowSettingsSupported()).thenReturn(false);

        new ShowMemoryAction(parent, computer, dialogs).actionPerformed(null);

        verify(dialogs).showInfo("Memory GUI is not supported", "Show Memory");
    }
}
