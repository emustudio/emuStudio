/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertEquals;

public class PerformFindSequenceActionTest {

    private MemoryContextImpl context;
    private MemoryTableModel tableModel;
    private Dialogs dialogs;
    private AtomicInteger foundAddress;
    private boolean disposed;

    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(256, 1, 0);
        tableModel = new MemoryTableModel(context);
        dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        foundAddress = new AtomicInteger(-1);
        disposed = false;
    }

    private PerformFindSequenceAction createAction(
            boolean isCurrentPage, boolean isPlainText,
            String positionText, String findText, int currentAddress) {
        JTextComponent txtPosition = new JTextField(positionText);
        JTextComponent txtFindText = new JTextField(findText);

        return new PerformFindSequenceAction(
                dialogs, () -> disposed = true, tableModel,
                foundAddress::set,
                () -> isCurrentPage, () -> isPlainText,
                currentAddress, txtPosition, txtFindText
        );
    }

    @Test(expected = NullPointerException.class)
    public void testNullDialogsThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(null, () -> {}, tableModel, i -> {}, () -> true, () -> true, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDisposeThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, null, tableModel, i -> {}, () -> true, () -> true, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullTableModelThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, () -> {}, null, i -> {}, () -> true, () -> true, 0, txt, txt);
    }

    @Test
    public void testFindSequenceHexFromCurrentPage() {
        context.write(10, (byte) 0xAA);
        context.write(11, (byte) 0xBB);

        PerformFindSequenceAction action = createAction(true, false, "", "0xAA 0xBB", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(10, foundAddress.get());
    }

    @Test
    public void testFindSequencePlainText() {
        context.write(0, (byte) 'H');
        context.write(1, (byte) 'i');

        PerformFindSequenceAction action = createAction(true, true, "", "Hi", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(0, foundAddress.get());
    }

    @Test
    public void testFindSequenceFromSpecificAddress() {
        context.write(5, (byte) 0xAA);
        context.write(50, (byte) 0xAA);

        PerformFindSequenceAction action = createAction(false, false, "10", "0xAA", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(50, foundAddress.get());
    }

    @Test
    public void testFindSequenceNotFound() {
        PerformFindSequenceAction action = createAction(true, false, "", "0xDE 0xAD", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(-1, foundAddress.get());
    }

    @Test
    public void testEmptyFindTextShowsError() {
        Dialogs strictDialogs = createMock(Dialogs.class);
        strictDialogs.showError(anyString(), anyString());
        expectLastCall().once();
        replay(strictDialogs);

        JTextComponent txtPosition = new JTextField("0");
        JTextComponent txtFindText = new JTextField("");

        PerformFindSequenceAction action = new PerformFindSequenceAction(
                strictDialogs, () -> {}, tableModel,
                foundAddress::set,
                () -> true, () -> false,
                0, txtPosition, txtFindText
        );
        action.actionPerformed(new ActionEvent(this, 0, ""));

        verify(strictDialogs);
    }

    @Test
    public void testInvalidPositionShowsError() {
        Dialogs strictDialogs = createMock(Dialogs.class);
        strictDialogs.showError(eq(PerformFindSequenceAction.ERROR_NUMBER_FORMAT), anyString());
        expectLastCall().once();
        replay(strictDialogs);

        JTextComponent txtPosition = new JTextField("not_a_number");
        JTextComponent txtFindText = new JTextField("0xAA");

        PerformFindSequenceAction action = new PerformFindSequenceAction(
                strictDialogs, () -> {}, tableModel,
                foundAddress::set,
                () -> false, () -> false,
                0, txtPosition, txtFindText
        );
        action.actionPerformed(new ActionEvent(this, 0, ""));

        verify(strictDialogs);
    }

    @Test
    public void testInvalidHexSequenceShowsError() {
        Dialogs strictDialogs = createMock(Dialogs.class);
        strictDialogs.showError(eq(PerformFindSequenceAction.ERROR_NUMBER_FORMAT), anyString());
        expectLastCall().once();
        replay(strictDialogs);

        JTextComponent txtPosition = new JTextField("0");
        JTextComponent txtFindText = new JTextField("xyz");

        PerformFindSequenceAction action = new PerformFindSequenceAction(
                strictDialogs, () -> {}, tableModel,
                foundAddress::set,
                () -> true, () -> false,
                0, txtPosition, txtFindText
        );
        action.actionPerformed(new ActionEvent(this, 0, ""));

        verify(strictDialogs);
    }

    @Test
    public void testDisposeCalledOnSuccess() {
        context.write(0, (byte) 0x42);
        PerformFindSequenceAction action = createAction(true, false, "", "0x42", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(true, disposed);
    }
}

