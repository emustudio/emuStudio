/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence;

import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class PerformFindSequenceActionTest {

    private Dialogs dialogs;
    private AtomicReference<byte[]> sequence;
    private AtomicInteger from;
    private boolean disposed;

    @Before
    public void setUp() {
        dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        sequence = new AtomicReference<>();
        from = new AtomicInteger(-1);
        disposed = false;
    }

    private PerformFindSequenceAction createAction(
            boolean isCurrentPage, boolean isPlainText,
            String positionText, String findText, int currentAddress) {
        JTextComponent txtPosition = new JTextField(positionText);
        JTextComponent txtFindText = new JTextField(findText);

        return new PerformFindSequenceAction(
                dialogs, () -> disposed = true, (value, address) -> {
                    sequence.set(value);
                    from.set(address);
                },
                () -> isCurrentPage, () -> isPlainText,
                currentAddress, txtPosition, txtFindText
        );
    }

    @Test(expected = NullPointerException.class)
    public void testNullDialogsThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(null, () -> {}, (value, from) -> {}, () -> true, () -> true, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDisposeThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, null, (value, from) -> {}, () -> true, () -> true, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStartSearchThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, () -> {}, null, () -> true, () -> true, 0, txt, txt);
    }

    @Test
    public void testFindSequenceHexFromCurrentPage() {
        PerformFindSequenceAction action = createAction(true, false, "", "0xAA 0xBB", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertArrayEquals(new byte[]{(byte) 0xAA, (byte) 0xBB}, sequence.get());
        assertEquals(0, from.get());
    }

    @Test
    public void testFindSequencePlainText() {
        PerformFindSequenceAction action = createAction(true, true, "", "Hi", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertArrayEquals(new byte[]{'H', 'i'}, sequence.get());
    }

    @Test
    public void testFindSequenceFromSpecificAddress() {
        PerformFindSequenceAction action = createAction(false, false, "10", "0xAA", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(10, from.get());
    }

    @Test
    public void testSearchRequestAlwaysIncludesPattern() {
        PerformFindSequenceAction action = createAction(true, false, "", "0xDE 0xAD", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertArrayEquals(new byte[]{(byte) 0xDE, (byte) 0xAD}, sequence.get());
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
                strictDialogs, () -> {}, (value, from) -> {},
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
                strictDialogs, () -> {}, (value, from) -> {},
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
                strictDialogs, () -> {}, (value, from) -> {},
                () -> true, () -> false,
                0, txtPosition, txtFindText
        );
        action.actionPerformed(new ActionEvent(this, 0, ""));

        verify(strictDialogs);
    }

    @Test
    public void testDisposeCalledOnSuccess() {
        PerformFindSequenceAction action = createAction(true, false, "", "0x42", 0);
        action.actionPerformed(new ActionEvent(this, 0, ""));

        assertEquals(true, disposed);
    }
}
