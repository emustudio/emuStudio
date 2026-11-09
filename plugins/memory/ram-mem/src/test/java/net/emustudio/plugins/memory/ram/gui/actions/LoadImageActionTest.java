/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ram.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.ram.MemoryContextImpl;
import net.emustudio.plugins.memory.ram.TestRamMemoryContextFactory;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class LoadImageActionTest {

    private MemoryContextImpl context;
    private Dialogs dialogs;

    @Before
    public void setUp() {
        context = TestRamMemoryContextFactory.create();
        dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDialogsThrows() {
        new LoadImageAction(null, context, () -> {});
    }

    @Test(expected = NullPointerException.class)
    public void testNullContextThrows() {
        new LoadImageAction(dialogs, null, () -> {});
    }

    @Test(expected = NullPointerException.class)
    public void testNullRepaintThrows() {
        new LoadImageAction(dialogs, context, null);
    }

    @Test
    public void testConstructor() {
        LoadImageAction action = new LoadImageAction(dialogs, context, () -> {});
        assertNotNull(action);
    }
}
