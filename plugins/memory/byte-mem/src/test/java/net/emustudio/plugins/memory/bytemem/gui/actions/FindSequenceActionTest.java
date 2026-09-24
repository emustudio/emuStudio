/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;
import javax.swing.*;
import static org.easymock.EasyMock.*;
public class FindSequenceActionTest {
    private MemoryTableModel tableModel;
    @Before
    public void setUp() {
        tableModel = new MemoryTableModel(TestMemoryContextFactory.create(256, 1, 0));
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        JDialog parent = createNiceMock(JDialog.class);
        replay(parent);
        new FindSequenceAction(null, search(), () -> 0, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullSearchThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, null, () -> 0, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullGetCurrentAddressThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, search(), null, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullParentThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        new FindSequenceAction(dialogs, search(), () -> 0, null, null);
    }
    @Test
    public void testConstructorAcceptsNullGUI() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, search(), () -> 0, parent, null);
    }

    private MemorySearch search() {
        return new MemorySearch(tableModel, address -> {}, status -> {});
    }
}
