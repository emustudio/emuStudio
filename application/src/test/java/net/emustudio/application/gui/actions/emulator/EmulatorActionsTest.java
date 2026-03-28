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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class EmulatorActionsTest {

    @Test
    public void jumpActionHandlesMissingCpuInvalidAddressAndSuccessfulJump() {
        Dialogs dialogs = mock(Dialogs.class);
        AtomicInteger refreshCount = new AtomicInteger();

        VirtualComputer noCpuComputer = mock(VirtualComputer.class);
        when(noCpuComputer.getCPU()).thenReturn(Optional.empty());
        new JumpAction(noCpuComputer, dialogs, refreshCount::incrementAndGet).actionPerformed(null);
        verify(dialogs).showInfo("CPU is not set", "Jump to address");

        reset(dialogs);
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenReturn(Optional.of(42));
        when(cpu.setInstructionLocation(42)).thenReturn(false);
        new JumpAction(computer, dialogs, refreshCount::incrementAndGet).actionPerformed(null);
        verify(dialogs).showError("Invalid memory address (please check memory size)");

        reset(dialogs, cpu);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenThrow(new NumberFormatException("bad"));
        new JumpAction(computer, dialogs, refreshCount::incrementAndGet).actionPerformed(null);
        verify(dialogs).showError("Invalid address format", "Jump to address");

        reset(dialogs, cpu);
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(dialogs.readInteger("Memory address:", "Jump to address", 0)).thenReturn(Optional.of(7));
        when(cpu.setInstructionLocation(7)).thenReturn(true);
        new JumpAction(computer, dialogs, refreshCount::incrementAndGet).actionPerformed(null);
        verify(cpu).setInstructionLocation(7);
        assertTrue(refreshCount.get() > 0);
    }

    @Test
    public void jumpToBeginningAndStepBackMoveProgramCounterOnlyWhenApplicable() {
        CPU cpu = mock(CPU.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        DebugTableModel debugTableModel = mock(DebugTableModel.class);
        AtomicInteger refreshCount = new AtomicInteger();

        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        new JumpToBeginningAction(computer, refreshCount::incrementAndGet).actionPerformed(null);
        verify(cpu).setInstructionLocation(0);
        assertEquals(1, refreshCount.get());

        when(cpu.getInstructionLocation()).thenReturn(10, 0);
        when(debugTableModel.guessPreviousInstructionLocation()).thenReturn(6);
        StepBackAction action = new StepBackAction(computer, debugTableModel, refreshCount::incrementAndGet);
        action.actionPerformed(null);
        action.actionPerformed(null);

        verify(cpu).setInstructionLocation(6);
        verify(debugTableModel).guessPreviousInstructionLocation();
        assertEquals(2, refreshCount.get());
    }

    @Test
    public void runPauseResetStepAndStopActionsDelegateToController() {
        EmulationController controller = mock(EmulationController.class);
        JTable debugTable = new JTable();
        AtomicInteger statusUpdates = new AtomicInteger();

        RunAction runAction = new RunAction(controller, debugTable);
        runAction.actionPerformed(null);
        assertFalse(debugTable.isEnabled());
        verify(controller).start();

        when(controller.isTimedRunning()).thenReturn(false, true);
        PauseAction pauseAction = new PauseAction(controller, statusUpdates::incrementAndGet);
        pauseAction.actionPerformed(null);
        pauseAction.actionPerformed(null);
        verify(controller, times(2)).pause();
        assertEquals(1, statusUpdates.get());

        new ResetAction(controller).actionPerformed(null);
        new StepAction(controller).actionPerformed(null);
        new StopAction(controller).actionPerformed(null);

        verify(controller).reset();
        verify(controller).step();
        verify(controller).stop();
    }

    @Test
    public void runTimedActionStartsTimedRunOrShowsNumberFormatError() {
        EmulationController controller = mock(EmulationController.class);
        Dialogs dialogs = mock(Dialogs.class);

        when(dialogs.readInteger("Enter time slice in milliseconds:", "Timed emulation", 500))
                .thenReturn(Optional.of(250));
        new RunTimedAction(controller, dialogs).actionPerformed(null);
        verify(controller).step(250, TimeUnit.MILLISECONDS);

        reset(controller, dialogs);
        when(dialogs.readInteger("Enter time slice in milliseconds:", "Timed emulation", 500))
                .thenThrow(new NumberFormatException("bad"));
        new RunTimedAction(controller, dialogs).actionPerformed(null);
        verify(dialogs).showError("Invalid number format", "Timed emulation");
    }

    @Test
    public void deviceActionsRequireSelectionAndHandleRuntimeErrors() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        Device device = mock(Device.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getDevices()).thenReturn(Collections.singletonList(device));

        ShowDeviceGuiAction missingSelectionGui = new ShowDeviceGuiAction(parent, computer, dialogs, () -> -1);
        missingSelectionGui.actionPerformed(null);
        assertFalse(missingSelectionGui.isEnabled());
        verify(dialogs).showError("Device has to be selected!", "Show device");

        reset(dialogs, device);
        ShowDeviceSettingsAction missingSelectionSettings = new ShowDeviceSettingsAction(parent, computer, dialogs, () -> -1);
        missingSelectionSettings.actionPerformed(null);
        assertFalse(missingSelectionSettings.isEnabled());
        verify(dialogs).showError("Device has to be selected!", "Show device settings");

        reset(dialogs, device);
        ShowDeviceGuiAction guiAction = new ShowDeviceGuiAction(parent, computer, dialogs, () -> 0);
        guiAction.actionPerformed(null);
        assertTrue(guiAction.isEnabled());
        verify(device).showGUI(parent);

        reset(dialogs, device);
        doThrow(new IllegalStateException("boom")).when(device).showSettings(parent);
        ShowDeviceSettingsAction settingsAction = new ShowDeviceSettingsAction(parent, computer, dialogs, () -> 0);
        settingsAction.actionPerformed(null);
        verify(dialogs).showError("Unexpected error. Please see log file for details", "Show device settings");
    }

    @Test
    public void showMemoryActionUsesSupportFlagAndDisplaysFallbackMessage() {
        JFrame parent = new JFrame();
        Dialogs dialogs = mock(Dialogs.class);
        Memory supportedMemory = mock(Memory.class);
        VirtualComputer supportedComputer = mock(VirtualComputer.class);
        when(supportedComputer.getMemory()).thenReturn(Optional.of(supportedMemory));
        when(supportedMemory.isShowSettingsSupported()).thenReturn(true);

        new ShowMemoryAction(parent, supportedComputer, dialogs).actionPerformed(null);
        verify(supportedMemory).showSettings(parent);

        reset(dialogs);
        Memory unsupportedMemory = mock(Memory.class);
        VirtualComputer unsupportedComputer = mock(VirtualComputer.class);
        when(unsupportedComputer.getMemory()).thenReturn(Optional.of(unsupportedMemory));
        when(unsupportedMemory.isShowSettingsSupported()).thenReturn(false);

        new ShowMemoryAction(parent, unsupportedComputer, dialogs).actionPerformed(null);
        verify(dialogs).showInfo("Memory GUI is not supported", "Show Memory");
    }
}
