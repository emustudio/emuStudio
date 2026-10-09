/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88pio;

import net.emustudio.emulib.runtime.settings.CannotUpdateSettingException;
import net.emustudio.emulib.runtime.settings.PluginSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class PioGuiTest {
    private final Map<String, Object> values = new HashMap<>();
    private final PluginSettings settings = createMock(PluginSettings.class);
    private final Dialogs dialogs = createStrictMock(Dialogs.class);
    private GUI gui;
    private SettingsDialog configuration;
    private PioGui status;
    private RuntimeException persistenceFailure;

    @Before public void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        expect(settings.getString(anyString(), anyString())).andAnswer(() ->
                (String) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        expect(settings.getInt(anyString(), anyInt())).andAnswer(() ->
                (int) values.getOrDefault(getCurrentArgument(0), getCurrentArgument(1))).anyTimes();
        settings.setString(anyString(), anyString());
        expectLastCall().andAnswer(this::put).anyTimes();
        settings.setInt(anyString(), anyInt());
        expectLastCall().andAnswer(this::put).anyTimes();
        replay(settings);
        gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.button(anyString())).andAnswer(() -> new JButton((String) getCurrentArgument(0))).anyTimes();
        expect(gui.scrollPane(anyObject(Component.class))).andAnswer(() ->
                new JScrollPane((Component) getCurrentArgument(0))).anyTimes();
        replay(gui);
    }

    private Object put() {
        if (persistenceFailure != null) { throw persistenceFailure; }
        values.put(getCurrentArgument(0), getCurrentArgument(1));
        return null;
    }

    @After public void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (configuration != null) { configuration.dispose(); }
            if (status != null) { status.dispose(); }
        });
    }

    private void configure() { configuration = new SettingsDialog(null, settings, dialogs, gui); }
    private JComboBox<?> mode() { return components(configuration, JComboBox.class).get(0); }
    private JTextField base() {
        return components(configuration, JTextField.class).stream()
                .filter(field -> !(field instanceof JFormattedTextField)).findFirst().orElseThrow();
    }
    private JSpinner count() { return components(configuration, JSpinner.class).get(0); }
    private JSpinner vector() { return components(configuration, JSpinner.class).get(1); }

    @Test public void saveFourPioPersistsCompleteValidatedConfiguration() throws Exception {
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            base().setText("0xF0");
            count().setValue(4);
            vector().setValue(3);
            assertEquals(8, components(configuration, JList.class).get(0).getModel().getSize());
            assertTrue(components(configuration, JList.class).get(0).getModel().getElementAt(7).toString().contains("FEh control, FFh DDR/data"));
            configuration.getRootPane().getDefaultButton().doClick();
            assertFalse(configuration.isDisplayable());
        });
        assertEquals(Map.of("boardType", "88-4PIO", "basePort", 240, "piaCount", 4, "interruptVector", 3), values);
        verify(dialogs);
    }

    @Test public void saveOriginalBoardUsesTwoPortsAndDisablesPiaCount() throws Exception {
        values.put("boardType", "88-PIO");
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            assertFalse(count().isEnabled());
            base().setText("254");
            vector().setValue(0);
            assertEquals(2, components(configuration, JList.class).get(0).getModel().getSize());
            button(configuration, "Save").doClick();
        });
        assertEquals(Map.of("boardType", "88-PIO", "basePort", 254, "piaCount", 2, "interruptVector", 0), values);
        verify(dialogs);
    }

    @Test public void switchingBoardsKeepsSeparatePortDraftsAndModeDefaults() throws Exception {
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            base().setText("0xB0");
            mode().setSelectedItem("88-PIO");
            assertEquals("0x04", base().getText());
            base().setText("0x06");
            mode().setSelectedItem("88-4PIO");
            assertEquals("0xB0", base().getText());
            mode().setSelectedItem("88-PIO");
            assertEquals("0x06", base().getText());
            button(configuration, "Set default").doClick();
            assertEquals("0x04", base().getText());
        });
        assertTrue(values.isEmpty());
        verify(dialogs);
    }

    @Test public void invalidOriginalBaseIsRejectedBeforeAnyPersistence() throws Exception {
        rejectBase("88-PIO", "255", "even address");
    }

    @Test public void misalignedFourPioBaseIsRejectedBeforeAnyPersistence() throws Exception {
        rejectBase("88-4PIO", "0xA1", "aligned to 16 ports");
    }

    @Test public void invalidNumberIsRejectedBeforeAnyPersistence() throws Exception {
        rejectBase("88-4PIO", "garbage", "decimal or 0x hexadecimal");
    }

    private void rejectBase(String mode, String base, String message) throws Exception {
        dialogs.showError(contains(message), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            mode().setSelectedItem(mode);
            base().setText(base);
            button(configuration, "Save").doClick();
            assertTrue(configuration.isDisplayable());
        });
        assertTrue(values.isEmpty());
        verify(dialogs);
    }

    @Test public void invalidTypedVectorDoesNotPersistOtherChanges() throws Exception {
        dialogs.showError(anyString(), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            base().setText("0xB0");
            ((JSpinner.DefaultEditor) vector().getEditor()).getTextField().setText("8");
            button(configuration, "Save").doClick();
            assertTrue(configuration.isDisplayable());
        });
        assertTrue(values.isEmpty());
        verify(dialogs);
    }

    @Test public void escapeDiscardsDraftsWithoutACancelButton() throws Exception {
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            base().setText("0xB0");
            vector().setValue(2);
            assertFalse(components(configuration, AbstractButton.class).stream().anyMatch(b -> "Cancel".equals(b.getText())));
            escape(configuration);
            assertFalse(configuration.isDisplayable());
        });
        assertTrue(values.isEmpty());
        verify(dialogs);
    }

    @Test public void persistenceFailureKeepsSettingsOpen() throws Exception {
        persistenceFailure = new CannotUpdateSettingException("Read-only settings");
        dialogs.showError(contains("Read-only settings"), eq("Save settings"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            configure();
            button(configuration, "Save").doClick();
            assertTrue(configuration.isDisplayable());
        });
        assertTrue(values.isEmpty());
        verify(dialogs);
    }

    @Test public void originalStatusDoesNotAcknowledgeInputOrOutput() throws Exception {
        PioUnit pio = new PioUnit(4);
        pio.writeData((byte) 42);
        pio.write(5, (byte) 99);
        pio.setOutputReady(true);
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, pio, null, 4, gui, () -> "unknown");
            for (int i = 0; i < 3; i++) { status.refresh(); }
            assertTrue(labels(status).contains("R D"));
            assertTrue(labels(status).contains("0x2A"));
            assertTrue(labels(status).contains("0x63"));
        });
        assertEquals(3, pio.read(4));
        assertEquals(42, pio.getInputLatch());
        assertEquals(99, pio.getOutputLatch());
    }

    @Test public void attachedDeviceFieldShowsOnlyTheLiveIdentityInBothBoardModes() throws Exception {
        AtomicReference<String> name = new AtomicReference<>("MITS 88-HDSK");
        SwingUtilities.invokeAndWait(() -> {
            for (boolean original : new boolean[]{true, false}) {
                name.set("MITS 88-HDSK");
                status = new PioGui(null, original ? new PioUnit(4) : null,
                        original ? null : new PioBoard(0xA0, 2), original ? 4 : 0xA0, gui, name::get);
                JTextField field = components(status, JTextField.class).stream()
                        .filter(text -> !(text instanceof JFormattedTextField)).findFirst().orElseThrow();
                assertEquals("MITS 88-HDSK", field.getText());
                assertFalse(field.isEditable());
                name.set("unknown");
                status.refresh();
                assertEquals("unknown", field.getText());
                status.dispose();
            }
        });
    }

    @Test public void fourPioStatusSwitchesAllChannelsWithoutClearingIrqFlags() throws Exception {
        PioBoard board = new PioBoard(0xF0, 4);
        board.write(0xFF, (byte) 0xF0); // PIA4-B DDR.
        board.write(0xFE, (byte) 5); // Data selected; C1 falling-edge IRQ enabled.
        board.write(0xFF, (byte) 0xA0);
        board.setInputPins(7, 0x5A);
        board.setControlLine1(7, false);
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, null, board, 0xF0, gui, () -> "unknown");
            JComboBox<?> channels = components(status, JComboBox.class).get(0);
            assertEquals(8, channels.getItemCount());
            channels.setSelectedIndex(7);
            status.refresh();
            assertTrue(labels(status).contains("1 . D I"));
            assertTrue(labels(status).contains("0x85"));
            assertTrue(labels(status).contains("0x5A"));
            assertTrue(labels(status).contains("0xA0"));
            assertTrue(labels(status).contains("0xF0"));
            channels.setSelectedIndex(0);
            assertTrue(labels(status).contains(". . . ."));
        });
        assertEquals(0x85, board.getControl(7));
        assertFalse(board.getControlLine1(7));
    }

    @Test public void originalManualInputStrobesAndReadyReflectsLiveState() throws Exception {
        PioUnit pio = new PioUnit(4);
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, pio, null, 4, gui, () -> "unknown");
            JSpinner input = components(status, JSpinner.class).get(0);
            ((JSpinner.DefaultEditor) input.getEditor()).getTextField().setText("127");
            button(status, "Strobe input").doClick();
            button(status, "Output device ready").doClick();
            assertEquals(127, pio.getInputLatch());
            assertEquals(3, pio.read(4));
            pio.write(5, (byte) 99);
            status.refresh();
            assertFalse(button(status, "Output device ready").isSelected());
        });
    }

    @Test public void fourPioManualInputTargetsSelectedChannelAndRespectsC2Direction() throws Exception {
        PioBoard board = new PioBoard(0xA0, 2);
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, null, board, 0xA0, gui, () -> "unknown");
            components(status, JComboBox.class).get(0).setSelectedIndex(3);
            components(status, JSpinner.class).get(0).setValue(42);
            button(status, "Set input pins").doClick();
            button(status, "C1 high").doClick();
            assertEquals(42, board.getInputPins(3));
            assertEquals(255, board.getInputPins(0));
            assertFalse(board.getControlLine1(3));
            board.write(0xA6, (byte) 0x38); // Static-high C2 output.
            board.setControlLine2(3, false);
            status.refresh();
            assertFalse(button(status, "C2 high").isEnabled());
            assertTrue(button(status, "C2 high").isSelected());
        });
    }

    @Test public void attachedPeripheralOwnsManualInputsUntilDetached() throws Exception {
        PioBoard board = new PioBoard(0xA0, 2);
        PioContext.Peripheral peripheral = createNiceMock(PioContext.Peripheral.class);
        replay(peripheral);
        board.attachPeripheral(peripheral);
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, null, board, 0xA0, gui, () -> "unknown");
            assertFalse(button(status, "Set input pins").isEnabled());
            assertFalse(button(status, "C1 high").isEnabled());
            assertFalse(button(status, "C2 high").isEnabled());
            board.detachPeripheral(peripheral);
            status.refresh();
            assertTrue(button(status, "Set input pins").isEnabled());
        });
    }

    @Test public void statusPollingStopsOnEscapeAndRestartsOnReopen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            status = new PioGui(null, new PioUnit(4), null, 4, gui, () -> "unknown");
            Timer timer = timer();
            assertFalse(timer.isRunning());
            status.setVisible(true);
            assertTrue(timer.isRunning());
            escape(status);
            assertFalse(timer.isRunning());
            status.setVisible(true);
            assertTrue(timer.isRunning());
            status.setVisible(false);
            assertFalse(timer.isRunning());
        });
    }

    private Timer timer() {
        try {
            Field field = PioGui.class.getDeclaredField("timer");
            field.setAccessible(true);
            return (Timer) field.get(status);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static void escape(JDialog window) {
        JRootPane root = window.getRootPane();
        Object action = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
        root.getActionMap().get(action).actionPerformed(new ActionEvent(root, 0, "escape"));
    }

    private static List<String> labels(Container container) {
        List<String> result = new ArrayList<>();
        for (JLabel label : components(container, JLabel.class)) { result.add(label.getText()); }
        return result;
    }

    private static AbstractButton button(Container container, String text) {
        return components(container, AbstractButton.class).stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
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
