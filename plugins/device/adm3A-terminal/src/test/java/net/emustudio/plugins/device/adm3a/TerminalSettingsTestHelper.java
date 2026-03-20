/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;

import static org.easymock.EasyMock.*;

/**
 * Helper class to create TerminalSettings for tests outside the package.
 * The TerminalSettings constructor is package-private, so this helper
 * (in the same package) provides test-accessible factory methods.
 */
public class TerminalSettingsTestHelper {

    /**
     * Creates TerminalSettings with NoGUI mode backed by nice mocks.
     */
    public static TerminalSettings createNoGuiSettings() {
        PluginSettings pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(pluginSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(pluginSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(pluginSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(pluginSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        return new TerminalSettings(pluginSettings, dialogs);
    }

    /**
     * Creates TerminalSettings with custom input/output file paths and delay.
     */
    public static TerminalSettings createNoGuiSettings(String inputPath, String outputPath, int inputReadDelayMillis) {
        PluginSettings pluginSettings = createNiceMock(PluginSettings.class);
        expect(pluginSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(true).anyTimes();
        expect(pluginSettings.getBoolean(anyString(), anyBoolean())).andReturn(false).anyTimes();
        expect(pluginSettings.getString(eq("inputFileName"), anyString())).andReturn(inputPath).anyTimes();
        expect(pluginSettings.getString(eq("outputFileName"), anyString())).andReturn(outputPath).anyTimes();
        expect(pluginSettings.getString(anyString(), anyString())).andAnswer(() -> (String) getCurrentArguments()[1]).anyTimes();
        expect(pluginSettings.getInt(eq("inputReadDelayMillis"), anyInt())).andReturn(inputReadDelayMillis).anyTimes();
        expect(pluginSettings.getInt(anyString(), anyInt())).andAnswer(() -> (Integer) getCurrentArguments()[1]).anyTimes();
        replay(pluginSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        return new TerminalSettings(pluginSettings, dialogs);
    }
}

