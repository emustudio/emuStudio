/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a.interaction;

import net.emustudio.plugins.device.adm3a.TerminalSettings;
import net.emustudio.plugins.device.adm3a.TerminalSettingsTestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class KeyboardFromFileTest {
    private Path inputFile;
    private Path outputFile;

    @Before
    public void setUp() throws IOException {
        inputFile = Files.createTempFile("adm3a-test-input", ".txt");
        outputFile = Files.createTempFile("adm3a-test-output", ".txt");
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(inputFile);
        Files.deleteIfExists(outputFile);
    }

    @Test
    public void testProcessReadsFileAndNotifiesHandler() throws IOException {
        Files.writeString(inputFile, "ABC");

        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings(
                inputFile.toString(), outputFile.toString(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.process();

        assertEquals(3, received.size());
        assertEquals((byte) 'A', received.get(0).byteValue());
        assertEquals((byte) 'B', received.get(1).byteValue());
        assertEquals((byte) 'C', received.get(2).byteValue());
    }

    @Test
    public void testProcessEmptyFile() throws IOException {
        Files.writeString(inputFile, "");

        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings(
                inputFile.toString(), outputFile.toString(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.process();

        assertTrue(received.isEmpty());
    }

    @Test
    public void testProcessNonExistentFileDoesNotThrow() throws IOException {
        Files.deleteIfExists(inputFile);

        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings(
                inputFile.toString(), outputFile.toString(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.process();
        assertTrue(received.isEmpty());
    }

    @Test
    public void testProcessNotifiesMultipleHandlers() throws IOException {
        Files.writeString(inputFile, "X");

        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings(
                inputFile.toString(), outputFile.toString(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> handler1 = new ArrayList<>();
        List<Byte> handler2 = new ArrayList<>();
        keyboard.addOnKeyHandler(handler1::add);
        keyboard.addOnKeyHandler(handler2::add);

        keyboard.process();

        assertEquals(1, handler1.size());
        assertEquals(1, handler2.size());
    }

    @Test
    public void testCloseRemovesHandlers() throws IOException {
        Files.writeString(inputFile, "X");

        TerminalSettings settings = TerminalSettingsTestHelper.createNoGuiSettings(
                inputFile.toString(), outputFile.toString(), 0);
        KeyboardFromFile keyboard = new KeyboardFromFile(settings);

        List<Byte> received = new ArrayList<>();
        keyboard.addOnKeyHandler(received::add);

        keyboard.close();
        keyboard.process();

        assertTrue(received.isEmpty());
    }
}
