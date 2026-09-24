/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.rasp.gui.actions;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.memory.rasp.MemoryContextImpl;
import net.emustudio.plugins.memory.rasp.MemoryContextImplFactory;
import net.emustudio.plugins.memory.rasp.api.RaspLabel;
import net.emustudio.plugins.memory.rasp.api.RaspMemoryContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class DumpMemoryActionTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private MemoryContextImpl context;
    private Dialogs dialogs;

    @Before
    public void setUp() {
        context = MemoryContextImplFactory.create();
        dialogs = createNiceMock(Dialogs.class);
    }

    @After
    public void tearDown() {
        context.destroy();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullDialogsThrows() {
        new DumpMemoryAction(null, context, () -> 0);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        new DumpMemoryAction(dialogs, null, () -> 0);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullProgramLocationThrows() {
        replay(dialogs);
        new DumpMemoryAction(dialogs, context, null);
    }

    @Test
    public void testActionPerformedUserCancels() {
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.empty());
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 0);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));

        verify(dialogs);
    }

    @Test
    public void testActionPerformedDumpToTextFile() throws Exception {
        context.write(0, 0x10);
        context.write(1, 0xFF);
        context.write(2, 0x01); // getSize() returns max key=2, loop runs for i=0,1

        File dumpFile = tmpFolder.newFile("dump.txt");
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(dumpFile.toPath()));
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 0);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        waitForCompletion(action);

        // Verify the file content (hex format: "%X:\t%02X\n")
        String content = Files.readString(dumpFile.toPath());
        assertTrue(content.contains("0:\t10"));
        assertTrue(content.contains("1:\tFF"));
    }

    @Test
    public void testActionPerformedDumpToBinaryFile() throws Exception {
        context.write(0, 1);
        context.write(1, 15);
        context.setLabels(List.of(createLabel(0, "START")));
        context.setInputs(List.of(42));

        File dumpFile = tmpFolder.newFile("dump.brasp");
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(dumpFile.toPath()));
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 5);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        waitForCompletion(action);

        // Verify by deserializing
        MemoryContextImpl context2 = MemoryContextImplFactory.create();
        AtomicInteger programLocation = new AtomicInteger(-1);
        context2.deserialize(dumpFile.getAbsolutePath(), programLocation::set);

        assertEquals(5, programLocation.get());
        assertEquals(Integer.valueOf(1), context2.read(0));
        assertEquals(Integer.valueOf(15), context2.read(1));
        assertTrue(context2.getLabel(0).isPresent());
        assertEquals("START", context2.getLabel(0).get().getLabel());
        assertEquals(List.of(42), context2.getSnapshot().inputs);

        context2.destroy();
    }

    @Test
    public void testActionPerformedDumpToTextFileUpperCaseExtension() throws Exception {
        context.write(0, 0x0A);
        context.write(1, 0x00); // ensure getSize() returns 1 so loop runs for i=0

        File dumpFile = new File(tmpFolder.getRoot(), "dump.TXT");
        dumpFile.createNewFile();

        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(dumpFile.toPath()));
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 0);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        waitForCompletion(action);

        // Should still write text format for .TXT
        String content = Files.readString(dumpFile.toPath());
        assertTrue(content.contains("0:\t0A"));
    }

    @Test
    public void testActionPerformedDumpEmptyMemoryTextFile() throws Exception {
        File dumpFile = tmpFolder.newFile("empty.txt");
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(dumpFile.toPath()));
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 0);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        waitForCompletion(action);

        String content = Files.readString(dumpFile.toPath());
        assertEquals("", content);
    }

    @Test
    public void testActionPerformedIOErrorShowsErrorDialog() throws Exception {
        // Use a path that cannot be written to
        Path invalidPath = Path.of(tmpFolder.getRoot().getAbsolutePath(), "nonexistent-dir", "dump.txt");
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(invalidPath));
        dialogs.showError(anyString());
        expectLastCall().once();
        replay(dialogs);

        DumpMemoryAction action = new DumpMemoryAction(dialogs, context, () -> 0);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "dump"));
        waitForCompletion(action);

        verify(dialogs);
    }

    private void waitForCompletion(Action action) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (action.isEnabled()) {
                return;
            }
            Thread.sleep(10);
        }
        fail("Memory action did not complete");
    }

    private RaspLabel createLabel(int address, String label) {
        return new RaspLabel() {
            @Override
            public int getAddress() {
                return address;
            }

            @Override
            public String getLabel() {
                return label;
            }
        };
    }
}
