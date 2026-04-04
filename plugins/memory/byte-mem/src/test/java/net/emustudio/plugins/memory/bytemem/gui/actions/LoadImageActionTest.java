/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Test;
import javax.swing.*;
import static org.easymock.EasyMock.*;
public class LoadImageActionTest {
    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        ByteMemoryContext ctx = createNiceMock(ByteMemoryContext.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(ctx, parent);
        new LoadImageAction(null, ctx, parent, () -> {}, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new LoadImageAction(dialogs, null, parent, () -> {}, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullParentThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        ByteMemoryContext ctx = createNiceMock(ByteMemoryContext.class);
        replay(dialogs, ctx);
        new LoadImageAction(dialogs, ctx, null, () -> {}, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullRepaintThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        ByteMemoryContext ctx = createNiceMock(ByteMemoryContext.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, ctx, parent);
        new LoadImageAction(dialogs, ctx, parent, null, null);
    }
    @Test
    public void testConstructorAcceptsNullGUI() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        ByteMemoryContext ctx = createNiceMock(ByteMemoryContext.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, ctx, parent);
        // Should not throw
        new LoadImageAction(dialogs, ctx, parent, () -> {}, null);
    }
}
