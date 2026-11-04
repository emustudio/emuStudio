/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions.find_sequence;

import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.nio.ByteOrder;
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
                () -> isCurrentPage, () -> isPlainText ? PerformFindSequenceAction.Format.TEXT : PerformFindSequenceAction.Format.BYTES, () -> ByteOrder.BIG_ENDIAN,
                currentAddress, txtPosition, txtFindText
        );
    }

    @Test(expected = NullPointerException.class)
    public void testNullDialogsThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(null, () -> {}, (value, from) -> {}, () -> true, () -> PerformFindSequenceAction.Format.TEXT, () -> ByteOrder.BIG_ENDIAN, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullDisposeThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, null, (value, from) -> {}, () -> true, () -> PerformFindSequenceAction.Format.TEXT, () -> ByteOrder.BIG_ENDIAN, 0, txt, txt);
    }

    @Test(expected = NullPointerException.class)
    public void testNullStartSearchThrows() {
        JTextComponent txt = new JTextField();
        new PerformFindSequenceAction(dialogs, () -> {}, null, () -> true, () -> PerformFindSequenceAction.Format.TEXT, () -> ByteOrder.BIG_ENDIAN, 0, txt, txt);
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
                () -> true, () -> PerformFindSequenceAction.Format.BYTES, () -> ByteOrder.BIG_ENDIAN,
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
                () -> false, () -> PerformFindSequenceAction.Format.BYTES, () -> ByteOrder.BIG_ENDIAN,
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
                () -> true, () -> PerformFindSequenceAction.Format.BYTES, () -> ByteOrder.BIG_ENDIAN,
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

    @Test
    public void numericFormatsProduceExpectedBytes() {
        for (ByteOrder order : new ByteOrder[]{ByteOrder.BIG_ENDIAN, ByteOrder.LITTLE_ENDIAN}) {
            for (PerformFindSequenceAction.Format format : new PerformFindSequenceAction.Format[]{PerformFindSequenceAction.Format.INT16, PerformFindSequenceAction.Format.INT32, PerformFindSequenceAction.Format.INT64, PerformFindSequenceAction.Format.FLOAT32, PerformFindSequenceAction.Format.FLOAT64}) {
                JTextField value = new JTextField("1");
                PerformFindSequenceAction action = new PerformFindSequenceAction(dialogs, () -> {}, (bytes, address) -> sequence.set(bytes), () -> true, () -> format, () -> order, 0, new JTextField("0"), value);

                action.actionPerformed(new ActionEvent(this, 0, ""));

                byte[] bigEndian;
                switch (format) {
                    case INT16: bigEndian = new byte[]{0, 1}; break;
                    case INT32: bigEndian = new byte[]{0, 0, 0, 1}; break;
                    case INT64: bigEndian = new byte[]{0, 0, 0, 0, 0, 0, 0, 1}; break;
                    case FLOAT32: bigEndian = new byte[]{0x3f, (byte) 0x80, 0, 0}; break;
                    default: bigEndian = new byte[]{0x3f, (byte) 0xf0, 0, 0, 0, 0, 0, 0};
                }
                if (order == ByteOrder.LITTLE_ENDIAN) {
                    for (int i = 0; i < bigEndian.length / 2; i++) {
                        byte tmp = bigEndian[i];
                        bigEndian[i] = bigEndian[bigEndian.length - 1 - i];
                        bigEndian[bigEndian.length - 1 - i] = tmp;
                    }
                }
                assertArrayEquals(bigEndian, sequence.get());
            }
        }
    }
}
