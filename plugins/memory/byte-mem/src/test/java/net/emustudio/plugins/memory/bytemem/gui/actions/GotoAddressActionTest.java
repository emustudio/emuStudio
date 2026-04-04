/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;
import org.junit.Test;
import java.awt.event.ActionEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
public class GotoAddressActionTest {
    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        replay(context);
        new GotoAddressAction(null, context, addr -> {});
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        new GotoAddressAction(dialogs, null, addr -> {});
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullConsumerThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        replay(dialogs, context);
        new GotoAddressAction(dialogs, context, null);
    }
    @Test
    public void testActionPerformedWithValidAddress() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.of(100));
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(100, receivedAddress.get());
    }
    @Test
    public void testActionPerformedWithOutOfBoundsAddress() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.of(70000));
        dialogs.showError(anyString(), anyString());
        expectLastCall().once();
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(-1, receivedAddress.get()); // should not have been called
        verify(dialogs);
    }
    @Test
    public void testActionPerformedWithNegativeAddress() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.of(-1));
        dialogs.showError(anyString(), anyString());
        expectLastCall().once();
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(-1, receivedAddress.get());
        verify(dialogs);
    }
    @Test
    public void testActionPerformedWithEmptyInput() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.empty());
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(-1, receivedAddress.get());
    }
    @Test
    public void testActionPerformedWithZeroAddress() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.of(0));
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(0, receivedAddress.get());
    }
    @Test
    public void testActionPerformedWithMaxValidAddress() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.readInteger("Enter memory address:", "Go to address"))
                .andReturn(Optional.of(65535));
        replay(dialogs);
        ByteMemoryContext context = createNiceMock(ByteMemoryContext.class);
        expect(context.getSize()).andReturn(65536).anyTimes();
        replay(context);
        AtomicInteger receivedAddress = new AtomicInteger(-1);
        GotoAddressAction action = new GotoAddressAction(dialogs, context, receivedAddress::set);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "goto"));
        assertEquals(65535, receivedAddress.get());
    }
}
