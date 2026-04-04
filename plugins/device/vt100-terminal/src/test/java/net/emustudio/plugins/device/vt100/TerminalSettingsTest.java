/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.vt100;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class TerminalSettingsTest {
    private Dialogs dialogs;
    private TerminalSettings settings;

    @Before
    public void setUp() {
        dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        settings = new TerminalSettings(PluginSettings.UNAVAILABLE, dialogs);
    }

    @Test
    public void testDefaultInputPath() {
        assertEquals(Path.of(TerminalSettings.DEFAULT_INPUT_FILE_NAME), settings.getInputPath());
    }

    @Test
    public void testDefaultOutputPath() {
        assertEquals(Path.of(TerminalSettings.DEFAULT_OUTPUT_FILE_NAME), settings.getOutputPath());
    }

    @Test
    public void testDefaultColumns() {
        assertEquals(TerminalSettings.DEFAULT_COLUMNS, settings.getColumns());
    }

    @Test
    public void testDefaultRows() {
        assertEquals(TerminalSettings.DEFAULT_ROWS, settings.getRows());
    }

    @Test
    public void testDefaultInputReadDelayMillis() {
        assertEquals(TerminalSettings.DEFAULT_INPUT_READ_DELAY_MILLIS, settings.getInputReadDelayMillis());
    }

    @Test
    public void testSetInputPath() {
        settings.setInputPath(Path.of("custom-input.txt"));
        assertEquals(Path.of("custom-input.txt"), settings.getInputPath());
    }

    @Test
    public void testSetOutputPath() {
        settings.setOutputPath(Path.of("custom-output.txt"));
        assertEquals(Path.of("custom-output.txt"), settings.getOutputPath());
    }

    @Test
    public void testSetInputReadDelayMillis() {
        settings.setInputReadDelayMillis(100);
        assertEquals(100, settings.getInputReadDelayMillis());
    }

    @Test
    public void testSetSize() {
        settings.setSize(120, 40);
        assertEquals(120, settings.getColumns());
        assertEquals(40, settings.getRows());
    }

    @Test
    public void testSizeChangedObserverNotified() {
        AtomicInteger observedColumns = new AtomicInteger();
        AtomicInteger observedRows = new AtomicInteger();

        settings.addSizeChangedObserver((cols, rows) -> {
            observedColumns.set(cols);
            observedRows.set(rows);
        });

        settings.setSize(100, 50);

        assertEquals(100, observedColumns.get());
        assertEquals(50, observedRows.get());
    }

    @Test
    public void testMultipleObserversNotified() {
        AtomicBoolean observer1 = new AtomicBoolean(false);
        AtomicBoolean observer2 = new AtomicBoolean(false);

        settings.addSizeChangedObserver((c, r) -> observer1.set(true));
        settings.addSizeChangedObserver((c, r) -> observer2.set(true));

        settings.setSize(80, 25);

        assertTrue(observer1.get());
        assertTrue(observer2.get());
    }

    @Test
    public void testDestroyClearsObservers() {
        AtomicBoolean notified = new AtomicBoolean(false);
        settings.addSizeChangedObserver((c, r) -> notified.set(true));

        settings.destroy();
        settings.setSize(100, 50);

        assertFalse(notified.get());
    }

    @Test
    public void testGuiNotSupportedWhenNoGui() {
        PluginSettings pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(eq(PluginSettings.EMUSTUDIO_NO_GUI), anyBoolean())).andReturn(true);
        expect(pluginSettings.getString(anyString(), anyString())).andStubReturn(TerminalSettings.DEFAULT_INPUT_FILE_NAME);
        expect(pluginSettings.getInt(anyString(), anyInt())).andStubReturn(0);
        replay(pluginSettings);

        TerminalSettings noGuiSettings = new TerminalSettings(pluginSettings, dialogs);
        assertFalse(noGuiSettings.isGuiSupported());
    }

    @Test
    public void testGuiSupportedByDefault() {
        assertTrue(settings.isGuiSupported());
    }

    @Test
    public void testDefaultConstants() {
        assertEquals("vt100-terminal.in", TerminalSettings.DEFAULT_INPUT_FILE_NAME);
        assertEquals("vt100-terminal.out", TerminalSettings.DEFAULT_OUTPUT_FILE_NAME);
        assertEquals(80, TerminalSettings.DEFAULT_COLUMNS);
        assertEquals(24, TerminalSettings.DEFAULT_ROWS);
        assertEquals(0, TerminalSettings.DEFAULT_INPUT_READ_DELAY_MILLIS);
    }
}

