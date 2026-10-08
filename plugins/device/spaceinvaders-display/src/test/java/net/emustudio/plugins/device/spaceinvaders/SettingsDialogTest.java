/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class SettingsDialogTest {
    private final PluginSettings settings = createMock(PluginSettings.class);
    private final Dialogs dialogs = createMock(Dialogs.class);
    private final AtomicInteger saves = new AtomicInteger();
    private GUI gui;
    private SettingsDialog dialog;

    @Before
    public void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        expect(settings.getInt("scale", 2)).andReturn(2).anyTimes();
        expect(settings.getBoolean("colorOverlay", true)).andReturn(true).anyTimes();
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false).anyTimes();
        expect(settings.getBoolean("soundEnabled", true)).andReturn(true).anyTimes();
        expect(settings.getString("soundSamplesDirectory", "examples/space-invaders/sounds"))
                .andReturn("examples/space-invaders/sounds").anyTimes();
        gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString()))
                .andAnswer(JPanel::new).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.button(anyString())).andAnswer(() -> new JButton((String) getCurrentArgument(0))).anyTimes();
        replay(gui);
    }

    @After
    public void tearDown() throws Exception {
        if (dialog != null) {
            SwingUtilities.invokeAndWait(dialog::dispose);
        }
    }

    private void createDialog() {
        replay(settings, dialogs);
        dialog = new SettingsDialog(null, settings, dialogs, gui, saves::incrementAndGet);
    }

    @Test
    public void escapeDiscardsEdits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            createDialog();
            assertEquals(2, spinner().getValue());
            assertTrue(button("Color overlay").isSelected());
            assertTrue(button("Sound enabled").isSelected());
            assertEquals("examples/space-invaders/sounds", directory().getText());
            spinner().setValue(4);
            directory().setText("other/sounds");
            JRootPane root = dialog.getRootPane();
            Object escape = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
            root.getActionMap().get(escape).actionPerformed(new ActionEvent(root, 0, "escape"));
            assertFalse(dialog.isDisplayable());
            assertEquals(0, saves.get());
        });
        verify(settings, dialogs);
    }

    @Test
    public void saveCommitsTypedScaleAndAllSettings() throws Exception {
        settings.setInt("scale", 3);
        settings.setBoolean("colorOverlay", false);
        settings.setBoolean("soundEnabled", false);
        settings.setString("soundSamplesDirectory", "other/sounds");
        SwingUtilities.invokeAndWait(() -> {
            createDialog();
            ((JSpinner.DefaultEditor) spinner().getEditor()).getTextField().setText("3");
            button("Color overlay").setSelected(false);
            button("Sound enabled").setSelected(false);
            directory().setText(" other/sounds ");
            dialog.getRootPane().getDefaultButton().doClick();
            assertEquals(1, saves.get());
            assertFalse(dialog.isDisplayable());
        });
        verify(settings, dialogs);
    }

    @Test
    public void browseUpdatesDirectoryAndCancelKeepsSelection() throws Exception {
        Path selected = Path.of("other/sounds");
        expect(dialogs.chooseDirectory("Sound samples directory", "Select",
                Path.of("examples/space-invaders/sounds"))).andReturn(Optional.of(selected));
        expect(dialogs.chooseDirectory("Sound samples directory", "Select", selected))
                .andReturn(Optional.empty());
        SwingUtilities.invokeAndWait(() -> {
            createDialog();
            button("Browse...").doClick();
            assertEquals(selected.toString(), directory().getText());
            button("Browse...").doClick();
            assertEquals(selected.toString(), directory().getText());
            assertEquals(0, saves.get());
        });
        verify(settings, dialogs);
    }

    @Test
    public void invalidInputKeepsDialogOpenAndDoesNotSave() throws Exception {
        dialogs.showError("Scale must be a positive integer.", "Save settings");
        dialogs.showError("Invalid sound samples directory.", "Save settings");
        expectLastCall().times(2);
        SwingUtilities.invokeAndWait(() -> {
            createDialog();
            ((JSpinner.DefaultEditor) spinner().getEditor()).getTextField().setText("0");
            button("Save").doClick();
            ((JSpinner.DefaultEditor) spinner().getEditor()).getTextField().setText("2");
            directory().setText(" ");
            button("Save").doClick();
            directory().setText("invalid\0path");
            button("Save").doClick();
            assertTrue(dialog.isDisplayable());
            assertEquals(0, saves.get());
        });
        verify(settings, dialogs);
    }

    @Test
    public void persistenceFailureKeepsDialogOpenAndDoesNotApply() throws Exception {
        settings.setInt("scale", 2);
        expectLastCall().andThrow(new CannotUpdateSettingException("Read-only settings"));
        dialogs.showError("Could not save settings. Please see log file for details.", "Save settings");
        SwingUtilities.invokeAndWait(() -> {
            createDialog();
            button("Save").doClick();
            assertTrue(dialog.isDisplayable());
            assertEquals(0, saves.get());
        });
        verify(settings, dialogs);
    }

    private JSpinner spinner() {
        return components(dialog.getContentPane(), JSpinner.class).get(0);
    }

    private JTextField directory() {
        return components(dialog.getContentPane(), JTextField.class).stream()
                .filter(field -> !(field instanceof JFormattedTextField)).findFirst().orElseThrow();
    }

    private AbstractButton button(String text) {
        return components(dialog.getContentPane(), AbstractButton.class).stream()
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> List<T> components(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) {
                result.add(type.cast(component));
            }
            if (component instanceof Container) {
                result.addAll(components((Container) component, type));
            }
        }
        return result;
    }
}
