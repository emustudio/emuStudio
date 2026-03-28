/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.settings.AppSettings;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;

import static net.emustudio.emulib.runtime.ui.Constants.FONT_COMMON;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_MONOSPACED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmuStudioUITest {

    @Test
    public void applyRoundedCornersMarksComponentForRoundedPainting() {
        JPanel panel = new JPanel();

        EmuStudioUI.applyRoundedCorners(panel);

        assertEquals(Boolean.TRUE, panel.getClientProperty("JComponent.roundRect"));
    }

    @Test
    public void createLogoJLabelUsesExpectedStyling() {
        JLabel label = EmuStudioUI.createLogoJLabel();

        assertEquals(Color.WHITE, label.getBackground());
        assertTrue(label.getBorder() instanceof FadingBorder);
        assertFalse(label.isOpaque());
    }

    @Test
    public void initializeAppliesThemeAndCustomUiDefaults() {
        for (AppSettings.Theme theme : AppSettings.Theme.values()) {
            AppSettings settings = new AppSettings(Config.inMemory(), false, false);
            settings.setString(AppSettings.KEY_THEME, theme.name());

            EmuStudioUI.initialize(settings);

            assertEquals(theme, EmuStudioUI.getCurrentTheme());
            assertEquals(Boolean.TRUE, UIManager.get("Button.opaque"));
            assertEquals(FONT_COMMON, UIManager.get("Button.font"));
            assertEquals(FONT_MONOSPACED, UIManager.get("TextField.font"));
            assertEquals(UIManager.get("Panel.background"), UIManager.get("TabbedPane.selected"));
        }
    }

    @Test
    public void setThemeUpdatesCurrentThemeForVisibleWindows() {
        JFrame frame = new JFrame("theme");
        frame.add(new JButton("Button"));
        frame.pack();

        AppSettings settings = new AppSettings(Config.inMemory(), false, false);
        settings.setString(AppSettings.KEY_THEME, AppSettings.Theme.DARK.name());

        try {
            EmuStudioUI.setTheme(settings);
        } finally {
            frame.dispose();
        }

        assertEquals(AppSettings.Theme.DARK, EmuStudioUI.getCurrentTheme());
    }
}
