/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.swing.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class MdsGuiTest {
    private final MdsController controller = new MdsController();
    private final Dialogs dialogs = createMock(Dialogs.class);
    private GUI gui;
    private MdsGui window;
    private Path image;

    @Before
    public void setUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        image = Files.createTempFile("88-mds", ".dsk");
        Files.write(image, new byte[(int) MdsDrive.IMAGE_SIZE]);
        gui = createNiceMock(GUI.class);
        expect(gui.panel(anyString(), anyString(), anyString())).andAnswer(JPanel::new).anyTimes();
        expect(gui.button(anyString(), anyObject(Runnable.class))).andAnswer(() -> {
            JButton button = new JButton((String) getCurrentArgument(0));
            Runnable action = getCurrentArgument(1);
            button.addActionListener(event -> action.run());
            return button;
        }).anyTimes();
        replay(gui);
    }

    @After
    public void tearDown() throws Exception {
        if (window != null) {
            SwingUtilities.invokeAndWait(window::dispose);
        }
        controller.close();
        if (image != null) {
            Files.deleteIfExists(image);
        }
    }

    @Test
    public void attachAndDetachSelectedDrive() throws Exception {
        expect(dialogs.chooseFile(anyString(), eq("Attach"), eq(false))).andReturn(Optional.of(image));
        replay(dialogs);

        SwingUtilities.invokeAndWait(() -> {
            window = new MdsGui(null, controller, dialogs, gui);
            assertTrue(hasLabel(window, "No image attached"));

            button(window, "Attach image").doClick();
            assertEquals(Optional.of(image), controller.imagePath(0));
            assertTrue(hasLabel(window, image.toString()));

            button(window, "Detach").doClick();
            assertEquals(Optional.empty(), controller.imagePath(0));
            assertTrue(hasLabel(window, "No image attached"));
        });
        verify(dialogs);
    }

    private static JButton button(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JButton && text.equals(((JButton) component).getText())) {
                return (JButton) component;
            }
            if (component instanceof Container) {
                JButton found = button((Container) component, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean hasLabel(Container container, String text) {
        for (Component component : container.getComponents()) {
            if (component instanceof JLabel && text.equals(((JLabel) component).getText())) {
                return true;
            }
            if (component instanceof Container && hasLabel((Container) component, text)) {
                return true;
            }
        }
        return false;
    }
}
