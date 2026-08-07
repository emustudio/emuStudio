/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;

import static net.emustudio.plugins.memory.ssem.gui.actions.DumpMemoryActionTest.mockApi;
import static org.easymock.EasyMock.*;

public class LoadImageActionTest {
    private final static int PROGRAM_LOCATION = 4;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testNoFileIsChosenWorks() {
        Runnable repaint = createMock(Runnable.class);
        replay(repaint);
        new LoadImageAction(mockApi(), new MemoryContextImpl(new Annotations()), repaint).actionPerformed(null);
        verify(repaint);
    }

    @Test
    public void testLoadBSSEM() throws Exception {
        Path path = folder.newFolder().toPath().resolve("binary.bssem");
        write(path);
        MemoryContext<Byte> memory = createMock(MemoryContext.class);
        Byte[] toWrite = new Byte[32 * 4];
        for (int i = 0; i < 32 * 4; i += 4) {
            toWrite[i] = (byte) i;
            toWrite[i + 1] = (byte) (i + 1);
            toWrite[i + 2] = (byte) (i + 2);
            toWrite[i + 3] = (byte) (i + 3);
        }
        memory.write(0, toWrite);
        expectLastCall().once();
        replay(memory);

        Runnable repaint = createMock(Runnable.class);
        repaint.run();
        expectLastCall().once();
        replay(repaint);

        LoadImageAction action = new LoadImageAction(mockApi(path), memory, repaint);
        action.actionPerformed(null);
        waitForCompletion(action);

        verify(memory, repaint);
    }

    private void waitForCompletion(Action action) throws InterruptedException {
        for (int i = 0; i < 200; i++) {
            if (action.isEnabled()) {
                return;
            }
            Thread.sleep(10);
        }
        throw new AssertionError("Memory action did not complete");
    }


    private void write(Path path) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(33 * 4);
        buffer.putInt(PROGRAM_LOCATION / 4);
        for (int i = 0; i < 32 * 4; i += 4) {
            buffer.put(new byte[]{(byte) i, (byte) (i + 1), (byte) (i + 2), (byte) (i + 3)});
        }
        buffer.flip();
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            fos.getChannel().write(buffer);
        }
    }
}
