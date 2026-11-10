/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.debugger.DebugTableModel;
import net.emustudio.application.gui.dialogs.EmulatorPanel;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.plugin.memory.MemoryContext;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InOrder;
import org.mockito.ArgumentCaptor;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DialogAndExitActionsTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void aboutActionOpensAboutDialog() throws Exception {
        AboutAction action = new AboutAction(showFrame(onEdt(() -> new JFrame("parent"))), new EmuStudioGui());

        FutureTask<Void> task = startAction(action);
        JDialog dialog = waitForWindow(JDialog.class, window -> "About emuStudio".equals(window.getTitle()));
        runOnEdt(dialog::dispose);

        task.get(5, TimeUnit.SECONDS);
    }

    @Test
    public void viewComputerActionOpensPreviewDialog() throws Exception {
        try (ComputerConfig config = createConfig()) {
            VirtualComputer computer = mock(VirtualComputer.class);
            when(computer.getComputerConfig()).thenReturn(config);
            when(computer.getCompiler()).thenReturn(Optional.empty());
            when(computer.getCPU()).thenReturn(Optional.empty());
            when(computer.getMemory()).thenReturn(Optional.empty());
            when(computer.getDevices()).thenReturn(java.util.Collections.emptyList());

            ViewComputerAction action = new ViewComputerAction(
                    showFrame(onEdt(() -> new JFrame("parent"))),
                    computer,
                    mock(Dialogs.class),
                    new AppSettings(Config.inMemory(), false, false),
                    new EmuStudioGui()
            );

            FutureTask<Void> task = startAction(action);
            JDialog dialog = waitForWindow(JDialog.class, window -> "Computer information preview".equals(window.getTitle()));
            runOnEdt(dialog::dispose);

            task.get(5, TimeUnit.SECONDS);
        }
    }

    @Test
    public void exitActionWaitsForExecutionWithoutBlockingSwing() throws Exception {
        EmulationController controller = mock(EmulationController.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        Runnable dispose = mock(Runnable.class);
        CompletableFuture<Void> stopped = new CompletableFuture<>();
        CountDownLatch exited = new CountDownLatch(1);
        when(controller.closeAsync()).thenReturn(stopped);
        JFrame window = showFrame(onEdt(JFrame::new));
        ExitAction action = new ExitAction(window, () -> true, controller, computer, dispose, exited::countDown);

        runOnEdt(() -> action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "exit")));

        assertFalse(onEdt(action::isEnabled));
        assertFalse(onEdt(window::isEnabled));
        verifyNoInteractions(computer, dispose);

        stopped.complete(null);

        assertTrue(exited.await(5, TimeUnit.SECONDS));
        InOrder shutdown = inOrder(controller, dispose, computer);
        shutdown.verify(controller).closeAsync();
        shutdown.verify(dispose).run();
        shutdown.verify(computer).close();
    }

    @Test
    public void exitActionDoesNothingWhenSaveIsNotConfirmed() {
        EmulationController controller = mock(EmulationController.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        Runnable dispose = mock(Runnable.class);

        new ExitAction(onEdt(JFrame::new), () -> false, controller, computer, dispose).actionPerformed(null);

        verifyNoInteractions(controller, computer, dispose);
    }

    @Test
    public void exitDetachesMemoryListenerBeforePluginDestructionQueuesNotification() throws Exception {
        VirtualComputer computer = mock(VirtualComputer.class);
        MemoryContext<?> memory = mock(MemoryContext.class);
        DebugTableModel model = mock(DebugTableModel.class);
        JFrame window = showFrame(onEdt(JFrame::new));
        EmulatorPanel panel = onEdt(() -> new EmulatorPanel(window, computer, model,
                mock(Dialogs.class), null, memory, position -> {}, new EmuStudioGui()));
        ArgumentCaptor<MemoryContext.MemoryListener> listener =
                ArgumentCaptor.forClass(MemoryContext.MemoryListener.class);
        verify(memory).addMemoryListener(listener.capture());
        clearInvocations(memory, model);
        CountDownLatch exited = new CountDownLatch(1);
        doAnswer(call -> {
            assertFalse("Plugin destruction must not block Swing", SwingUtilities.isEventDispatchThread());
            // A notification already captured by a memory plugin can arrive after listener removal.
            listener.getValue().memoryContentChanged(0, 0);
            return null;
        }).when(computer).close();
        ExitAction action = new ExitAction(window, () -> true, null, computer, () -> {
            assertTrue(SwingUtilities.isEventDispatchThread());
            panel.close();
            window.dispose();
        }, exited::countDown);

        runOnEdt(() -> action.actionPerformed(null));

        assertTrue(exited.await(5, TimeUnit.SECONDS));
        runOnEdt(() -> {});
        verify(memory).removeMemoryListener(listener.getValue());
        verify(model, never()).memoryChanged(anyInt(), anyInt());
    }

    @Test
    public void failedCloseRestoresWindowInputWithoutDisposingComputer() throws Exception {
        EmulationController controller = mock(EmulationController.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        Runnable dispose = mock(Runnable.class);
        Runnable exit = mock(Runnable.class);
        CompletableFuture<Void> stopped = new CompletableFuture<>();
        when(controller.closeAsync()).thenReturn(stopped);
        JFrame window = showFrame(onEdt(JFrame::new));
        ExitAction action = new ExitAction(window, () -> true, controller, computer, dispose, exit);

        runOnEdt(() -> action.actionPerformed(null));
        assertFalse(onEdt(window::isEnabled));
        stopped.completeExceptionally(new IllegalStateException("Could not close execution"));

        JDialog error = waitForWindow(JDialog.class, dialog -> "Exit".equals(dialog.getTitle()));
        assertTrue(onEdt(window::isEnabled));
        assertTrue(onEdt(action::isEnabled));
        runOnEdt(error::dispose);
        verifyNoInteractions(computer, dispose, exit);
    }

    private FutureTask<Void> startAction(Action action) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.actionPerformed(null);
            return null;
        });
        Thread thread = new Thread(task, "dialog-action-test");
        thread.setDaemon(true);
        thread.start();
        return task;
    }

    private ComputerConfig createConfig() throws Exception {
        FileConfig config = FileConfig.of(temporaryFolder.newFile("view-computer-action.toml"));
        config.set("name", "Action computer");
        return new ComputerConfig(config);
    }

}
