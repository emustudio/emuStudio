/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class PaperTapeGuiTest {
    @Rule public final TemporaryFolder folder = new TemporaryFolder();
    private final PaperTapeUnit tape = new PaperTapeUnit();
    private final Dialogs dialogs = createStrictMock(Dialogs.class);
    private GUI gui;
    private PaperTapeGui window;
    private SettingsDialog settingsWindow;

    @Before public void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        gui = createNiceMock(GUI.class);
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.section(anyString(), anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.labelBold(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.button(anyString())).andAnswer(() -> new JButton((String) getCurrentArgument(0))).anyTimes();
        expect(gui.toolBar()).andAnswer(JToolBar::new).anyTimes();
        expect(gui.scrollPane(anyObject(Component.class))).andAnswer(() ->
                new JScrollPane((Component) getCurrentArgument(0))).anyTimes();
        expect(gui.splitPaneLeftToRight(anyObject(Component.class), anyObject(Component.class), anyDouble()))
                .andAnswer(() -> new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                        getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(gui.buttonBrowseDirectories(eq(dialogs), anyString(), anyString(), anyObject()))
                .andReturn(new JButton()).anyTimes();
        replay(gui);
    }

    @After public void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (window != null) { window.dispose(); }
            if (settingsWindow != null) { settingsWindow.dispose(); }
        });
        tape.close();
    }

    @Test public void browserFiltersSortsLoadsAndPreservesSelectionOnRefresh() throws Exception {
        Path input = folder.newFile("a.hex").toPath();
        Files.write(input, new byte[]{1, 2});
        folder.newFile("B.PT");
        folder.newFile("c.bin");
        folder.newFile("ignore.txt");
        folder.newFolder("directory.pt");
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            assertFalse(button(window, "Load").isEnabled());
            window.selectDirectory(folder.getRoot().toPath());
            JList<?> list = components(window, JList.class).get(0);
            assertEquals(3, list.getModel().getSize());
            assertEquals(input, list.getModel().getElementAt(0));
            list.setSelectedIndex(0);
            button(window, "Load").doClick();
            assertEquals(Optional.of(input), tape.getReaderPath());
            button(window, "Refresh").doClick();
            assertEquals(input, list.getSelectedValue());
            assertEquals(1, tape.read(PaperTapeUnit.DATA_PORT));
            button(window, "Rewind").doClick();
            assertEquals(0, tape.getReaderPosition());
            button(window, "Eject").doClick();
            assertTrue(tape.getReaderPath().isEmpty());
            assertFalse(button(window, "Rewind").isEnabled());
        });
        verify(dialogs);
    }

    @Test public void refreshShowsProgressWithoutConsumingDataOrEndMarker() throws Exception {
        Path input = folder.newFile("input.bin").toPath();
        Files.write(input, new byte[]{42});
        tape.attachReader(input);
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            window.refresh();
            assertEquals(0, tape.getReaderPosition());
            assertEquals(42, tape.read(PaperTapeUnit.DATA_PORT));
            window.refresh();
            JProgressBar progress = components(window, JProgressBar.class).get(0);
            assertEquals("1 / 1 bytes", progress.getString());
            assertEquals(1, progress.getValue());
            assertEquals(0x1A, tape.read(PaperTapeUnit.DATA_PORT));
        });
    }

    @Test public void punchFileDialogWritesUnchangedBytes() throws Exception {
        Path output = folder.newFile("output.pt").toPath();
        expect(dialogs.chooseFile(eq("Save punch tape"), eq("Save"), eq(Path.of(System.getProperty("user.dir"))), eq(true), EasyMock.<List<FileExtensionsFilter>>anyObject()))
                .andReturn(Optional.of(output));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            button(window, "Save tape").doClick();
            assertTrue(button(window, "Close output").isEnabled());
            tape.write(PaperTapeUnit.DATA_PORT, (byte) 0xFE);
            button(window, "Close output").doClick();
            assertTrue(tape.getPunchPath().isEmpty());
            assertFalse(button(window, "Close output").isEnabled());
        });
        assertArrayEquals(new byte[]{(byte) 0xFE}, Files.readAllBytes(output));
        verify(dialogs);
    }

    @Test public void failedLoadReportsErrorAndRetainsAttachedReader() throws Exception {
        Path input = folder.newFile("input.pt").toPath();
        Path missing = folder.newFile("missing.pt").toPath();
        tape.attachReader(input);
        dialogs.showError(anyString(), eq("Load reader tape"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            window.selectDirectory(folder.getRoot().toPath());
            components(window, JList.class).get(0).setSelectedValue(missing, true);
        });
        Files.delete(missing);
        SwingUtilities.invokeAndWait(() -> {
            button(window, "Load").doClick();
            assertEquals(Optional.of(input), tape.getReaderPath());
        });
        verify(dialogs);
    }

    @Test public void canceledPunchDialogPreservesBothAttachments() throws Exception {
        Path input = folder.newFile("input.pt").toPath();
        Path output = folder.newFile("output.pt").toPath();
        tape.attachReader(input);
        tape.attachPunch(output);
        expect(dialogs.chooseFile(anyString(), eq("Save"), eq(Path.of(System.getProperty("user.dir"))), eq(true), EasyMock.<List<FileExtensionsFilter>>anyObject()))
                .andReturn(Optional.empty());
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            button(window, "Save tape").doClick();
        });
        assertEquals(Optional.of(input), tape.getReaderPath());
        assertEquals(Optional.of(output), tape.getPunchPath());
        verify(dialogs);
    }

    @Test public void pollingStopsOnEscapeAndHideAndRestartsOnReopen() throws Exception {
        Path output = folder.newFile("output.pt").toPath();
        tape.attachPunch(output);
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createWindow();
            Timer timer = timer();
            assertFalse(timer.isRunning());
            window.setVisible(true);
            assertTrue(timer.isRunning());
            escape(window);
            assertFalse(timer.isRunning());
            assertEquals(Optional.of(output), tape.getPunchPath());
            window.setVisible(true);
            assertTrue(timer.isRunning());
            window.setVisible(false);
            assertFalse(timer.isRunning());
        });
    }

    @Test public void settingsSavePersistsStartupPreference() throws Exception {
        PluginSettings settings = createMock(PluginSettings.class);
        expect(settings.getBoolean("showGuiAtStartup", false)).andReturn(false);
        settings.setBoolean("showGuiAtStartup", true);
        replay(settings, dialogs);
        SwingUtilities.invokeAndWait(() -> {
            settingsWindow = new SettingsDialog(null, settings, dialogs, gui);
            components(settingsWindow, JCheckBox.class).get(0).setSelected(true);
            settingsWindow.getRootPane().getDefaultButton().doClick();
            assertFalse(settingsWindow.isDisplayable());
        });
        verify(settings, dialogs);
    }

    @Test public void startupPreferenceShowsGuiAfterPortsAttach() throws Exception {
        PluginSettings settings = createStrictMock(PluginSettings.class);
        expect(settings.getBoolean(PluginSettings.EMUSTUDIO_NO_GUI, false)).andReturn(false);
        expect(settings.getBoolean("showGuiAtStartup", false)).andReturn(true);
        Context8080 cpu = createNiceMock(Context8080.class);
        expect(cpu.attachDevice(eq(0x12), anyObject())).andReturn(true);
        expect(cpu.attachDevice(eq(0x13), anyObject())).andReturn(true);
        ContextPool pool = createNiceMock(ContextPool.class);
        expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
        ApplicationApi api = createStrictMock(ApplicationApi.class);
        expect(api.getContextPool()).andReturn(pool).times(2);
        expect(api.getDialogs()).andReturn(dialogs);
        expect(api.getGUI()).andReturn(gui);
        replay(settings, cpu, pool, api, dialogs);
        SwingUtilities.invokeAndWait(() -> {
            DeviceImpl device = new DeviceImpl(0, api, settings);
            try {
                device.initialize();
                assertTrue(java.util.Arrays.stream(java.awt.Window.getWindows())
                        .anyMatch(w -> w instanceof PaperTapeGui && w.isShowing()));
            } catch (Exception e) {
                throw new AssertionError(e);
            } finally {
                device.destroy();
            }
        });
        verify(settings, cpu, pool, api, dialogs);
    }

    @Test public void settingsEscapeDiscardsDraft() throws Exception {
        PluginSettings settings = createStrictMock(PluginSettings.class);
        expect(settings.getBoolean("showGuiAtStartup", false)).andReturn(true);
        replay(settings, dialogs);
        SwingUtilities.invokeAndWait(() -> {
            settingsWindow = new SettingsDialog(null, settings, dialogs, gui);
            components(settingsWindow, JCheckBox.class).get(0).setSelected(false);
            escape(settingsWindow);
            assertFalse(settingsWindow.isDisplayable());
        });
        verify(settings, dialogs);
    }

    @Test public void settingsPersistenceFailureKeepsDraftOpen() throws Exception {
        PluginSettings settings = createStrictMock(PluginSettings.class);
        expect(settings.getBoolean("showGuiAtStartup", false)).andReturn(false);
        settings.setBoolean("showGuiAtStartup", true);
        expectLastCall().andThrow(new CannotUpdateSettingException("Read-only settings"));
        dialogs.showError(eq("Read-only settings"), eq("Save settings"));
        replay(settings, dialogs);
        SwingUtilities.invokeAndWait(() -> {
            settingsWindow = new SettingsDialog(null, settings, dialogs, gui);
            components(settingsWindow, JCheckBox.class).get(0).setSelected(true);
            settingsWindow.getRootPane().getDefaultButton().doClick();
            assertTrue(settingsWindow.isDisplayable());
            assertTrue(components(settingsWindow, JCheckBox.class).get(0).isSelected());
        });
        verify(settings, dialogs);
    }

    private void createWindow() { window = new PaperTapeGui(null, tape, dialogs, gui); }

    private Timer timer() {
        try {
            Field field = PaperTapeGui.class.getDeclaredField("refreshTimer");
            field.setAccessible(true);
            return (Timer) field.get(window);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void escape(JDialog window) {
        JRootPane root = window.getRootPane();
        Object action = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        root.getActionMap().get(action).actionPerformed(new ActionEvent(root, 0, "escape"));
    }

    private static AbstractButton button(Container container, String text) {
        return components(container, AbstractButton.class).stream()
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T> List<T> components(Container container, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) { found.add(type.cast(child)); }
            if (child instanceof Container) { found.addAll(components((Container) child, type)); }
        }
        return found;
    }
}
