/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.debugger.DebuggerSession;
import net.emustudio.application.debugger.DebuggerPanel;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.debugtable.DebugTableModel;
import net.emustudio.application.gui.dialogs.StudioFrame;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.gui.framework.DialogsGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ContextPool;
import org.junit.Test;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class UtilsTest extends AbstractSwingTest {

    @Test(timeout = 10000)
    public void mainThreadStartupInitializesDebuggerWithoutReadingMemory() throws Exception {
        assertMainWindowStarts(false);
    }

    @Test(timeout = 10000)
    public void eventThreadStartupInitializesDebuggerWithoutReadingMemory() throws Exception {
        assertMainWindowStarts(true);
    }

    @Test
    public void mainWindowStartupFailureReachesCaller() {
        VirtualComputer computer = mock(VirtualComputer.class);
        IllegalStateException failure = new IllegalStateException("Startup failed");
        when(computer.getComputerConfig()).thenThrow(failure);

        InvocationTargetException thrown = assertThrows(InvocationTargetException.class, () ->
                Utils.showMainWindow(computer, new AppSettings(Config.inMemory(), false, false), mock(DialogsGui.class),
                        mock(DebugTableModel.class), mock(ContextPool.class), null, new EmuStudioGui()));

        assertSame(failure, thrown.getCause());
    }

    @SuppressWarnings("unchecked")
    private void assertMainWindowStarts(boolean fromEdt) throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        ComputerConfig config = mock(ComputerConfig.class);
        CPU cpu = mock(CPU.class);
        MemoryContext<Byte> memory = mock(MemoryContext.class);
        ContextPool contextPool = mock(ContextPool.class);
        DialogsGui dialogs = mock(DialogsGui.class);
        when(config.getName()).thenReturn("Startup test");
        when(computer.getComputerConfig()).thenAnswer(invocation -> {
            assertTrue("Window must be constructed on Swing event thread", SwingUtilities.isEventDispatchThread());
            return config;
        });
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(cpu.getAddressSpaceSize()).thenReturn(8);
        when(memory.getCellTypeClass()).thenReturn(Byte.class);
        when(memory.getSize()).thenReturn(8);
        when(memory.read(anyInt())).thenThrow(new AssertionError("Startup must not copy memory cells"));
        doReturn(memory).when(contextPool).get(Utils.EMUSTUDIO_ID, MemoryContext.class);
        try (DebuggerSession session = new DebuggerSession(cpu, memory, null)) {
            when(computer.getDebugger()).thenReturn(Optional.of(session));
            java.util.concurrent.Callable<Void> start = () -> {
                Utils.showMainWindow(computer, new AppSettings(Config.inMemory(), false, false), dialogs,
                        mock(DebugTableModel.class), contextPool, null, new EmuStudioGui());
                return null;
            };
            if (fromEdt) onEdt(start);
            else start.call();
            StudioFrame frame = waitForWindow(StudioFrame.class, window -> "emuStudio [Startup test]".equals(window.getTitle()));
            DebuggerPanel debugger = findComponent(frame, DebuggerPanel.class, panel -> true);
            try {
                JButton find = findButtonByTooltip(debugger, "Show observed flow from PC");
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
                while (!onEdt(find::isEnabled) && System.nanoTime() < deadline) Thread.sleep(10);
                assertTrue("Initial metadata loading must finish", onEdt(find::isEnabled));
                verify(memory, never()).read(anyInt());
                verify(dialogs, never()).showError(anyString(), anyString());
            } finally {
                runOnEdt(debugger::close);
                runOnEdt(frame::dispose);
            }
        }
    }

    @Test
    public void showSplashScreenDisplaysLoadingDialog() {
        LoadingDialog splash = onEdt(() -> Utils.showSplashScreen(new EmuStudioGui()));
        showDialog(splash);

        assertTrue(onEdt(splash::isShowing));
    }

    @Test
    public void loadComputerConfigFromGuiReturnsEmptyWhenDialogIsClosedWithoutSelection() throws Exception {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);
        DialogsGui dialogs = new DialogsGui(new EmuStudioGui());

        FutureTask<Optional<ComputerConfig>> task = new FutureTask<>(
                () -> Utils.loadComputerConfigFromGui(appSettings, dialogs, new EmuStudioGui())
        );
        Thread thread = new Thread(task, "load-computer-config-test");
        thread.setDaemon(true);
        thread.start();

        JDialog dialog = waitForWindow(JDialog.class, window -> "emuStudio - Open virtual computer".equals(window.getTitle()));
        triggerButton(findButton(dialog, "Exit"));

        assertFalse(task.get(5, TimeUnit.SECONDS).isPresent());
    }
}
