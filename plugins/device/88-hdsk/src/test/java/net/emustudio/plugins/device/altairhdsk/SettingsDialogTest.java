/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.ContextPool;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.device.mits88pio.PioBoard;
import net.emustudio.plugins.device.mits88pio.api.PioContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import org.easymock.Capture;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class SettingsDialogTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private final Map<String, Object> values = new HashMap<>();
    private final PluginSettings settings = createMock(PluginSettings.class);
    private final Dialogs dialogs = createMock(Dialogs.class);
    private final AtomicInteger saves = new AtomicInteger();
    private GUI gui;
    private SettingsDialog dialog;
    private RuntimeException persistenceFailure;
    private DeviceImpl device;
    private HdskController liveSimh;
    private MhdskController liveMits;

    @Before public void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        expect(settings.getString(anyString(), anyString())).andAnswer(() ->
                (String) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(settings.getString(anyString())).andAnswer(() ->
                Optional.ofNullable((String) values.get(getCurrentArgument(0)))).anyTimes();
        expect(settings.getInt(anyString(), anyInt())).andAnswer(() ->
                (int) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(settings.getBoolean(anyString(), anyBoolean())).andAnswer(() ->
                (boolean) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        settings.setString(anyString(), anyString());
        expectLastCall().andAnswer(() -> put()).anyTimes();
        settings.setInt(anyString(), anyInt());
        expectLastCall().andAnswer(() -> put()).anyTimes();
        settings.setBoolean(anyString(), anyBoolean());
        expectLastCall().andAnswer(() -> put()).anyTimes();
        settings.remove(anyString());
        expectLastCall().andAnswer(() -> {
            if (persistenceFailure != null) { throw persistenceFailure; }
            values.remove(getCurrentArgument(0));
            return null;
        }).anyTimes();
        replay(settings);

        gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.button(anyString())).andAnswer(() -> new JButton((String) getCurrentArgument(0))).anyTimes();
        replay(gui);
    }

    private Object put() {
        if (persistenceFailure != null) { throw persistenceFailure; }
        values.put(getCurrentArgument(0), getCurrentArgument(1));
        return null;
    }

    @After public void tearDown() throws Exception {
        if (dialog != null) { SwingUtilities.invokeAndWait(dialog::dispose); }
        if (device != null) { device.destroy(); }
    }

    private void createDialog(boolean mits) {
        dialog = new SettingsDialog(null, settings, mits, null, null, dialogs, gui, saves::incrementAndGet);
    }

    @SuppressWarnings("unchecked")
    private void createLiveDialog(boolean mits) {
        try {
            ContextPool pool = createNiceMock(ContextPool.class);
            ApplicationApi api = createNiceMock(ApplicationApi.class);
            expect(api.getContextPool()).andReturn(pool).anyTimes();
            Capture<PioContext.Peripheral> physical = newCapture();
            Capture<Context8080.CpuPortDevice> synthetic = newCapture();
            if (mits) {
                values.put("controllerType", "MITS");
                PioContext pio = createNiceMock(PioContext.class);
                expect(pio.getChannelCount()).andReturn(4).anyTimes();
                pio.attachPeripheral(capture(physical));
                expect(pool.getContext(0, PioContext.class, 0)).andReturn(pio);
                replay(pio);
            } else {
                Context8080 cpu = createNiceMock(Context8080.class);
                MemoryContext<Byte> memory = createNiceMock(MemoryContext.class);
                expect(memory.getCellTypeClass()).andReturn(Byte.class);
                expect(cpu.attachDevice(eq(0xFD), capture(synthetic))).andReturn(true);
                expect(pool.getCPUContext(0, Context8080.class)).andReturn(cpu);
                expect(pool.getMemoryContext(0, MemoryContext.class)).andReturn(memory);
                replay(cpu, memory);
            }
            replay(pool, api);
            device = new DeviceImpl(0, api, settings);
            device.initialize();
            if (mits) { liveMits = (MhdskController) physical.getValue(); }
            else { liveSimh = (HdskController) synthetic.getValue(); }
            reopenLiveDialog(mits);
        } catch (Exception e) { throw new AssertionError(e); }
    }

    private void reopenLiveDialog(boolean mits) {
        dialog = new SettingsDialog(null, settings, mits, liveSimh, liveMits, dialogs, gui, () -> {
            device.applySettings();
            saves.incrementAndGet();
        });
    }

    @Test public void savePreservesEditsAcrossAllSixteenDrives() throws Exception {
        Path path = temporary.newFile("simh.dsk").toPath();
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(false);
            image().setText(" " + path + " ");
            sectorSize().setText("0x100");
            button("P").doClick();
            sectors().setText("64");
            button("A").doClick();
            assertEquals("0x100", sectorSize().getText());
            dialog.getRootPane().getDefaultButton().doClick();
            assertFalse(dialog.isDisplayable());
        });
        assertEquals(path.toString(), values.get("image0"));
        assertEquals(256, values.get("sectorSize0"));
        assertEquals(64, values.get("sectorsPerTrack15"));
        assertEquals("SIMH", values.get("controllerType"));
        assertEquals(1, saves.get());
        verify(dialogs);
    }

    @Test public void mitsHasEightPlattersWithFixedGeometryAndWriteProtection() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("mits.dsk");
        MhdskController.createImage(path);
        values.put("controllerType", "MITS");
        values.put("sectorSize0", 512);
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(true);
            assertEquals(8, components(dialog, JToggleButton.class).stream().filter(Component::isVisible)
                    .filter(b -> !(b instanceof JCheckBox)).count());
            assertEquals("256", sectorSize().getText());
            assertEquals("24", sectors().getText());
            assertFalse(sectorSize().isEnabled());
            assertFalse(button("Set default").isEnabled());
            image().setText(path.toString());
            button("Read-only").setSelected(true);
            button("3F").doClick();
            image().setText(path.toString());
            button("Save").doClick();
        });
        assertEquals(path.toString(), values.get("image0"));
        assertEquals(path.toString(), values.get("image7"));
        assertEquals(true, values.get("readOnly0"));
        assertEquals(512, values.get("sectorSize0")); // SIMH geometry survives MITS edits.
        assertFalse(values.containsKey("readOnly8"));
        assertEquals(1, saves.get());
        verify(dialogs);
    }

    @Test public void escapeDiscardsUnmountAndDoesNotTouchLiveMedia() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("live.dsk");
        MhdskController.createImage(path);
        values.put("controllerType", "MITS");
        values.put("readOnly1", true);
        replay(dialogs);
        try (MhdskController controller = new MhdskController(new PioBoard(0xA0, 2))) {
            controller.attach(0, path, true);
            SwingUtilities.invokeAndWait(() -> {
                dialog = new SettingsDialog(null, settings, true, null, controller, dialogs, gui, saves::incrementAndGet);
                assertEquals(path.toString(), image().getText());
                assertTrue(button("Read-only").isSelected());
                button("0F").doClick();
                assertTrue(button("Read-only").isSelected());
                button("0R").doClick();
                assertFalse(components(dialog, AbstractButton.class).stream().anyMatch(b -> "Cancel".equals(b.getText())));
                button("Unmount all").doClick();
                JRootPane root = dialog.getRootPane();
                Object escape = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                        .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
                root.getActionMap().get(escape).actionPerformed(new ActionEvent(root, 0, "escape"));
                assertFalse(dialog.isDisplayable());
            });
            assertEquals(path, controller.getImage(0));
            assertTrue(controller.isReadOnly(0));
        }
        assertEquals(Map.of("controllerType", "MITS", "readOnly1", true), values);
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    @Test public void invalidUnselectedDriveIsShownAndNothingIsPersisted() throws Exception {
        dialogs.showError(contains("Sector size must be a power of two"), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(false);
            button("P").doClick();
            sectorSize().setText("384");
            button("A").doClick();
            sectors().setText("64");
            button("Save").doClick();
            assertEquals("384", sectorSize().getText());
            assertTrue(button("P").isSelected());
            assertTrue(dialog.isDisplayable());
        });
        assertTrue(values.isEmpty());
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    @Test public void truncatedMitsImageIsRejectedBeforeSaving() throws Exception {
        Path path = temporary.newFile("short.dsk").toPath();
        values.put("controllerType", "MITS");
        dialogs.showError(contains("MHDSK platter must contain exactly"), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(true);
            image().setText(path.toString());
            button("Save").doClick();
            assertTrue(dialog.isDisplayable());
        });
        assertEquals(Map.of("controllerType", "MITS"), values);
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    @Test public void browseUpdatesImageAndCancelKeepsDraft() throws Exception {
        Path path = temporary.newFile("selected.dsk").toPath();
        expect(dialogs.chooseFile(eq("Open disk image"), eq("Open"), anyObject(Path.class), eq(false),
                anyObject(FileExtensionsFilter[].class))).andReturn(Optional.of(path));
        expect(dialogs.chooseFile(eq("Open disk image"), eq("Open"), eq(path.getParent()), eq(false),
                anyObject(FileExtensionsFilter[].class))).andReturn(Optional.empty());
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(false);
            button("Browse...").doClick();
            button("Browse...").doClick();
            assertEquals(path.toString(), image().getText());
        });
        assertTrue(values.isEmpty());
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    @Test public void switchingControllerPreservesGeometryAndExplainsReopening() throws Exception {
        dialogs.showInfo(contains("Reopen the computer"), eq("88-HDSK Settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(false);
            sectorSize().setText("512");
            mode().setSelectedItem("MITS");
            assertFalse(sectorSize().isEnabled());
            mode().setSelectedItem("SIMH");
            assertEquals("512", sectorSize().getText());
            mode().setSelectedItem("MITS");
            button("Save").doClick();
        });
        assertEquals("MITS", values.get("controllerType"));
        assertEquals(1, saves.get());
        verify(dialogs);
    }

    @Test public void persistenceFailureKeepsDialogOpenWithoutApplying() throws Exception {
        persistenceFailure = new CannotUpdateSettingException("Read-only settings");
        dialogs.showError(contains("Read-only settings"), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createDialog(false);
            button("Save").doClick();
            assertTrue(dialog.isDisplayable());
        });
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    @Test public void settingsMountAndUnmountTheSelectedSimhDriveOnSave() throws Exception {
        Path path = temporary.newFile("simh-mounted.dsk").toPath();
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createLiveDialog(false);
            button("P").doClick();
            image().setText(path.toString());
            sectorSize().setText("256");
            assertTrue(liveSimh.imagePath(15).isEmpty());
            button("Save").doClick();
            assertEquals(path, liveSimh.imagePath(15).orElseThrow());
            assertEquals(256, liveSimh.sectorSize(15));
            assertTrue(liveSimh.imagePath(0).isEmpty());
            reopenLiveDialog(false);
            button("P").doClick();
            button("Unmount").doClick();
            assertEquals(path, liveSimh.imagePath(15).orElseThrow());
            button("Save").doClick();
            assertTrue(liveSimh.imagePath(15).isEmpty());
        });
        assertFalse(values.containsKey("image15"));
        assertEquals(2, saves.get());
        verify(dialogs);
    }

    @Test public void settingsCreateMitsImageAndMountItOnSaveWithWriteProtection() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("created-mits.dsk");
        expect(dialogs.chooseFile(eq("Create disk image"), eq("Create"), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter[].class))).andReturn(Optional.of(path));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createLiveDialog(true);
            button("0F").doClick();
            button("Read-only").setSelected(true);
            button("Create image").doClick();
            assertNull(liveMits.getImage(1));
            button("Save").doClick();
            assertEquals(path, liveMits.getImage(1));
            assertTrue(liveMits.isReadOnly(1));
            assertNull(liveMits.getImage(0));
            reopenLiveDialog(true);
            button("Unmount all").doClick();
            assertEquals(path, liveMits.getImage(1));
            button("Save").doClick();
            assertNull(liveMits.getImage(1));
        });
        assertEquals(Platter.CAPACITY, Files.size(path));
        assertEquals(2, saves.get());
        verify(dialogs);
    }

    @Test public void settingsCreateSimhImageWithoutMountingBeforeSave() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("created-simh.dsk");
        expect(dialogs.chooseFile(eq("Create disk image"), eq("Create"), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter[].class))).andReturn(Optional.of(path));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createLiveDialog(false);
            button("Create image").doClick();
            assertEquals(path.toString(), image().getText());
            assertTrue(liveSimh.imagePath(0).isEmpty());
            button("Save").doClick();
            assertEquals(path, liveSimh.imagePath(0).orElseThrow());
        });
        assertTrue(Files.isRegularFile(path));
        assertEquals(1, saves.get());
        verify(dialogs);
    }

    @Test public void settingsCreateNeverOverwritesExistingMedia() throws Exception {
        Path path = temporary.newFile("existing.dsk").toPath();
        byte[] bytes = {5, 6, 7};
        Files.write(path, bytes);
        expect(dialogs.chooseFile(eq("Create disk image"), eq("Create"), anyObject(Path.class), eq(true),
                anyObject(FileExtensionsFilter[].class))).andReturn(Optional.of(path));
        dialogs.showError(contains("Could not create image"), eq("Create disk image"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createLiveDialog(false);
            button("Create image").doClick();
            assertTrue(image().getText().isEmpty());
            assertTrue(liveSimh.imagePath(0).isEmpty());
        });
        assertArrayEquals(bytes, Files.readAllBytes(path));
        assertEquals(0, saves.get());
        verify(dialogs);
    }

    private JTextField image() { return components(dialog, JTextField.class).get(0); }
    private JTextField sectors() { return components(dialog, JTextField.class).get(1); }
    private JTextField sectorSize() { return components(dialog, JTextField.class).get(2); }
    private JComboBox<?> mode() { return components(dialog, JComboBox.class).get(0); }
    private AbstractButton button(String text) {
        return components(dialog, AbstractButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }

    private static <T> List<T> components(Container container, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component component : container.getComponents()) {
            if (type.isInstance(component)) { result.add(type.cast(component)); }
            if (component instanceof Container) { result.addAll(components((Container) component, type)); }
        }
        return result;
    }
}
