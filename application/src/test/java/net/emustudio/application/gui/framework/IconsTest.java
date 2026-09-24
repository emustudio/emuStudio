/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import org.junit.After;
import org.junit.Test;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;

import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_OPEN_FILE;
import static net.emustudio.application.gui.framework.EmuStudioGui.ICON_RUN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class IconsTest {
    private final String originalLookAndFeel = UIManager.getLookAndFeel().getClass().getName();

    @After
    public void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(originalLookAndFeel);
    }

    @Test
    public void openIconUsesMetalAndNimbusDefaults() throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        assertSame(UIManager.getIcon("Tree.openIcon"), Icons.loadIcon(ICON_OPEN_FILE));

        UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        assertSame(UIManager.getIcon("Tree.openIcon"), Icons.loadIcon(ICON_OPEN_FILE));
    }

    @Test
    public void customIconCanBeOverriddenThroughUiManager() {
        Icon override = new ImageIcon(new byte[]{0});
        UIManager.put(Icons.customKey(ICON_RUN), override);
        assertSame(override, Icons.loadIcon(ICON_RUN));
        assertNotNull(Icons.loadIcon(ICON_OPEN_FILE));
    }
}
