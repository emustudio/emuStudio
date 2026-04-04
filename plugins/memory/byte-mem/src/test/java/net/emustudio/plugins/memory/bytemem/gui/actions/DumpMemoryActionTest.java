/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
public class DumpMemoryActionTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();
    private MemoryContextImpl context;
    @Before
    public void setUp() {
        context = TestMemoryContextFactory.create(4, 1, 0);
        context.write(0, (byte) 0xAA);
        context.write(1, (byte) 0xBB);
        context.write(2, (byte) 0xCC);
        context.write(3, (byte) 0xDD);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        new DumpMemoryAction(null, context);
    }
    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        new DumpMemoryAction(dialogs, null);
    }
    @Test
    public void testDumpToTextFile() throws Exception {
        File txtFile = tmpFolder.newFile("dump.txt");
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.chooseFile(anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class)))
                .andReturn(Optional.of(txtFile.toPath()));
        replay(dialogs);
        DumpMemoryAction action = new DumpMemoryAction(dialogs, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        String content = Files.readString(txtFile.toPath());
        assertTrue(content.contains("AA"));
        assertTrue(content.contains("BB"));
    }
    @Test
    public void testDumpToBinaryFile() throws Exception {
        File binFile = tmpFolder.newFile("dump.bin");
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.chooseFile(anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class)))
                .andReturn(Optional.of(binFile.toPath()));
        replay(dialogs);
        DumpMemoryAction action = new DumpMemoryAction(dialogs, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        byte[] bytes = Files.readAllBytes(binFile.toPath());
        assertEquals(4, bytes.length);
        assertEquals((byte) 0xAA, bytes[0]);
        assertEquals((byte) 0xBB, bytes[1]);
        assertEquals((byte) 0xCC, bytes[2]);
        assertEquals((byte) 0xDD, bytes[3]);
    }
    @Test
    public void testDumpCancelledByUser() {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.chooseFile(anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class),
                anyObject(net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter.class)))
                .andReturn(Optional.empty());
        replay(dialogs);
        DumpMemoryAction action = new DumpMemoryAction(dialogs, context);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        // No exception = pass
    }
}
