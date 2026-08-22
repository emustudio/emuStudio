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
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class LoadImageActionTest {
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
        new LoadImageAction(null, context, () -> {}, i -> {});
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullContextThrows() {
        new LoadImageAction(dialogs, null, () -> {}, i -> {});
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullRepaintThrows() {
        new LoadImageAction(dialogs, context, null, i -> {});
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullSetProgramLocationThrows() {
        replay(dialogs);
        new LoadImageAction(dialogs, context, () -> {}, null);
    }

    @Test
    public void testActionPerformedUserCancels() {
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.empty());
        replay(dialogs);

        LoadImageAction action = new LoadImageAction(dialogs, context, () -> {}, i -> {});
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));

        verify(dialogs);
    }

    @Test
    public void testActionPerformedLoadsImage() throws Exception {
        // Create a valid image file
        context.write(0, 1);
        context.write(1, 15);
        context.setLabels(List.of(createLabel(0, "START")));
        context.setInputs(List.of(42));

        File imageFile = tmpFolder.newFile("test.brasp");
        RaspMemoryContext.serialize(imageFile.toPath(), 7, context.getSnapshot());

        // Clear context to verify loading works
        context.clear();
        assertEquals(0, context.getSize());

        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(imageFile.toPath()));
        replay(dialogs);

        AtomicInteger programLocation = new AtomicInteger(-1);
        AtomicBoolean repainted = new AtomicBoolean(false);

        LoadImageAction action = new LoadImageAction(
                dialogs, context, () -> repainted.set(true), programLocation::set
        );
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));
        waitForCompletion(action);

        assertEquals(7, programLocation.get());
        assertEquals(Integer.valueOf(1), context.read(0));
        assertEquals(Integer.valueOf(15), context.read(1));
        assertTrue(context.getLabel(0).isPresent());
        assertEquals("START", context.getLabel(0).get().getLabel());
        assertEquals(List.of(42), context.getSnapshot().inputs);
        assertTrue(repainted.get());
    }

    @Test
    public void testActionPerformedInvalidFileShowsError() throws Exception {
        File invalidFile = tmpFolder.newFile("invalid.brasp");
        // Write garbage
        java.nio.file.Files.writeString(invalidFile.toPath(), "this is not a valid image");

        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(invalidFile.toPath()));
        dialogs.showError(anyString(), anyString());
        expectLastCall().once();
        replay(dialogs);

        LoadImageAction action = new LoadImageAction(
                dialogs, context, () -> {}, i -> {}
        );
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));
        waitForCompletion(action);

        verify(dialogs);
    }

    @Test
    public void testActionPerformedNonExistentFileShowsError() throws Exception {
        Path nonExistent = Path.of(tmpFolder.getRoot().getAbsolutePath(), "nonexistent.brasp");

        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(nonExistent));
        dialogs.showError(anyString(), anyString());
        expectLastCall().once();
        replay(dialogs);

        LoadImageAction action = new LoadImageAction(
                dialogs, context, () -> {}, i -> {}
        );
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));
        waitForCompletion(action);

        verify(dialogs);
    }

    @Test
    public void testActionPerformedSecondCallUsesRecentPath() throws Exception {
        // First call - creates and loads an image
        context.write(0, 1);
        File imageFile1 = tmpFolder.newFile("first.brasp");
        RaspMemoryContext.serialize(imageFile1.toPath(), 0, context.getSnapshot());
        context.clear();

        // Second call
        context.write(0, 2);
        File imageFile2 = tmpFolder.newFile("second.brasp");
        RaspMemoryContext.serialize(imageFile2.toPath(), 1, context.getSnapshot());
        context.clear();

        // First call returns imageFile1
        expect(dialogs.chooseFile(
                anyString(), anyString(), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(imageFile1.toPath()));
        // Second call - we expect the dialog to receive the recent path (imageFile1's path)
        expect(dialogs.chooseFile(
                anyString(), anyString(), eq(imageFile1.toPath()), eq(false),
                anyObject(FileExtensionsFilter.class)
        )).andReturn(Optional.of(imageFile2.toPath()));
        replay(dialogs);

        AtomicInteger programLocation = new AtomicInteger(-1);
        LoadImageAction action = new LoadImageAction(
                dialogs, context, () -> {}, programLocation::set
        );

        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));
        waitForCompletion(action);
        action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "load"));
        waitForCompletion(action);

        // After second load, program location should be from second file
        assertEquals(1, programLocation.get());
        assertEquals(Integer.valueOf(2), context.read(0));
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
