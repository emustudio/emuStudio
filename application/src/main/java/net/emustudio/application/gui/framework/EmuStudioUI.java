/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;

import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.runtime.ui.components.FadingBorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.emustudio.emulib.runtime.ui.Constants.*;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

/**
 * Central UI configuration for emuStudio.
 * Provides theme management and common UI utilities.
 */
public class EmuStudioUI {
    // General icons
    public static final String LOGO_FILE = "/net/emustudio/application/gui/dialogs/logo.png";
    public static final String ICON_FAVICON = "/net/emustudio/application/gui/favicon16.png";
    
    // Schema element icons
    public static final String ICON_COMPILER = "/net/emustudio/application/gui/dialogs/compile.png";
    public static final String ICON_CPU = "/net/emustudio/application/gui/dialogs/cpu.gif";
    public static final String ICON_MEMORY = "/net/emustudio/application/gui/dialogs/ram.gif";
    public static final String ICON_DEVICE = "/net/emustudio/application/gui/dialogs/device.png";
    public static final String ICON_CONNECTION = "/net/emustudio/application/gui/dialogs/connection.png";
    public static final String ICON_BIDIRECTION = "/net/emustudio/application/gui/dialogs/bidirection.gif";
    public static final String ICON_GRID = "/net/emustudio/application/gui/dialogs/grid_memory.gif";
    
    // Editor icons
    public static final String ICON_FIND = "/net/emustudio/application/gui/dialogs/edit-find.png";
    public static final String ICON_REPLACE = "/net/emustudio/application/gui/dialogs/edit-find-replace.png";
    public static final String ICON_NEW_FILE = "/net/emustudio/application/gui/dialogs/document-new.png";
    public static final String ICON_OPEN_FILE = "/net/emustudio/application/gui/dialogs/document-open.png";
    public static final String ICON_SAVE = "/net/emustudio/application/gui/dialogs/document-save.png";
    public static final String ICON_UNDO = "/net/emustudio/application/gui/dialogs/edit-undo.png";
    public static final String ICON_REDO = "/net/emustudio/application/gui/dialogs/edit-redo.png";
    public static final String ICON_CUT = "/net/emustudio/application/gui/dialogs/edit-cut.png";
    public static final String ICON_COPY = "/net/emustudio/application/gui/dialogs/edit-copy.png";
    public static final String ICON_PASTE = "/net/emustudio/application/gui/dialogs/edit-paste.png";
    
    // Emulator icons
    public static final String ICON_RUN = "/net/emustudio/application/gui/dialogs/go-play.png";
    public static final String ICON_RUN_TIMED = "/net/emustudio/application/gui/dialogs/go-play-time.png";
    public static final String ICON_PAUSE = "/net/emustudio/application/gui/dialogs/go-pause.png";
    public static final String ICON_STOP = "/net/emustudio/application/gui/dialogs/go-stop.png";
    public static final String ICON_STEP = "/net/emustudio/application/gui/dialogs/go-next.png";
    public static final String ICON_STEP_BACK = "/net/emustudio/application/gui/dialogs/go-previous.png";
    public static final String ICON_JUMP = "/net/emustudio/application/gui/dialogs/go-jump.png";
    public static final String ICON_JUMP_TO_BEGINNING = "/net/emustudio/application/gui/dialogs/go-first.png";
    public static final String ICON_RESET = "/net/emustudio/application/gui/dialogs/reset.png";
    public static final String ICON_BREAKPOINT = "/net/emustudio/application/gui/dialogs/breakpoint.png";
    public static final String ICON_BREAKPOINTS = "/net/emustudio/application/gui/dialogs/breakpoints.png";
    
    // Computer management icons
    public static final String ICON_COMPUTER = "/net/emustudio/application/gui/dialogs/computer.png";
    public static final String ICON_ADD = "/net/emustudio/application/gui/dialogs/list-add.png";
    public static final String ICON_REMOVE = "/net/emustudio/application/gui/dialogs/list-remove.png";
    public static final String ICON_DELETE = "/net/emustudio/application/gui/dialogs/edit-delete.png";
    public static final String ICON_RENAME = "/net/emustudio/application/gui/dialogs/rename-computer.png";
    
    // Debug table pagination icons
    public static final String ICON_PAGE_FIRST = "/net/emustudio/application/gui/dialogs/page-first.png";
    public static final String ICON_PAGE_BACK = "/net/emustudio/application/gui/dialogs/page-back.png";
    public static final String ICON_PAGE_CURRENT = "/net/emustudio/application/gui/dialogs/page-current.png";
    public static final String ICON_PAGE_FORWARD = "/net/emustudio/application/gui/dialogs/page-forward.png";
    public static final String ICON_PAGE_SEEK_BACKWARD = "/net/emustudio/application/gui/dialogs/page-seek-backward.png";
    public static final String ICON_PAGE_SEEK_FORWARD = "/net/emustudio/application/gui/dialogs/page-seek-forward.png";
    
    // Dialog icons
    public static final String ICON_LOADING = "/net/emustudio/application/gui/dialogs/loading.gif";
    public static final String ICON_MOTHERBOARD = "/net/emustudio/application/gui/dialogs/motherboard-icon.gif";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmuStudioUI.class);

    private static AppSettings.Theme currentTheme = AppSettings.Theme.INTELLIJ;

    public static void initialize(AppSettings config) {
        AppSettings.Theme theme = config.getTheme().orElse(AppSettings.Theme.INTELLIJ);
        try {
            switch (theme) {
                case LIGHT:
                    FlatLightLaf.setup();
                    break;
                case DARK:
                    FlatDarkLaf.setup();
                    break;
                case INTELLIJ:
                default:
                    FlatIntelliJLaf.setup();
                    break;
            }
            currentTheme = theme;
            customize();
        } catch (Exception e) {
            LOGGER.error("Failed to initialize FlatLaf: " + e.getMessage(), e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.error("Failed to set look and feel after FlatLaf failure: " + e.getMessage(), e);
            }
        }
    }

    private static void customize() {
        // Additional UI tweaks
        UIManager.put("Button.arc", 8);
        UIManager.put("Component.arc", 8);
        UIManager.put("TextComponent.arc", 8);

        // Keep font settings for compatibility
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


    public static void setTheme(AppSettings config) {
        initialize(config);

        // Update all windows
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    public static AppSettings.Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Apply rounded corners to a component.
     */
    public static void applyRoundedCorners(JComponent component) {
        component.putClientProperty("JComponent.roundRect", true);
    }

    /**
     * Create a JLabel with the emuStudio logo, styled with a fading border.
     *
     * @return JLabel containing the logo
     */
    public static JLabel createLogoJLabel() {
        JLabel lblLogo = new JLabel(loadIcon(LOGO_FILE));
        lblLogo.setBackground(Color.WHITE);
        lblLogo.setBorder(new FadingBorder(10, Color.WHITE));
        lblLogo.setOpaque(false);
        return lblLogo;
    }
}
