/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.dialogs;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.application.gui.AbstractSwingTest;
import net.emustudio.application.gui.framework.EmuStudioGui;
import net.emustudio.application.gui.schema.DrawingModel;
import net.emustudio.application.gui.schema.DrawingPanel;
import net.emustudio.application.gui.schema.Schema;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.application.settings.ComputerConfig;
import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.ui.Dialogs;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class SchemaEditorDialogTest extends AbstractSwingTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void gridControlsReflectInitialSchemaSettings() throws Exception {
        AppSettings appSettings = new AppSettings(Config.inMemory(), false, false);
        appSettings.setUseSchemaGrid(false);
        appSettings.setSchemaGridGap(42);

        try (ComputerConfig computerConfig = createComputerConfig("Schema test")) {
            Schema schema = new Schema(computerConfig, appSettings);
            SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

            showDialog(dialog);

            JToggleButton gridButton = findToggleButtonByTooltip(dialog, "Set/unset using grid");
            JSlider gridSlider = findComponent(dialog, JSlider.class, slider -> true);

            assertFalse(onEdt(gridButton::isSelected));
            assertEquals(42, onEdt(gridSlider::getValue).intValue());
        }
    }

    @Test
    public void saveButtonPersistsSchemaAndClosesDialog() {
        Dialogs dialogs = mock(Dialogs.class);
        Schema schema = mockSchema("Save test");
        SchemaEditorDialog dialog = createDialog(schema, dialogs);

        showDialog(dialog);
        triggerButton(findButtonByTooltip(dialog, "Save & Close"));

        verify(schema).save();
        assertFalse(onEdt(dialog::isDisplayable));
    }

    @Test
    public void deleteKeyRemovesSelectedSchemaItems() {
        Schema schema = mockSchema("Delete test");
        SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

        showDialog(dialog);
        runOnEdt(() -> dialog.keyPressed(new KeyEvent(
                dialog, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE, KeyEvent.CHAR_UNDEFINED
        )));

        verify(schema).deleteSelected();
    }

    @Test
    public void escapeKeyCancelsDrawingAndClearsSelection() throws Exception {
        Schema schema = mockSchema("Escape test");
        SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

        showDialog(dialog);
        setDrawingTool(dialog, DrawingPanel.Tool.TOOL_DELETE, "device.jar");

        runOnEdt(() -> dialog.keyPressed(new KeyEvent(
                dialog, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED
        )));

        verify(schema).select(-1, -1, 0, 0);
        assertEquals(DrawingPanel.Tool.TOOL_NOTHING, getDrawingModel(dialog).drawTool);
    }

    @Test
    public void pluginButtonsPopulateComboSetToolAndSecondClickClearsSelection() throws Exception {
        assertPluginButtonBehavior("Set compiler", PLUGIN_TYPE.COMPILER, DrawingPanel.Tool.TOOL_COMPILER);
        assertPluginButtonBehavior("Set CPU", PLUGIN_TYPE.CPU, DrawingPanel.Tool.TOOL_CPU);
        assertPluginButtonBehavior("Set operating memory", PLUGIN_TYPE.MEMORY, DrawingPanel.Tool.TOOL_MEMORY);
        assertPluginButtonBehavior("Add device", PLUGIN_TYPE.DEVICE, DrawingPanel.Tool.TOOL_DEVICE);
    }

    @Test
    public void lineButtonTogglesConnectionMode() throws Exception {
        SchemaEditorDialog dialog = createDialog(mockSchema("Line test"), mock(Dialogs.class));

        showDialog(dialog);
        JToggleButton lineButton = findToggleButtonByTooltip(dialog, "Add connection");
        JComboBox<?> pluginCombo = findComponent(dialog, JComboBox.class, combo -> true);

        triggerButton(lineButton);

        assertEquals(DrawingPanel.Tool.TOOL_CONNECTION, getDrawingModel(dialog).drawTool);
        assertTrue(readButtonSelected(dialog));
        assertTrue(onEdt(lineButton::isSelected));
        assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());

        triggerButton(lineButton);

        assertEquals(DrawingPanel.Tool.TOOL_NOTHING, getDrawingModel(dialog).drawTool);
        assertFalse(readButtonSelected(dialog));
        assertFalse(onEdt(lineButton::isSelected));
        assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());
    }

    @Test
    public void deleteButtonTogglesDeleteMode() throws Exception {
        SchemaEditorDialog dialog = createDialog(mockSchema("Delete mode"), mock(Dialogs.class));

        showDialog(dialog);
        JToggleButton deleteButton = findToggleButtonByTooltip(dialog, "Delete component or connection");
        JComboBox<?> pluginCombo = findComponent(dialog, JComboBox.class, combo -> true);

        triggerButton(deleteButton);

        assertEquals(DrawingPanel.Tool.TOOL_DELETE, getDrawingModel(dialog).drawTool);
        assertTrue(readButtonSelected(dialog));
        assertTrue(onEdt(deleteButton::isSelected));
        assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());

        triggerButton(deleteButton);

        assertEquals(DrawingPanel.Tool.TOOL_NOTHING, getDrawingModel(dialog).drawTool);
        assertFalse(readButtonSelected(dialog));
        assertFalse(onEdt(deleteButton::isSelected));
        assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());
    }

    @Test
    public void gridAndBidirectionButtonsUpdateSchemaAndDrawingModel() throws Exception {
        Schema schema = mockSchema("Controls test");
        SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

        showDialog(dialog);
        JToggleButton gridButton = findToggleButtonByTooltip(dialog, "Set/unset using grid");
        JToggleButton bidirectionButton = findToggleButtonByTooltip(dialog, "Bidirectional connection");
        JSlider gridSlider = findComponent(dialog, JSlider.class, slider -> true);

        runOnEdt(() -> gridSlider.setValue(35));
        verify(schema, atLeastOnce()).setSchemaGridGap(35);

        triggerButton(gridButton);

        verify(schema, atLeastOnce()).setUseSchemaGrid(false);
        verify(schema, atLeastOnce()).setSchemaGridGap(35);
        assertFalse(onEdt(gridSlider::isEnabled));

        triggerButton(bidirectionButton);

        assertFalse(getDrawingModel(dialog).bidirectional);
    }

    @Test
    public void toolListenerResetsToolbarStateAfterToolUse() throws Exception {
        Path plugin = createPluginFile(PLUGIN_TYPE.COMPILER);
        try (ComputerConfig computerConfig = createComputerConfig("Listener test")) {
            Schema schema = new Schema(computerConfig, new AppSettings(Config.inMemory(), false, false));
            SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

            showDialog(dialog);
            JToggleButton compilerButton = findToggleButtonByTooltip(dialog, "Set compiler");
            JComboBox<?> pluginCombo = findComponent(dialog, JComboBox.class, combo -> true);

            triggerButton(compilerButton);
            getDrawingPanel(dialog).fireToolWasUsed();

            assertEquals(DrawingPanel.Tool.TOOL_NOTHING, getDrawingModel(dialog).drawTool);
            assertFalse(readButtonSelected(dialog));
            assertFalse(onEdt(compilerButton::isSelected));
            assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());
        } finally {
            cleanupPluginFile(plugin, PLUGIN_TYPE.COMPILER);
        }
    }

    @Test
    public void getSchemaAndEscapeSettingsReflectDialogConfiguration() throws Exception {
        Schema schema = mockSchema("Getter test");
        SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

        assertSame(schema, dialog.getSchema());
        assertFalse(shouldCloseOnEscape(dialog));

        dialog.keyTyped(new KeyEvent(dialog, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, 0, 'a'));
        dialog.keyReleased(new KeyEvent(dialog, KeyEvent.KEY_RELEASED, System.currentTimeMillis(), 0, KeyEvent.VK_A, 'a'));
    }

    @Test
    public void saveButtonShowsErrorWhenSchemaSaveFails() throws Exception {
        Dialogs dialogs = mock(Dialogs.class);
        Schema schema = mockSchema("Save error");
        doThrow(new CannotUpdateSettingException("boom")).when(schema).save();
        SchemaEditorDialog dialog = createDialog(schema, dialogs);

        showDialog(dialog);
        triggerButton(findButtonByTooltip(dialog, "Save & Close"));

        verify(dialogs).showError(
                "Could not save computer schema. Please consult log file for details.",
                "Save schema"
        );
        assertFalse(onEdt(dialog::isDisplayable));
    }

    private SchemaEditorDialog createDialog(Schema schema, Dialogs dialogs) {
        return onEdt(() -> {
            SchemaEditorDialog dialog = new SchemaEditorDialog(new JDialog(), schema, dialogs, new EmuStudioGui());
            dialog.setModal(false);
            return dialog;
        });
    }

    private Schema mockSchema(String computerName) {
        Schema schema = mock(Schema.class);
        ComputerConfig computerConfig = mock(ComputerConfig.class);

        when(computerConfig.getName()).thenReturn(computerName);
        when(schema.getComputerConfig()).thenReturn(computerConfig);
        when(schema.useSchemaGrid()).thenReturn(true);
        when(schema.getSchemaGridGap()).thenReturn(20);

        return schema;
    }

    private ComputerConfig createComputerConfig(String computerName) throws IOException {
        FileConfig config = FileConfig.of(temporaryFolder.newFile(computerName.replace(' ', '_') + ".toml"));
        config.set("name", computerName);
        return new ComputerConfig(config);
    }

    private void assertPluginButtonBehavior(String tooltip, PLUGIN_TYPE pluginType, DrawingPanel.Tool expectedTool)
            throws Exception {
        Path pluginFile = createPluginFile(pluginType);

        try (ComputerConfig computerConfig = createComputerConfig(pluginType.name() + " test")) {
            Schema schema = new Schema(computerConfig, new AppSettings(Config.inMemory(), false, false));
            SchemaEditorDialog dialog = createDialog(schema, mock(Dialogs.class));

            showDialog(dialog);

            JToggleButton button = findToggleButtonByTooltip(dialog, tooltip);
            JComboBox<?> pluginCombo = findComponent(dialog, JComboBox.class, combo -> true);

            triggerButton(button);

            assertEquals(1, onEdt(pluginCombo::getItemCount).intValue());
            assertEquals(displayName(pluginFile), onEdt(() -> String.valueOf(pluginCombo.getSelectedItem())));
            assertEquals(expectedTool, getDrawingModel(dialog).drawTool);
            assertEquals(pluginFile.getFileName().toString(), getDrawingModel(dialog).pluginFileName);
            assertTrue(readButtonSelected(dialog));

            triggerButton(button);

            assertEquals(0, onEdt(pluginCombo::getItemCount).intValue());
            assertEquals(DrawingPanel.Tool.TOOL_NOTHING, getDrawingModel(dialog).drawTool);
            assertFalse(readButtonSelected(dialog));
            assertFalse(onEdt(button::isSelected));
        } finally {
            cleanupPluginFile(pluginFile, pluginType);
        }
    }

    private DrawingPanel getDrawingPanel(SchemaEditorDialog dialog) throws Exception {
        return getField(dialog, "panel", DrawingPanel.class);
    }

    private DrawingModel getDrawingModel(SchemaEditorDialog dialog) throws Exception {
        return getField(getDrawingPanel(dialog), "drawingModel", DrawingModel.class);
    }

    private boolean readButtonSelected(SchemaEditorDialog dialog) throws Exception {
        return getField(dialog, "buttonSelected", Boolean.class);
    }

    private void setDrawingTool(SchemaEditorDialog dialog, DrawingPanel.Tool tool, String fileName) throws Exception {
        runOnEdt(() -> {
            try {
                getDrawingPanel(dialog).setTool(tool, fileName);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private boolean shouldCloseOnEscape(SchemaEditorDialog dialog) throws Exception {
        Method method = SchemaEditorDialog.class.getDeclaredMethod("shouldCloseOnEscape");
        method.setAccessible(true);
        return (Boolean) method.invoke(dialog);
    }

    private Path createPluginFile(PLUGIN_TYPE pluginType) throws IOException {
        Path pluginDir = Path.of(System.getProperty("user.dir"), pluginDirName(pluginType));
        Files.createDirectories(pluginDir);

        Path pluginFile = pluginDir.resolve("schema-editor-" + pluginType.name().toLowerCase() + ".jar");
        Files.write(pluginFile, new byte[0]);
        return pluginFile;
    }

    private void cleanupPluginFile(Path pluginFile, PLUGIN_TYPE pluginType) throws IOException {
        Files.deleteIfExists(pluginFile);

        Path pluginDir = Path.of(System.getProperty("user.dir"), pluginDirName(pluginType));
        if (Files.isDirectory(pluginDir) && isDirectoryEmpty(pluginDir)) {
            Files.deleteIfExists(pluginDir);
        }
    }

    private boolean isDirectoryEmpty(Path dir) throws IOException {
        try (var entries = Files.list(dir)) {
            return entries.findAny().isEmpty();
        }
    }

    private String pluginDirName(PLUGIN_TYPE pluginType) {
        switch (pluginType) {
            case COMPILER:
                return "compiler";
            case CPU:
                return "cpu";
            case MEMORY:
                return "memory";
            case DEVICE:
                return "device";
            default:
                throw new IllegalArgumentException("Unexpected plugin type: " + pluginType);
        }
    }

    private String displayName(Path pluginFile) {
        String fileName = pluginFile.getFileName().toString();
        return fileName.substring(0, fileName.length() - ".jar".length());
    }

    private <T> T getField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
