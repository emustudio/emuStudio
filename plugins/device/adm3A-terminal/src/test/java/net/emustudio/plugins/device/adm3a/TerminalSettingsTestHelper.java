/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.adm3a;

import net.emustudio.emulib.application.settings.PluginSettings;
import net.emustudio.emulib.application.ui.Dialogs;

import java.util.Optional;

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
        expect(pluginSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI)).andReturn(Optional.of(true)).anyTimes();
        expect(pluginSettings.getBoolean(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(pluginSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(pluginSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
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
        expect(pluginSettings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI)).andReturn(Optional.of(true)).anyTimes();
        expect(pluginSettings.getBoolean(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(pluginSettings.getString("inputFileName")).andReturn(Optional.ofNullable(inputPath)).anyTimes();
        expect(pluginSettings.getString("outputFileName")).andReturn(Optional.ofNullable(outputPath)).anyTimes();
        expect(pluginSettings.getString(anyString())).andReturn(Optional.empty()).anyTimes();
        expect(pluginSettings.getInt("inputReadDelayMillis")).andReturn(Optional.of(inputReadDelayMillis)).anyTimes();
        expect(pluginSettings.getInt(anyString())).andReturn(Optional.empty()).anyTimes();
        replay(pluginSettings);

        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);

        return new TerminalSettings(pluginSettings, dialogs);
    }
}

