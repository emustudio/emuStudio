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
        new FindSequenceAction(null, addr -> {}, tableModel, () -> 0, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullSetPageFromAddressThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, null, tableModel, () -> 0, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullTableModelThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, addr -> {}, null, () -> 0, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullGetCurrentAddressThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, addr -> {}, tableModel, null, parent, null);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullParentThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        new FindSequenceAction(dialogs, addr -> {}, tableModel, () -> 0, null, null);
    }
    @Test
    public void testConstructorAcceptsNullGUI() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        JDialog parent = createNiceMock(JDialog.class);
        replay(dialogs, parent);
        new FindSequenceAction(dialogs, addr -> {}, tableModel, () -> 0, parent, null);
    }
}
