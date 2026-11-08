/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

public class ApplicationApiImplTest {

    @Test
    public void gettersAndProgramLocationExposeInjectedState() {
        ContextPool contextPool = mock(ContextPool.class);
        Dialogs dialogs = mock(Dialogs.class);
        GUI gui = mock(GUI.class);

        ApplicationApiImpl api = new ApplicationApiImpl(contextPool, dialogs, gui);

        assertSame(contextPool, api.getContextPool());
        assertSame(dialogs, api.getDialogs());
        assertSame(gui, api.getGUI());

        api.setProgramLocation(1234);
        assertEquals(1234, api.getProgramLocation());
    }

    @Test
    public void guiMayBeNullInHeadlessMode() {
        ApplicationApiImpl api = new ApplicationApiImpl(
                mock(ContextPool.class),
                mock(Dialogs.class),
                null
        );

        assertNull(api.getGUI());
    }
}
