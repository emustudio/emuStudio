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
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;
import net.emustudio.plugins.memory.ssem.MemoryContextImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class DumpMemoryActionTest {
    private final static int PROGRAM_LOCATION = 4;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testNoFileIsChosen() {
        DumpMemoryAction action = new DumpMemoryAction(mockApi(), new MemoryContextImpl(new Annotations()));
        action.actionPerformed(null);
    }

    @Test
    public void testHumanReadableDump() throws IOException {
        Path output = folder.newFolder().toPath().resolve("human-readable.txt");
        DumpMemoryAction action = new DumpMemoryAction(mockApi(output), prepareMemory());
        action.actionPerformed(null);

        String content = new String(read(output).array());
        assertEquals("0x00, 0x01, 0x02, 0x03\n" +
                "0x04, 0x05, 0x06, 0x07\n" +
                "0x08, 0x09, 0x0A, 0x0B\n" +
                "0x0C, 0x0D, 0x0E, 0x0F\n" +
                "0x10, 0x11, 0x12, 0x13\n" +
                "0x14, 0x15, 0x16, 0x17\n" +
                "0x18, 0x19, 0x1A, 0x1B\n" +
                "0x1C, 0x1D, 0x1E, 0x1F\n" +
                "0x20, 0x21, 0x22, 0x23\n" +
                "0x24, 0x25, 0x26, 0x27\n" +
                "0x28, 0x29, 0x2A, 0x2B\n" +
                "0x2C, 0x2D, 0x2E, 0x2F\n" +
                "0x30, 0x31, 0x32, 0x33\n" +
                "0x34, 0x35, 0x36, 0x37\n" +
                "0x38, 0x39, 0x3A, 0x3B\n" +
                "0x3C, 0x3D, 0x3E, 0x3F\n" +
                "0x40, 0x41, 0x42, 0x43\n" +
                "0x44, 0x45, 0x46, 0x47\n" +
                "0x48, 0x49, 0x4A, 0x4B\n" +
                "0x4C, 0x4D, 0x4E, 0x4F\n" +
                "0x50, 0x51, 0x52, 0x53\n" +
                "0x54, 0x55, 0x56, 0x57\n" +
                "0x58, 0x59, 0x5A, 0x5B\n" +
                "0x5C, 0x5D, 0x5E, 0x5F\n" +
                "0x60, 0x61, 0x62, 0x63\n" +
                "0x64, 0x65, 0x66, 0x67\n" +
                "0x68, 0x69, 0x6A, 0x6B\n" +
                "0x6C, 0x6D, 0x6E, 0x6F\n" +
                "0x70, 0x71, 0x72, 0x73\n" +
                "0x74, 0x75, 0x76, 0x77\n" +
                "0x78, 0x79, 0x7A, 0x7B\n" +
                "0x7C, 0x7D, 0x7E, 0x7F\n", content);
    }

    @Test
    public void testBSSEM() throws IOException {
        Path output = folder.newFolder().toPath().resolve("binary.bsem");
        DumpMemoryAction action = new DumpMemoryAction(mockApi(output), prepareMemory());
        action.actionPerformed(null);

        ByteBuffer data = read(output);
        assertEquals(PROGRAM_LOCATION, data.getInt() * 4);

        for (int i = 0; i < 32 * 4; i += 4) {
            byte[] row = new byte[4];
            data.get(row);
            assertArrayEquals(new byte[] { (byte)i, (byte)(i+1), (byte)(i+2), (byte)(i+3) }, row);
        }
    }

    public static ApplicationApi mockApi() {
        return mockApi(null);
    }

    public static ApplicationApi mockApi(Path chosenPath) {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        expect(dialogs.chooseFile(anyString(), anyString(), anyObject(Path.class),
                eq(true), anyObject(FileExtensionsFilter.class), anyObject(FileExtensionsFilter.class)))
                .andReturn(Optional.ofNullable(chosenPath))
                .anyTimes();
        expect(dialogs.chooseFile(anyString(), anyString(), anyObject(Path.class),
                eq(false), anyObject(FileExtensionsFilter.class)))
                .andReturn(Optional.ofNullable(chosenPath))
                .anyTimes();
        replay(dialogs);

        ApplicationApi api = createNiceMock(ApplicationApi.class);
        expect(api.getDialogs()).andReturn(dialogs).anyTimes();
        expect(api.getProgramLocation()).andReturn(PROGRAM_LOCATION).anyTimes();
        replay(api);

        return api;
    }

    public static ByteBuffer read(Path path) throws IOException {
        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            return ByteBuffer.wrap(stream.readAllBytes());
        }
    }

    private MemoryContext<Byte> prepareMemory() {
        MemoryContextImpl mem = new MemoryContextImpl(new Annotations());
        for (int i = 0; i < 32 * 4; i += 4) {
            mem.write(i, new Byte[] { (byte)i, (byte)(i+1), (byte)(i+2), (byte)(i+3) });
        }
        return mem;
    }
}
