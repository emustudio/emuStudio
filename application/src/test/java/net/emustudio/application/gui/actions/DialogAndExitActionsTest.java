/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.actions;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.application.emulation.EmulationController;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.security.Permission;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
    public void exitActionClosesResourcesAndRequestsExitWhenConfirmed() {
        EmulationController controller = mock(EmulationController.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        Runnable dispose = mock(Runnable.class);
        ExitAction action = new ExitAction(() -> true, controller, computer, dispose);

        ExitInterceptingSecurityManager securityManager = new ExitInterceptingSecurityManager();
        SecurityManager previous = System.getSecurityManager();
        System.setSecurityManager(securityManager);
        try {
            try {
                action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "exit"));
            } catch (ExitInterceptedException ignored) {
            }
        } finally {
            System.setSecurityManager(previous);
        }

        assertEquals(Integer.valueOf(0), securityManager.status);
        verify(controller).close();
        verify(computer).close();
        verify(dispose).run();
    }

    @Test
    public void exitActionDoesNothingWhenSaveIsNotConfirmed() {
        EmulationController controller = mock(EmulationController.class);
        VirtualComputer computer = mock(VirtualComputer.class);
        Runnable dispose = mock(Runnable.class);

        new ExitAction(() -> false, controller, computer, dispose).actionPerformed(null);

        verifyNoInteractions(controller, computer, dispose);
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

    private static final class ExitInterceptedException extends SecurityException {
        private static final long serialVersionUID = 1L;
    }

    private static final class ExitInterceptingSecurityManager extends SecurityManager {
        private Integer status;

        @Override
        public void checkPermission(Permission perm) {
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
        }

        @Override
        public void checkExit(int status) {
            this.status = status;
            throw new ExitInterceptedException();
        }
    }
}
