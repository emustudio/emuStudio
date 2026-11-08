/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.cpu.brainduck.gui;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ExecutionState;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.cpu.brainduck.BrainCPUContext;
import net.emustudio.plugins.cpu.brainduck.CpuImpl;
import net.emustudio.plugins.cpu.brainduck.MemoryStub;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import java.awt.Component;
import java.awt.Container;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class StatusPanelTest {
    private CpuImpl cpu;
    private StatusPanel panel;
    private java.util.function.Consumer<ExecutionState.Snapshot> observer;

    @Before
    public void setUp() throws Exception {
        MemoryStub memory = new MemoryStub();
        ContextPool contextPool = createNiceMock(ContextPool.class);
        expect(contextPool.get(0, ByteMemoryContext.class)).andReturn(memory).anyTimes();
        expect(contextPool.getMemory(0, Byte.class)).andReturn(memory).anyTimes();
        replay(contextPool);
        ExecutionState execution = createNiceMock(ExecutionState.class);
        expect(execution.subscribe(anyObject())).andAnswer(() -> {
            observer = getCurrentArgument(0);
            return () -> {};
        }).anyTimes();
        replay(execution);
        ApplicationApi application = createNiceMock(ApplicationApi.class);
        expect(application.getExecutionState()).andReturn(execution).anyTimes();
        expect(application.getContextPool()).andReturn(contextPool).anyTimes();
        replay(application);
        cpu = new CpuImpl(0L, application, PluginSettings.UNAVAILABLE);
        cpu.initialize();
        GUI gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArguments()[0])).anyTimes();
        replay(gui);
        panel = new StatusPanel(memory, cpu, gui, execution);
    }

    @After
    public void tearDown() {
        cpu.destroy();
    }

    @Test
    public void hostStateChangesReachStatusPanel() throws Exception {
        publish(CPU.RunState.STATE_RUNNING);
        SwingUtilities.invokeAndWait(() -> assertTrue(containsText(panel, "running")));

        publish(CPU.RunState.STATE_STOPPED_BREAK);
        SwingUtilities.invokeAndWait(() -> assertTrue(containsText(panel, "breakpoint")));
    }

    @Test
    public void hostInternalChangeRefreshesInstructionPosition() throws Exception {
        cpu.setInstructionLocation(12);

        publish(CPU.RunState.STATE_STOPPED_BREAK);

        SwingUtilities.invokeAndWait(() -> assertTrue(containsText(panel, "000C")));
    }

    @Test
    public void addressFalloutStillRendersUnreachableMemory() throws Exception {
        cpu.getEngine().P = -1;

        publish(CPU.RunState.STATE_STOPPED_ADDR_FALLOUT);

        SwingUtilities.invokeAndWait(() -> {
            assertTrue(containsText(panel, "stopped (address fallout)"));
            assertTrue(containsText(panel, "[unreachable]"));
        });
    }

    private void publish(CPU.RunState state) {
        observer.accept(new ExecutionState.Snapshot(state, false, false, cpu.getInstructionLocation(), Float.NaN));
    }

    private static boolean containsText(Component component, String text) {
        if (component instanceof JLabel && text.equals(((JLabel) component).getText())) return true;
        if (component instanceof JTextField && text.equals(((JTextField) component).getText())) return true;
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                if (containsText(child, text)) return true;
            }
        }
        return false;
    }
}
