/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.gui;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.plugins.device.mits88dcdd.gui.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import java.net.URL;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.*;

public class GuiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiUtils.class);

    public static void setupLookAndFeel(AppSettings config) {
        String lookAndFeel = config.getLookAndFeel().orElse("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");

        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception ignored) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                LOGGER.warn("Unable to set system look and feel", e);
            }
        }

        UIManager.put("Button.background", UIManager.get("Panel.background"));
        UIManager.put("Button.font", FONT_COMMON);
        UIManager.put("Button.opaque", true);

        UIManager.put("CheckBox.font", FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.font", FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", FONT_COMMON);

        UIManager.put("ColorChooser.font", FONT_COMMON);
        UIManager.put("ComboBox.font", FONT_COMMON);

        UIManager.put("TabbedPane.selected", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.background", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentAreaColor", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentOpaque", true);
        UIManager.put("TabbedPane.opaque", true);
        UIManager.put("TabbedPane.tabsOpaque", true);
        UIManager.put("TabbedPane.font", FONT_TITLE_BORDER);
        UIManager.put("TabbedPane.smallFont", FONT_COMMON);

        UIManager.put("EditorPane.font", FONT_MONOSPACED);
        UIManager.put("FormattedTextField.font", FONT_COMMON);
        UIManager.put("IconButton.font", FONT_COMMON);

        UIManager.put("InternalFrame.optionDialogTitleFont", FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.paletteTitleFont", FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.titleFont", FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.opaque", true);

        UIManager.put("Label.font", FONT_COMMON);
        UIManager.put("Label.opaque", true);

        UIManager.put("List.font", FONT_MONOSPACED);
        UIManager.put("List.rendererUseUIBorder", true);
        UIManager.put("List.focusCellHighlightBorder", null);

        UIManager.put("Menu.acceleratorFont", FONT_COMMON);
        UIManager.put("Menu.font", FONT_COMMON);

        UIManager.put("MenuBar.font", FONT_COMMON);

        UIManager.put("MenuItem.acceleratorFont", FONT_COMMON);
        UIManager.put("MenuItem.font", FONT_COMMON);

        UIManager.put("OptionPane.buttonFont", FONT_COMMON);
        UIManager.put("OptionPane.font", FONT_COMMON);
        UIManager.put("OptionPane.messageFont", FONT_COMMON);

        UIManager.put("Panel.font", FONT_COMMON);
        UIManager.put("Panel.opaque", true);

        UIManager.put("PasswordField.font", FONT_COMMON);
        UIManager.put("PopupMenu.font", FONT_COMMON);
        UIManager.put("ProgressBar.font", FONT_COMMON);
        UIManager.put("RadioButton.font", FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.font", FONT_COMMON);
        UIManager.put("ScrollPane.font", FONT_COMMON);
        UIManager.put("Slider.font", FONT_COMMON);
        UIManager.put("Spinner.font", FONT_COMMON);

        UIManager.put("Table.font", FONT_COMMON);
        UIManager.put("Table.focusCellHighlightBorder", null);

        UIManager.put("TableHeader.font", FONT_TITLE_BORDER);

        UIManager.put("TextArea.font", FONT_MONOSPACED);
        UIManager.put("TextField.font", FONT_MONOSPACED);
        UIManager.put("TextPane.font", FONT_MONOSPACED);
        UIManager.put("TitledBorder.font", FONT_TITLE_BORDER);
        UIManager.put("ToggleButton.font", FONT_COMMON);
        UIManager.put("ToolBar.font", FONT_COMMON);
        UIManager.put("ToolTip.font", FONT_COMMON);
        UIManager.put("Tree.font", FONT_COMMON);
        UIManager.put("Viewport.font", FONT_COMMON);
    }
}
