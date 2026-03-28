/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.cmdline;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import net.emustudio.application.gui.dialogs.LoadingDialog;
import net.emustudio.application.gui.framework.GuiDialogsImpl;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import org.junit.Test;

import javax.swing.*;
import java.util.Optional;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest extends AbstractSwingTest {

    @Test
    public void showSplashScreenDisplaysLoadingDialog() {
        LoadingDialog splash = onEdt(() -> Utils.showSplashScreen(new GUIImpl()));
        showDialog(splash);

        assertTrue(onEdt(splash::isShowing));
    }

    @Test
    public void loadComputerConfigFromGuiReturnsEmptyWhenDialogIsClosedWithoutSelection() throws Exception {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);
        GuiDialogsImpl dialogs = new GuiDialogsImpl(new GUIImpl());

        FutureTask<Optional<ComputerConfig>> task = new FutureTask<>(
                () -> Utils.loadComputerConfigFromGui(appSettings, dialogs, new GUIImpl())
        );
        Thread thread = new Thread(task, "load-computer-config-test");
        thread.setDaemon(true);
        thread.start();

        JDialog dialog = waitForWindow(JDialog.class, window -> "emuStudio - Open virtual computer".equals(window.getTitle()));
        triggerButton(findButton(dialog, "Exit"));

        assertFalse(task.get(5, TimeUnit.SECONDS).isPresent());
    }
}
