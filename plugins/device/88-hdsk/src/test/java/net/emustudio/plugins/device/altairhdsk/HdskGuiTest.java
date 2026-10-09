/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.plugins.device.mits88pio.PioBoard;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class HdskGuiTest {
    @Rule public final TemporaryFolder temporary = new TemporaryFolder();
    private final Dialogs dialogs = createStrictMock(Dialogs.class);
    private GUI gui;
    private HdskGui window;
    private HdskController simh;
    private MhdskController mits;

    @Before public void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless());
        gui = createNiceMock(GUI.class);
        expect(gui.section(anyString(), anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.label(anyString())).andAnswer(() -> new JLabel((String) getCurrentArgument(0))).anyTimes();
        expect(gui.button(anyString())).andAnswer(() -> new JButton((String) getCurrentArgument(0))).anyTimes();
        expect(gui.scrollPane(anyObject(Component.class))).andAnswer(() ->
                new JScrollPane((Component) getCurrentArgument(0))).anyTimes();
        replay(gui);
    }

    @After public void tearDown() throws Exception {
        if (window != null) { SwingUtilities.invokeAndWait(window::dispose); }
        if (simh != null) { simh.close(); }
        if (mits != null) { mits.close(); }
    }

    @SuppressWarnings("unchecked")
    private void createSimh() {
        simh = new HdskController(createNiceMock(MemoryContext.class));
        window = new HdskGui(null, simh, null, dialogs, gui);
    }

    private void createMits() {
        mits = new MhdskController(new PioBoard(0xA0, 2));
        window = new HdskGui(null, null, mits, dialogs, gui);
    }

    @Test public void simhDriveButtonsShowIndependentGeometryAndLiveMounts() throws Exception {
        Path path = temporary.newFile("simh.dsk").toPath();
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createSimh();
            assertFalse(buttons().stream().anyMatch(b -> List.of("Mount image", "Create image", "Unmount", "Mount read-only").contains(b.getText())));
            assertEquals(16, components(window, JToggleButton.class).size());
            try { simh.attach(15, path); } catch (Exception e) { throw new RuntimeException(e); }
            simh.configure(15, 512, 64);
            window.refresh();
            assertTrue(button("P").getToolTipText().contains("image mounted"));
            assertNotNull(button("P").getIcon());
            button("P").doClick();
            assertEquals(512, sectorSize().getSelectedItem());
            assertEquals(64, sectors().getValue());
            assertEquals(path.toString(), image().getText());
            assertFalse(image().isEditable());
            assertEquals(3, image().getRows());
            assertEquals(40, image().getColumns());
            button("A").doClick();
            assertEquals(128, sectorSize().getSelectedItem());
            assertEquals("none", image().getText());
        });
        verify(dialogs);
    }

    @Test public void mitsUsesEightPlatterButtonsAndReportsActualWriteProtection() throws Exception {
        Path path = temporary.getRoot().toPath().resolve("mits.dsk");
        MhdskController.createImage(path);
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createMits();
            assertFalse(buttons().stream().anyMatch(b -> List.of("Mount image", "Create image", "Unmount", "Mount read-only").contains(b.getText())));
            assertEquals(8, components(window, JToggleButton.class).size());
            try { mits.attach(7, path, true); } catch (Exception e) { throw new RuntimeException(e); }
            button("3F").doClick();
            assertTrue(labels().contains("Read-only"));
            assertTrue(labels().contains("Unit 3 fixed"));
            assertEquals(path.toString(), image().getText());
            assertTrue(components(window, JSpinner.class).isEmpty());
            assertFalse(buttons().stream().anyMatch(b -> "Apply geometry".equals(b.getText())));
        });
        verify(dialogs);
    }

    @Test public void refreshShowsExternalGeometryChangesButKeepsPendingEdits() throws Exception {
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createSimh();
            assertFalse(buttons().stream().anyMatch(b -> List.of("Mount image", "Create image", "Unmount", "Mount read-only").contains(b.getText())));
            sectorSize().setSelectedItem(512);
            sectors().setValue(64);
            window.refresh();
            assertEquals(512, sectorSize().getSelectedItem());
            assertEquals(64, sectors().getValue());
            simh.configure(0, 256, 16); // A settings-dialog save changes the running controller.
            window.refresh();
            assertEquals(256, sectorSize().getSelectedItem());
            assertEquals(16, sectors().getValue());
        });
        verify(dialogs);
    }

    @Test public void invalidTypedGeometryDoesNotChangeTheController() throws Exception {
        dialogs.showError(anyString(), eq("SIMH Altair HDSK"));
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createSimh();
            assertFalse(buttons().stream().anyMatch(b -> List.of("Mount image", "Create image", "Unmount", "Mount read-only").contains(b.getText())));
            ((JSpinner.DefaultEditor) sectors().getEditor()).getTextField().setText("0");
            button("Apply geometry").doClick();
            assertTrue(simh.imagePath(0).isEmpty());
            assertEquals(32, simh.sectorsPerTrack(0));
        });
        verify(dialogs);
    }

    @Test public void escapeStopsRefreshAndReopeningRestartsIt() throws Exception {
        replay(dialogs);
        SwingUtilities.invokeAndWait(() -> {
            createSimh();
            assertFalse(buttons().stream().anyMatch(b -> List.of("Mount image", "Create image", "Unmount", "Mount read-only").contains(b.getText())));
            Timer timer = timer();
            assertFalse(timer.isRunning());
            window.setVisible(true);
            assertTrue(timer.isRunning());
            JRootPane root = window.getRootPane();
            Object escape = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .get(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
            root.getActionMap().get(escape).actionPerformed(new ActionEvent(root, 0, "escape"));
            assertFalse(window.isDisplayable());
            assertFalse(timer.isRunning());
            window.setVisible(true);
            assertTrue(timer.isRunning());
            window.setVisible(false);
            assertFalse(timer.isRunning());
        });
        verify(dialogs);
    }

    private Timer timer() {
        try {
            Field field = HdskGui.class.getDeclaredField("refreshTimer");
            field.setAccessible(true);
            return (Timer) field.get(window);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private JComboBox<?> sectorSize() { return components(window, JComboBox.class).get(0); }
    private JSpinner sectors() { return components(window, JSpinner.class).get(0); }
    private JTextArea image() { return components(window, JTextArea.class).get(1); }
    private List<AbstractButton> buttons() { return components(window, AbstractButton.class); }
    private AbstractButton button(String text) {
        return buttons().stream().filter(b -> text.equals(b.getText())).findFirst().orElseThrow();
    }
    private List<String> labels() {
        List<String> labels = new ArrayList<>();
        components(window, JLabel.class).forEach(label -> labels.add(label.getText()));
        return labels;
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
