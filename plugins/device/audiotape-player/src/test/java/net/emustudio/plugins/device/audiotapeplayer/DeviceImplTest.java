/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotapeplayer;

import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.plugin.cpu.CPUContext;
import net.emustudio.emulib.plugin.device.DeviceContext;
import net.emustudio.plugins.device.audiotapeplayer.gui.SettingsDialog;
import org.easymock.Capture;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DeviceImplTest {
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void startsConfiguredTapeWithoutGui() throws Exception {
        assertConfiguredTapeStartsWithoutOpeningWindow(true);
    }

    @Test
    public void startsConfiguredTapeBeforeOpeningWindowWhenGuiIsSupported() throws Exception {
        assertConfiguredTapeStartsWithoutOpeningWindow(false);
    }

    @SuppressWarnings("unchecked")
    private void assertConfiguredTapeStartsWithoutOpeningWindow(boolean noGui) throws Exception {
        Path tape = folder.newFile("test.tap").toPath();
        Files.write(tape, new byte[]{2, 0, (byte) 0xff, (byte) 0xff});

        Capture<CPUContext.PassedCyclesListener> playback = newCapture();
        CPUContext cpu = mock(CPUContext.class);
        expect(cpu.getCPUFrequency()).andReturn(3500).anyTimes();
        cpu.addPassedCyclesListener(capture(playback));
        cpu.removePassedCyclesListener(anyObject());

        AtomicBoolean played = new AtomicBoolean();
        DeviceContext<Byte> lineIn = mock(DeviceContext.class);
        lineIn.writeData(anyByte());
        expectLastCall().andAnswer(() -> {
            if ((byte) getCurrentArgument(0) == 1) played.set(true);
            return null;
        }).anyTimes();

        ContextPool contexts = mock(ContextPool.class);
        expect(contexts.get(1L, CPUContext.class)).andReturn(cpu);
        expect(contexts.getDevice(1L, Byte.class)).andReturn(lineIn);
        ApplicationApi api = mock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(contexts);

        PluginSettings settings = niceMock(PluginSettings.class);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI)).andReturn(Optional.of(noGui));
        expect(settings.getBoolean(SettingsDialog.SETTINGS_KEY_SHOW_GUI_AT_STARTUP)).andStubReturn(Optional.of(false));
        expect(settings.getArray(SettingsDialog.SETTINGS_KEY_EVENTS)).andReturn(List.of("LOAD_TAPE:" + tape, "PLAY:"));
        replay(cpu, lineIn, contexts, api, settings);

        DeviceImpl device = new DeviceImpl(1L, api, settings);
        try {
            device.initialize();
            device.reset();
            playback.getValue().passedCycles(30_000_000);
            assertFalse(played.get());

            device.startAutomation().get(2, TimeUnit.SECONDS);
            playback.getValue().passedCycles(30_000_000);
            assertTrue(played.get());
        } finally {
            device.destroy();
        }
        verify(cpu, lineIn, contexts, api, settings);
    }
}
