/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.memory.ssem.gui.actions;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.plugins.memory.annotations.Annotations;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

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
    public void testLoadBSSEM() throws IOException {
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

        verify(memory, repaint);
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
