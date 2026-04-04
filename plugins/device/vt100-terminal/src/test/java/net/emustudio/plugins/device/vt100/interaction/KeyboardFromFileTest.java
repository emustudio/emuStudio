/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100.interaction;

import net.emustudio.plugins.device.vt100.TerminalSettings;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class KeyboardFromFileTest {
    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private TerminalSettings createSettings(Path inputPath, int delayMillis) {
        TerminalSettings settings = createNiceMock(TerminalSettings.class);
        expect(settings.getInputPath()).andReturn(inputPath).anyTimes();
        expect(settings.getInputReadDelayMillis()).andReturn(delayMillis).anyTimes();
        replay(settings);
        return settings;
    }

    @Test
    public void testProcessReadsAllBytesFromFile() throws IOException {
        File inputFile = tmpFolder.newFile("input.txt");
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write(new byte[]{0x41, 0x42, 0x43}); // A, B, C
        }

        TerminalSettings settings = createSettings(inputFile.toPath(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> receivedKeys = new ArrayList<>();
        keyboard.addOnKeyHandler(receivedKeys::add);

        keyboard.process();

        assertEquals(3, receivedKeys.size());
        assertEquals(Byte.valueOf((byte) 0x41), receivedKeys.get(0));
        assertEquals(Byte.valueOf((byte) 0x42), receivedKeys.get(1));
        assertEquals(Byte.valueOf((byte) 0x43), receivedKeys.get(2));
    }

    @Test
    public void testProcessNonExistentFileDoesNotThrow() {
        TerminalSettings settings = createSettings(Path.of("/nonexistent/file.txt"), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> receivedKeys = new ArrayList<>();
        keyboard.addOnKeyHandler(receivedKeys::add);

        keyboard.process(); // should not throw

        assertTrue(receivedKeys.isEmpty());
    }

    @Test
    public void testProcessEmptyFile() throws IOException {
        File inputFile = tmpFolder.newFile("empty.txt");

        TerminalSettings settings = createSettings(inputFile.toPath(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> receivedKeys = new ArrayList<>();
        keyboard.addOnKeyHandler(receivedKeys::add);

        keyboard.process();

        assertTrue(receivedKeys.isEmpty());
    }

    @Test
    public void testProcessWithSingleByte() throws IOException {
        File inputFile = tmpFolder.newFile("single.txt");
        try (FileOutputStream fos = new FileOutputStream(inputFile)) {
            fos.write(0x0D); // CR
        }

        TerminalSettings settings = createSettings(inputFile.toPath(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> receivedKeys = new ArrayList<>();
        keyboard.addOnKeyHandler(receivedKeys::add);

        keyboard.process();

        assertEquals(1, receivedKeys.size());
        assertEquals(Byte.valueOf((byte) 0x0D), receivedKeys.get(0));
    }
}
