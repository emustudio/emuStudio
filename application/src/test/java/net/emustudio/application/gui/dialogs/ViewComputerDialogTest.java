/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.GUIImpl;
import net.emustudio.application.gui.framework.P;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.application.settings.PluginConfig;
import net.emustudio.application.virtualcomputer.VirtualComputer;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.device.Device;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class ViewComputerDialogTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void dialogShowsCpuInfoByDefault() throws Exception {
        try (ComputerConfig computerConfig = createComputerConfig()) {
            DialogFixture fixture = createFixture(computerConfig);
            ViewComputerDialog dialog = createDialog(fixture.computer, fixture.appSettings);

            showDialog(dialog);

            JComboBox<?> deviceCombo = findComponent(dialog, JComboBox.class, combo -> true);
            JTextArea description = findComponent(dialog, JTextArea.class, area -> true);

            assertFalse(onEdt(deviceCombo::isVisible));
            assertEquals("CPU description", onEdt(description::getText));
            findLabel(dialog, "Demo computer");
            findLabel(dialog, "Z80 CPU");
            findLabel(dialog, "cpu.jar");
            findLabel(dialog, "1.0");
            findLabel(dialog, "CPU copyright");
        }
    }

    @Test
    public void deviceToolbarSwitchesToSelectedDeviceInfo() throws Exception {
        try (ComputerConfig computerConfig = createComputerConfig()) {
            DialogFixture fixture = createFixture(computerConfig);
            ViewComputerDialog dialog = createDialog(fixture.computer, fixture.appSettings);

            showDialog(dialog);
            triggerButton(findToggleButtonByTooltip(dialog, "Devices information"));

            JComboBox<?> deviceCombo = findComponent(dialog, JComboBox.class, combo -> true);
            JTextArea description = findComponent(dialog, JTextArea.class, area -> true);

            assertTrue(onEdt(deviceCombo::isVisible));
            assertEquals(0, onEdt(deviceCombo::getSelectedIndex).intValue());
            assertEquals("Device description", onEdt(description::getText));
            findLabel(dialog, "Terminal");
            findLabel(dialog, "device.jar");
            findLabel(dialog, "2.0");
            findLabel(dialog, "Device copyright");
        }
    }

    private ViewComputerDialog createDialog(VirtualComputer computer, AppSettings appSettings) {
        return onEdt(() -> {
            ViewComputerDialog dialog = new ViewComputerDialog(
                    new JFrame(), computer, appSettings, mock(Dialogs.class), new GUIImpl()
            );
            dialog.setModal(false);
            return dialog;
        });
    }

    private DialogFixture createFixture(ComputerConfig computerConfig) {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);

        computerConfig.setCPU(pluginConfig("cpu", PLUGIN_TYPE.CPU, "cpu.jar", 10, 10));
        computerConfig.setDevices(List.of(pluginConfig("device", PLUGIN_TYPE.DEVICE, "device.jar", 40, 40)));

        CPU cpu = mock(CPU.class);
        when(cpu.getTitle()).thenReturn("Z80 CPU");
        when(cpu.getVersion()).thenReturn("1.0");
        when(cpu.getCopyright()).thenReturn("CPU copyright");
        when(cpu.getDescription()).thenReturn("CPU description");

        Device device = mock(Device.class);
        when(device.getTitle()).thenReturn("Terminal");
        when(device.getVersion()).thenReturn("2.0");
        when(device.getCopyright()).thenReturn("Device copyright");
        when(device.getDescription()).thenReturn("Device description");

        VirtualComputer computer = mock(VirtualComputer.class);
        when(computer.getComputerConfig()).thenReturn(computerConfig);
        when(computer.getCompiler()).thenReturn(Optional.empty());
        when(computer.getCPU()).thenReturn(Optional.of(cpu));
        when(computer.getMemory()).thenReturn(Optional.empty());
        when(computer.getDevices()).thenReturn(List.of(device));

        return new DialogFixture(computer, appSettings);
    }

    private ComputerConfig createComputerConfig() throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile("view-computer.toml"));
        config.set("name", "Demo computer");
        return new ComputerConfig(config);
    }

    private PluginConfig pluginConfig(String id, PLUGIN_TYPE type, String pluginFile, int x, int y) {
        return PluginConfig.create(id, type, id, pluginFile, P.of(x, y), Config.inMemory());
    }

    private static final class DialogFixture {
        private final VirtualComputer computer;
        private final AppSettings appSettings;

        private DialogFixture(VirtualComputer computer, AppSettings appSettings) {
            this.computer = computer;
            this.appSettings = appSettings;
        }
    }
}
