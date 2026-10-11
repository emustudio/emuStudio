/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import org.junit.After;
import org.junit.Test;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import java.lang.reflect.Field;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_OPEN_FILE;
import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_RUN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class IconsTest {
    private final String originalLookAndFeel = UIManager.getLookAndFeel().getClass().getName();

    @After
    public void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(originalLookAndFeel);
        UIManager.put(Icons.customKey(ICON_RUN), null);
    }

    @Test
    public void bundledIconsKeepTheirSizeAndArtworkAcrossThemes() throws Exception {
        for (String theme : new String[]{UIManager.getCrossPlatformLookAndFeelClassName(),
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"}) {
            UIManager.setLookAndFeel(theme);
            for (Field field : EmuStudioGui.class.getDeclaredFields()) {
                if (field.getName().startsWith("ICON_") && field.getType() == String.class) {
                    Icon icon = Icons.loadIcon((String) field.get(null));
                    assertNotNull(field.getName(), icon);
                    assertEquals(field.getName(), Icons.SIZE, icon.getIconWidth());
                    assertEquals(field.getName(), Icons.SIZE, icon.getIconHeight());
                }
            }
            assertNotSame(UIManager.getIcon("Tree.openIcon"), Icons.loadIcon(ICON_OPEN_FILE));
            assertNotSame(UIManager.getIcon("InternalFrame.closeIcon"),
                    Icons.loadIcon(EmuStudioGui.ICON_REMOVE));
        }
    }

    @Test
    public void customIconCanBeOverriddenThroughUiManager() {
        Icon override = new ImageIcon(new byte[]{0});
        UIManager.put(Icons.customKey(ICON_RUN), override);
        assertSame(override, Icons.loadIcon(ICON_RUN));
        assertNotNull(Icons.loadIcon(ICON_OPEN_FILE));
    }
}
