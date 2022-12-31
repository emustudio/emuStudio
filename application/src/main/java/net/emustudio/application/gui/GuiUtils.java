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

import net.emustudio.application.Constants;
import net.emustudio.application.settings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyListener;

public class GuiUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuiUtils.class);

    /**
     * This method adds this key listener to all sub-components of given
     * component.
     *
     * @param c           Component to add this key listener recursively
     * @param keyListener the key listener object
     */
    public static void addKeyListenerRecursively(Component c, KeyListener keyListener) {
        c.addKeyListener(keyListener);
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                addKeyListenerRecursively(child, keyListener);
            }
        }
    }

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
        UIManager.put("Button.font", Constants.FONT_COMMON);
        UIManager.put("Button.opaque", true);

        UIManager.put("CheckBox.font", Constants.FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.font", Constants.FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", Constants.FONT_COMMON);

        UIManager.put("ColorChooser.font", Constants.FONT_COMMON);
        UIManager.put("ComboBox.font", Constants.FONT_COMMON);

        UIManager.put("TabbedPane.selected", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.background", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentAreaColor", UIManager.get("Panel.background"));
        UIManager.put("TabbedPane.contentOpaque", true);
        UIManager.put("TabbedPane.opaque", true);
        UIManager.put("TabbedPane.tabsOpaque", true);
        UIManager.put("TabbedPane.font", Constants.FONT_TITLE_BORDER);
        UIManager.put("TabbedPane.smallFont", Constants.FONT_COMMON);

        UIManager.put("EditorPane.font", Constants.FONT_MONOSPACED);
        UIManager.put("FormattedTextField.font", Constants.FONT_COMMON);
        UIManager.put("IconButton.font", Constants.FONT_COMMON);

        UIManager.put("InternalFrame.optionDialogTitleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.paletteTitleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.titleFont", Constants.FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.opaque", true);

        UIManager.put("Label.font", Constants.FONT_COMMON);
        UIManager.put("Label.opaque", true);

        UIManager.put("List.font", Constants.FONT_MONOSPACED);
        UIManager.put("List.rendererUseUIBorder", true);
        UIManager.put("List.focusCellHighlightBorder", null);

        UIManager.put("Menu.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("Menu.font", Constants.FONT_COMMON);

        UIManager.put("MenuBar.font", Constants.FONT_COMMON);

        UIManager.put("MenuItem.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("MenuItem.font", Constants.FONT_COMMON);

        UIManager.put("OptionPane.buttonFont", Constants.FONT_COMMON);
        UIManager.put("OptionPane.font", Constants.FONT_COMMON);
        UIManager.put("OptionPane.messageFont", Constants.FONT_COMMON);

        UIManager.put("Panel.font", Constants.FONT_COMMON);
        UIManager.put("Panel.opaque", true);

        UIManager.put("PasswordField.font", Constants.FONT_COMMON);
        UIManager.put("PopupMenu.font", Constants.FONT_COMMON);
        UIManager.put("ProgressBar.font", Constants.FONT_COMMON);
        UIManager.put("RadioButton.font", Constants.FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.acceleratorFont", Constants.FONT_COMMON);
        UIManager.put("RadioButtonMenuItem.font", Constants.FONT_COMMON);
        UIManager.put("ScrollPane.font", Constants.FONT_COMMON);
        UIManager.put("Slider.font", Constants.FONT_COMMON);
        UIManager.put("Spinner.font", Constants.FONT_COMMON);

        UIManager.put("Table.font", Constants.FONT_COMMON);
        UIManager.put("Table.focusCellHighlightBorder", null);

        UIManager.put("TableHeader.font", Constants.FONT_TITLE_BORDER);

        UIManager.put("TextArea.font", Constants.FONT_MONOSPACED);
        UIManager.put("TextField.font", Constants.FONT_MONOSPACED);
        UIManager.put("TextPane.font", Constants.FONT_MONOSPACED);
        UIManager.put("TitledBorder.font", Constants.FONT_TITLE_BORDER);
        UIManager.put("ToggleButton.font", Constants.FONT_COMMON);
        UIManager.put("ToolBar.font", Constants.FONT_COMMON);
        UIManager.put("ToolTip.font", Constants.FONT_COMMON);
        UIManager.put("Tree.font", Constants.FONT_COMMON);
        UIManager.put("Viewport.font", Constants.FONT_COMMON);
    }
}
