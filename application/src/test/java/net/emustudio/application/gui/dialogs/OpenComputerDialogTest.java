/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class OpenComputerDialogTest extends AbstractSwingTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void openButtonRequiresSelectedComputer() {
        Dialogs dialogs = mock(Dialogs.class);
        Consumer<ComputerConfig> selectComputer = mock(Consumer.class);
        OpenComputerDialog dialog = createDialog(dialogs, selectComputer);

        showDialog(dialog);

        JList<?> computerList = findComponent(dialog, JList.class, list -> true);
        runOnEdt(computerList::clearSelection);
        triggerButton(findButton(dialog, "Open computer"));

        verify(dialogs).showError("A computer has to be selected!", "Open computer");
        verifyNoInteractions(selectComputer);
    }

    @Test
    public void exitButtonClosesDialog() {
        OpenComputerDialog dialog = createDialog(mock(Dialogs.class), mock(Consumer.class));

        showDialog(dialog);
        triggerButton(findButton(dialog, "Exit"));

        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void configurationIsBelowSchemaWithMovableDivider() {
        OpenComputerDialog dialog = createDialog(mock(Dialogs.class), mock(Consumer.class));
        showDialog(dialog);

        JSplitPane split = findComponent(dialog, JSplitPane.class,
                pane -> pane.getOrientation() == JSplitPane.VERTICAL_SPLIT);
        assertTrue(onEdt(() -> split.getTopComponent() instanceof JScrollPane));
        assertTrue(onEdt(() -> split.getBottomComponent() instanceof JScrollPane));
        runOnEdt(() -> split.setDividerLocation(150));
        assertEquals(150, (int) onEdt(split::getDividerLocation));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void selectionShowsReadOnlyPluginTablesAndClearsOldConfiguration() throws IOException {
        Path configFile = temporaryFolder.newFile("computer.toml").toPath();
        StringBuilder toml = new StringBuilder("name = 'Test computer'\n");
        toml.append(pluginConfig("CPU", "cpu", "test-cpu", "speed = 1000\n# hidden = true\n"));
        toml.append(pluginConfig("MEMORY", "memory", "test-memory",
                "banks = [1, 2]\n[MEMORY.settings.options]\nenabled = true\n"));
        toml.append(pluginConfig("COMPILER", "compiler", "test-compiler", ""));
        toml.append(pluginConfig("[DEVICE]", "input", "tape", "editable = false\n"));
        toml.append(pluginConfig("[DEVICE]", "output", "tape", "editable = true\n"));
        toml.append("[[connections]]\nfrom = 'cpu'\nto = 'memory'\nbidirectional = true\npoints = []\n");
        Files.writeString(configFile, toml);

        try (ComputerConfig computer = ComputerConfig.load(configFile)) {
            OpenComputerDialog dialog = createDialog(mock(Dialogs.class), mock(Consumer.class));
            showDialog(dialog);
            JList<ComputerConfig> computerList = (JList<ComputerConfig>) findComponent(dialog, JList.class, list -> true);
            DefaultListModel<ComputerConfig> model = new DefaultListModel<>();
            model.addElement(computer);
            runOnEdt(() -> {
                computerList.setModel(model);
                computerList.setSelectedIndex(0);
            });

            JSplitPane split = findComponent(dialog, JSplitPane.class,
                    pane -> pane.getOrientation() == JSplitPane.VERTICAL_SPLIT);
            JPanel configuration = onEdt(() -> (JPanel) ((JScrollPane) split.getBottomComponent()).getViewport().getView());
            List<JTable> tables = onEdt(() -> Arrays.stream(configuration.getComponents())
                    .filter(JTable.class::isInstance).map(JTable.class::cast).collect(Collectors.toList()));
            assertEquals(5, tables.size());
            assertEquals(Arrays.asList("CPU", "Memory", "Compiler", "Device (tape)", "Device (tape)"),
                    tables.stream().map(Component::getName).collect(Collectors.toList()));
            assertRow(tables.get(0), 0, "name", "test-cpu");
            assertRow(tables.get(0), 1, "path", "test-cpu.jar");
            assertRow(tables.get(0), 2, "speed", "1000");
            assertRow(tables.get(1), 2, "banks", "[1, 2]");
            assertRow(tables.get(1), 3, "options.enabled", "true");
            assertEquals(2, tables.get(2).getRowCount());
            assertRow(tables.get(3), 2, "editable", "false");
            assertRow(tables.get(4), 2, "editable", "true");
            for (JTable table : tables) {
                assertNull(table.getTableHeader());
                for (int row = 0; row < table.getRowCount(); row++) {
                    assertFalse(table.isCellEditable(row, 0));
                    assertFalse(table.isCellEditable(row, 1));
                }
            }
            assertEquals(3, tables.get(0).getRowCount());
            assertEquals(toml.toString(), Files.readString(configFile));

            runOnEdt(computerList::clearSelection);
            assertEquals(0, (int) onEdt(configuration::getComponentCount));
            runOnEdt(() -> computerList.setSelectedIndex(0));
            runOnEdt(dialog::update);
            assertEquals(0, (int) onEdt(configuration::getComponentCount));
        }
    }

    private String pluginConfig(String section, String id, String name, String settings) {
        String type = section.replace("[", "").replace("]", "");
        return "[" + section + "]\nid = '" + id + "'\nname = '" + name + "'\npath = '" + name
                + ".jar'\ntype = '" + type + "'\nschemaPoint = '80,60'\n[" + type + ".settings]\n" + settings;
    }

    private void assertRow(JTable table, int row, String key, String value) {
        assertEquals(key, onEdt(() -> table.getValueAt(row, 0)));
        assertEquals(value, onEdt(() -> table.getValueAt(row, 1)));
    }

    private OpenComputerDialog createDialog(Dialogs dialogs, Consumer<ComputerConfig> selectComputer) {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);
        return onEdt(() -> {
            OpenComputerDialog dialog = new OpenComputerDialog(appSettings, dialogs, selectComputer, new EmuStudioGui());
            dialog.setModal(false);
            return dialog;
        });
    }
}
