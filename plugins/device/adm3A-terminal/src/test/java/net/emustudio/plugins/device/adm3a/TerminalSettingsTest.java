/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class TerminalSettingsTest {

    private TerminalSettings settings;
    private PluginSettings pluginSettings;

    @Before
    public void setUp() {
        pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false).anyTimes();
        expect(pluginSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(pluginSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(pluginSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(pluginSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        settings = new TerminalSettings(pluginSettings, dialogs);
    }

    @Test
    public void testDefaultValues() {
        assertFalse(settings.isHalfDuplex());
        assertFalse(settings.isAlwaysOnTop());
        assertEquals(Path.of(TerminalSettings.DEFAULT_INPUT_FILE_NAME), settings.getInputPath());
        assertEquals(Path.of(TerminalSettings.DEFAULT_OUTPUT_FILE_NAME), settings.getOutputPath());
        assertEquals(0, settings.getInputReadDelayMillis());
        assertEquals(0, settings.getDeviceIndex());
        assertEquals(TerminalSettings.TerminalFont.ORIGINAL, settings.getFont());
    }

    @Test
    public void testGuiSupported() {
        assertTrue(settings.isGuiSupported());
    }

    @Test
    public void testGuiNotSupported() {
        PluginSettings noGuiSettings = createNiceMock(PluginSettings.class);
        expect(noGuiSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(noGuiSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(noGuiSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(noGuiSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(noGuiSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        TerminalSettings noGui = new TerminalSettings(noGuiSettings, dialogs);
        assertFalse(noGui.isGuiSupported());
    }

    @Test
    public void testSetHalfDuplex() {
        settings.setHalfDuplex(true);
        assertTrue(settings.isHalfDuplex());
    }

    @Test
    public void testSetAlwaysOnTop() {
        settings.setAlwaysOnTop(true);
        assertTrue(settings.isAlwaysOnTop());
    }

    @Test
    public void testSetInputPath() throws IOException {
        Path newPath = Path.of("custom-input.txt");
        settings.setInputPath(newPath);
        assertEquals(newPath, settings.getInputPath());
    }

    @Test
    public void testSetOutputPath() throws IOException {
        Path newPath = Path.of("custom-output.txt");
        settings.setOutputPath(newPath);
        assertEquals(newPath, settings.getOutputPath());
    }

    @Test
    public void testSetInputReadDelayMillis() {
        settings.setInputReadDelayMillis(500);
        assertEquals(500, settings.getInputReadDelayMillis());
    }

    @Test
    public void testSetDeviceIndex() {
        settings.setDeviceIndex(3);
        assertEquals(3, settings.getDeviceIndex());
    }

    @Test
    public void testSetFont() {
        settings.setFont(TerminalSettings.TerminalFont.MODERN);
        assertEquals(TerminalSettings.TerminalFont.MODERN, settings.getFont());
    }

    @Test(expected = NullPointerException.class)
    public void testSetNullFontThrows() {
        settings.setFont(null);
    }

    @Test
    public void testAddAndRemoveChangedObserver() throws IOException {
        TerminalSettings.ChangedObserver observer = mock(TerminalSettings.ChangedObserver.class);
        observer.settingsChanged();
        expectLastCall().once();
        replay(observer);

        settings.addChangedObserver(observer);
        settings.setHalfDuplex(true); // triggers notification

        verify(observer);

        reset(observer);
        replay(observer);

        settings.removeChangedObserver(observer);
        settings.setHalfDuplex(false); // should NOT trigger notification

        verify(observer); // no interactions
    }

    @Test
    public void testObserverNotifiedOnSetAlwaysOnTop() {
        TerminalSettings.ChangedObserver observer = mock(TerminalSettings.ChangedObserver.class);
        try {
            observer.settingsChanged();
        } catch (IOException e) {
            // won't happen with mock
        }
        expectLastCall().once();
        replay(observer);

        settings.addChangedObserver(observer);
        settings.setAlwaysOnTop(true);

        verify(observer);
    }

    @Test
    public void testObserverNotifiedOnSetInputReadDelayMillis() {
        TerminalSettings.ChangedObserver observer = mock(TerminalSettings.ChangedObserver.class);
        try {
            observer.settingsChanged();
        } catch (IOException e) {
            // won't happen with mock
        }
        expectLastCall().once();
        replay(observer);

        settings.addChangedObserver(observer);
        settings.setInputReadDelayMillis(100);

        verify(observer);
    }

    @Test
    public void testTerminalFontValueOfIndex() {
        assertEquals(TerminalSettings.TerminalFont.ORIGINAL, TerminalSettings.TerminalFont.valueOf(0));
        assertEquals(TerminalSettings.TerminalFont.MODERN, TerminalSettings.TerminalFont.valueOf(1));
    }

    @Test(expected = RuntimeException.class)
    public void testTerminalFontValueOfInvalidIndex() {
        TerminalSettings.TerminalFont.valueOf(99);
    }

    @Test
    public void testTerminalFontName() {
        assertEquals("original", TerminalSettings.TerminalFont.ORIGINAL.name);
        assertEquals("modern", TerminalSettings.TerminalFont.MODERN.name);
    }

    @Test
    public void testWriteCallsSettingsSetters() throws Exception {
        PluginSettings writableSettings = createNiceMock(PluginSettings.class);
        expect(writableSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false).anyTimes();
        expect(writableSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(writableSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(writableSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        // Expect write operations
        writableSettings.setBoolean(anyString(), anyBoolean());
        expectLastCall().anyTimes();
        writableSettings.setInt(anyString(), anyInt());
        expectLastCall().anyTimes();
        writableSettings.setString(anyString(), anyString());
        expectLastCall().anyTimes();
        replay(writableSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        TerminalSettings s = new TerminalSettings(writableSettings, dialogs);
        s.write();

        verify(writableSettings);
    }

    @Test
    public void testInputPathEqualsOutputPathResetsToDefaults() {
        PluginSettings samePathSettings = createNiceMock(PluginSettings.class);
        expect(samePathSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false).anyTimes();
        expect(samePathSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(samePathSettings.getString(eq("inputFileName"), anyString())).andReturn("same.txt").anyTimes();
        expect(samePathSettings.getString(eq("outputFileName"), anyString())).andReturn("same.txt").anyTimes();
        expect(samePathSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(samePathSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(samePathSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        TerminalSettings s = new TerminalSettings(samePathSettings, dialogs);
        // Should have reset to defaults because input == output
        assertEquals(Path.of(TerminalSettings.DEFAULT_INPUT_FILE_NAME), s.getInputPath());
        assertEquals(Path.of(TerminalSettings.DEFAULT_OUTPUT_FILE_NAME), s.getOutputPath());
    }
}

