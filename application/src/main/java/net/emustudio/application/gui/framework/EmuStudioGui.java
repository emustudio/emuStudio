/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import net.emustudio.application.gui.components.BrowseButton;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.gui.components.ToolbarButton;
import net.emustudio.application.gui.components.ToolbarToggleButton;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static net.emustudio.emulib.runtime.ui.Constants.*;
import static net.emustudio.emulib.runtime.ui.Constants.FONT_COMMON;
import static net.emustudio.emulib.runtime.ui.GUI.loadIcon;

/**
 * Concrete implementation of the GUI interface using MigLayout for panel layouts.
 */
public class EmuStudioGui implements GUI {
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

    private static final Logger LOGGER = LoggerFactory.getLogger(EmuStudioGui.class);

    private AppSettings.Theme currentTheme = AppSettings.Theme.INTELLIJ;

    public void initialize(AppSettings config) {
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
            LOGGER.error("Failed to initialize FlatLaf: {}", e.getMessage(), e);
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.error("Failed to set look and feel after FlatLaf failure: {}", e.getMessage(), e);
            }
        }
    }

    private void customize() {
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


    public void setTheme(AppSettings config) {
        initialize(config);

        // Update all windows
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }

    public AppSettings.Theme getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Create a JLabel with the emuStudio logo, styled with a fading border.
     *
     * @return JLabel containing the logo
     */
    public JLabel createLogoJLabel() {
        JLabel lblLogo = new JLabel(loadIcon(LOGO_FILE));
        lblLogo.setBackground(Color.WHITE);
        lblLogo.setBorder(new FadingBorder(10, Color.WHITE));
        lblLogo.setOpaque(false);
        return lblLogo;
    }

    @Override
    public JButton toolbarButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ToolbarButton(action, iconResource, tooltipText, callerClass);
    }

    @Override
    public JButton toolbarButton(Action action) {
        return new ToolbarButton(action);
    }

    @Override
    public JButton toolbarButton(Action action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ToolbarButton(action, iconResource, tooltipText, callerClass);
    }

    @Override
    public JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction,
                                             String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ToolbarToggleButton(action, itemAction, iconResource, tooltipText, callerClass);
    }

    @Override
    public JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        return new ToolbarToggleButton(action, iconResource, tooltipText, callerClass);
    }

    @Override
    public JLabel label(String text) {
        return new JLabel(text);
    }

    @Override
    public JLabel labelBold(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    @Override
    public JLabel labelTitle(String text) {
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2));
        return label;
    }

    @Override
    public JLabel labelPadded(String text, int top, int left, int bottom, int right) {
        JLabel label = new JLabel(text);
        label.setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));
        return label;
    }

    @Override
    public JButton buttonMakePrimary(JButton button) {
        button.setDefaultCapable(true);
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        return button;
    }

    @Override
    public JButton button(String text) {
        return new JButton(text);
    }

    @Override
    public JButton button(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    @Override
    public JButton button(String iconResource, String text, Runnable action) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JButton btn = new JButton(text, GUI.loadIcon(iconResource, callerClass));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    @Override
    public JButton button(String text, ActionListener action) {
        JButton btn = new JButton(text);
        btn.addActionListener(action);
        return btn;
    }

    @Override
    public JButton buttonBrowseDirectories(Dialogs dialogs, String dialogTitle, String approveButtonText,
                                           Consumer<Path> onApprove) {
        return new BrowseButton(dialogs, dialogTitle, approveButtonText, onApprove);
    }

    @Override
    public JButton buttonBrowseFiles(Dialogs dialogs, String dialogTitle, String approveButtonText,
                                     boolean appendMissingExtensions, Consumer<Path> onApprove,
                                     FileExtensionsFilter... filters) {
        return new BrowseButton(dialogs, dialogTitle, approveButtonText, appendMissingExtensions, onApprove, filters);
    }

    @Override
    public JMenuItem menuItem(Action action) {
        return new JMenuItem(action);
    }

    @Override
    public JTextField textField(String text) {
        return textField(text, 20);
    }

    @Override
    public JTextField textField(String text, int columns) {
        return new JTextField(text, columns);
    }

    @Override
    public JTextArea textAreaReadOnly(int columns, int rows) {
        JTextArea textArea = new JTextArea(columns, rows);
        textArea.setEditable(false);
        return textArea;
    }

    @Override
    public JToolBar toolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBorderPainted(false);
        return toolbar;
    }

    @Override
    public JToolBar toolBarVertical() {
        JToolBar toolbar = toolBar();
        toolbar.setOrientation(JToolBar.VERTICAL);
        return toolbar;
    }

    @Override
    public JSplitPane splitPane() {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setBorder(null);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
        return splitPane;
    }

    @Override
    public JSplitPane splitPaneLeftToRight(Component left, Component right, Double resizeWeight) {
        JSplitPane sp = splitPane();
        sp.setLeftComponent(left);
        sp.setRightComponent(right);
        sp.setResizeWeight(resizeWeight);
        sp.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
        return sp;
    }

    @Override
    public JSplitPane splitPaneTopToBottom(Component top, Component bottom, Double resizeWeight) {
        JSplitPane sp = splitPane();
        sp.setLeftComponent(top);
        sp.setRightComponent(bottom);
        sp.setResizeWeight(resizeWeight);
        sp.setOrientation(JSplitPane.VERTICAL_SPLIT);
        return sp;
    }

    @Override
    public JScrollPane scrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    @Override
    public JPanel panelVertical() {
        return new JPanel(new MigLayout("insets dialog, fillx", "[grow]", "[]"));
    }

    @Override
    public JPanel panel(String layoutConstraints, String colConstraints, String rowConstraints) {
        return new JPanel(new MigLayout(layoutConstraints, colConstraints, rowConstraints));
    }

    @Override
    public JPanel panelHorizontal() {
        return new JPanel(new MigLayout("insets dialog, filly", "[]", "[grow]"));
    }

    @Override
    public JPanel panelButtons() {
        return new JPanel(new MigLayout("insets dialog", "[grow, right]", "[]"));
    }

    @Override
    public void styleTable(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(table.getRowHeight() + 4);
    }

    @Override
    public void styleList(JList<?> list) {
        list.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    @Override
    public JPanel section(String title, String layoutConstraints, String colConstraints, String rowConstraints) {
        JPanel panel = new JPanel(new MigLayout(layoutConstraints, colConstraints, rowConstraints));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    @Override
    public Border fadingBorder(int thickness, Color color) {
        return new FadingBorder(thickness, color);
    }
}

