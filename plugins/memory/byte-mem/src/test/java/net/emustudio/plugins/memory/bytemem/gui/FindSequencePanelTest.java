/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class FindSequencePanelTest {
    @Test
    public void currentPageIsReadForEachSearchAndEnterSubmits() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger currentPage = new AtomicInteger(16);
            AtomicInteger foundFrom = new AtomicInteger(-1);
            AtomicReference<byte[]> found = new AtomicReference<>();
            FindSequencePanel panel = panel(currentPage, foundFrom, found);
            assertFalse(panel.isVisible());
            panel.showSearch();
            JTextField sequence = component(panel, JTextField.class, "sequence");
            sequence.setText("Hi");

            sequence.postActionEvent();

            assertArrayEquals(new byte[]{'H', 'i'}, found.get());
            assertEquals(16, foundFrom.get());
            assertTrue(panel.isVisible());

            currentPage.set(48);
            component(panel, JButton.class, "findButton").doClick();

            assertEquals(48, foundFrom.get());
            assertTrue(panel.isVisible());
        });
    }

    @Test
    public void specificPositionAndNumericFormatRemainAfterCloseAndReopen() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger foundFrom = new AtomicInteger(-1);
            AtomicReference<byte[]> found = new AtomicReference<>();
            FindSequencePanel panel = panel(new AtomicInteger(4), foundFrom, found);
            panel.showSearch();
            component(panel, JTextField.class, "sequence").setText("258");
            component(panel, JComboBox.class, "findFormat").setSelectedIndex(2);
            component(panel, JComboBox.class, "numberFormat").setSelectedIndex(0);
            component(panel, JComboBox.class, "byteOrder").setSelectedIndex(1);
            component(panel, JComboBox.class, "startPosition").setSelectedIndex(1);
            component(panel, JTextField.class, "position").setText("0x20");

            component(panel, JButton.class, "closeFind").doClick();
            assertFalse(panel.isVisible());
            panel.showSearch();
            component(panel, JButton.class, "findButton").doClick();

            assertEquals(32, foundFrom.get());
            assertArrayEquals(new byte[]{2, 1}, found.get());
            assertTrue(panel.isVisible());
        });
    }

    @Test
    public void emptySequenceShowsErrorWithoutStartingSearch() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Dialogs dialogs = createMock(Dialogs.class);
            dialogs.showError(eq("Sequence cannot be empty"), eq("Find sequence"));
            expectLastCall().once();
            replay(dialogs);
            AtomicInteger foundFrom = new AtomicInteger(-1);
            FindSequencePanel panel = new FindSequencePanel(dialogs, gui(), () -> 0,
                    (bytes, address) -> foundFrom.set(address), () -> {});
            panel.showSearch();

            component(panel, JButton.class, "findButton").doClick();

            assertEquals(-1, foundFrom.get());
            assertTrue(panel.isVisible());
            verify(dialogs);
        });
    }

    private FindSequencePanel panel(AtomicInteger currentPage, AtomicInteger foundFrom,
                                    AtomicReference<byte[]> found) {
        Dialogs dialogs = createNiceMock(Dialogs.class);
        replay(dialogs);
        AtomicReference<FindSequencePanel> reference = new AtomicReference<>();
        FindSequencePanel panel = new FindSequencePanel(dialogs, gui(), currentPage::get,
                (bytes, address) -> { found.set(bytes); foundFrom.set(address); },
                () -> reference.get().setVisible(false));
        reference.set(panel);
        return panel;
    }

    private GUI gui() {
        return (GUI) Proxy.newProxyInstance(GUI.class.getClassLoader(), new Class<?>[]{GUI.class},
                (proxy, method, arguments) -> {
                    if (method.getReturnType() == JPanel.class) return new JPanel();
                    if (method.getReturnType() == JLabel.class) return new JLabel((String) arguments[0]);
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private <T extends Component> T component(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container) {
                T nested = find((Container) child, type, name);
                if (nested != null) return nested;
            }
        }
        throw new AssertionError("Component not found: " + name);
    }

    private <T extends Component> T find(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && name.equals(child.getName())) return type.cast(child);
            if (child instanceof Container) {
                T nested = find((Container) child, type, name);
                if (nested != null) return nested;
            }
        }
        return null;
    }
}
