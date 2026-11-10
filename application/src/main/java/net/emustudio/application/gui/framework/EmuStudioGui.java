/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.ScaledImageIcon;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.gui.components.ToolbarButton;
import net.emustudio.application.gui.components.ToolbarPanel;
import net.emustudio.application.gui.components.ToolbarToggleButton;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.application.ui.GUI;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;
import static net.emustudio.emulib.ui.Constants.*;
import static net.emustudio.emulib.ui.StandardIcons.LOGO_FILE;

/**
 * Concrete implementation of the GUI interface using MigLayout for panel layouts.
 */
public class EmuStudioGui implements GUI {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmuStudioGui.class);
    private static final int ICON_SIZE = 22;
    private static final String CUSTOM_PREFIX = "emuStudio.icon.";
    private static final String TOOLBAR_ICON = "emuStudio.toolbarIcon";
    private static final String TOOLBAR_ICON_CALLER = "emuStudio.toolbarIconCaller";
    public static final Font FONT_COMMON = new Font(Font.DIALOG, Font.PLAIN, 12);
    public static final Font FONT_TITLE_BORDER = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private AppSettings.Theme currentTheme = AppSettings.Theme.INTELLIJ;

    public static Icon loadIcon(String resource) {
        return loadIcon(resource, EmuStudioGui.class);
    }

    public static Icon loadIcon(String resource, Class<?> callerClass) {
        String customKey = customKey(resource);
        Icon customIcon = UIManager.getIcon(customKey);
        if (customIcon != null) {
            return customIcon;
        }

        Icon fallback = GUI.loadIcon(resource, callerClass);
        if (fallback != null) {
            UIManager.put(customKey, fallback);
        }
        return fallback;
    }

    static String customKey(String resource) {
        return CUSTOM_PREFIX + resource;
    }

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

    public JButton toolbarButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JButton button = new ToolbarButton(action, iconResource, tooltipText, callerClass);
        toolbarIcon(button, iconResource, callerClass);
        return button;
    }

    @Override
    public JButton toolbarButton(Action action) {
        return new ToolbarButton(action);
    }

    public JButton toolbarButton(Action action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JButton button = new ToolbarButton(action, iconResource, tooltipText, callerClass);
        toolbarIcon(button, iconResource, callerClass);
        return button;
    }

    public JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction,
                                             String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JToggleButton button = new ToolbarToggleButton(action, itemAction, iconResource, tooltipText, callerClass);
        toolbarIcon(button, iconResource, callerClass);
        return button;
    }

    public JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, String iconResource, String tooltipText) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JToggleButton button = new ToolbarToggleButton(action, iconResource, tooltipText, callerClass);
        toolbarIcon(button, iconResource, callerClass);
        return button;
    }

    public JButton toolbarTextButton(Consumer<ActionEvent> action, String iconResource, String text) {
        Class<?> callerClass = StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass();
        JButton button = new ToolbarButton(action, iconResource, text, callerClass);
        toolbarIcon(button, iconResource, callerClass);
        button.setText(text);
        button.setHorizontalAlignment(SwingConstants.LEADING);
        button.getAccessibleContext().setAccessibleName(text);
        return button;
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

    public JLabel labelTitle(String text) {
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2));
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
    public JTextField textField(String text, int columns) {
        return new JTextField(text, columns);
    }

    @Override
    public JTextArea textAreaReadOnly(int rows, int columns) {
        JTextArea textArea = new JTextArea(rows, columns);
        textArea.setEditable(false);
        return textArea;
    }

    @Override
    public JToolBar toolBar() {
        JToolBar toolbar = new JToolBar();
        toolbar.putClientProperty("FlatLaf.style", "focusableButtons: true");
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBorderPainted(false);
        return toolbar;
    }

    @Override
    public JSplitPane splitPane(int orientation, Component first, Component second, double resizeWeight) {
        JSplitPane splitPane = new JSplitPane(orientation, true, first, second);
        splitPane.setBorder(null);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(resizeWeight);
        return splitPane;
    }

    @Override
    public JScrollPane scrollPane(Component view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    @Override
    public JPanel panel(String layoutConstraints, String colConstraints, String rowConstraints) {
        return new JPanel(new MigLayout(layoutConstraints, colConstraints, rowConstraints));
    }

    @Override
    public void styleTable(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        int contentHeight = Math.max(ICON_SIZE, table.getFontMetrics(table.getFont()).getHeight());
        table.setRowHeight(Math.max(table.getRowHeight(), contentHeight + 4));
    }

    @Override
    public JPanel section(String title, String layoutConstraints, String colConstraints, String rowConstraints) {
        JPanel panel = new JPanel(new MigLayout(layoutConstraints, colConstraints, rowConstraints));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    public JLabel label(String text, JComponent field) {
        JLabel label = label(text);
        label.setLabelFor(field);
        field.getAccessibleContext().setAccessibleName(text);
        return label;
    }

    /**
     * Adds a full-width toolbar row to a MigLayout panel.
     */
    public JToolBar toolbarRow(JPanel panel, Component... components) {
        JToolBar group = toolBar();
        group.setLayout(new MigLayout("insets 0, hidemode 3, gapx 2", "", ""));
        for (Component component : components) group.add(component);
        panel.add(group, "span, growx, wrap");
        return group;
    }

    public JScrollPane toolbarScrollPane(JComponent tools) {
        JScrollPane scroll = scrollPane(tools);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.setMinimumSize(new Dimension(0, 0));
        return scroll;
    }

    public static void toolbarIcon(AbstractButton button, String resource) {
        toolbarIcon(button, resource, StackWalker.getInstance(RETAIN_CLASS_REFERENCE).getCallerClass());
    }

    private static void toolbarIcon(AbstractButton button, String resource, Class<?> callerClass) {
        if (resource == null) return;
        if (button.getClientProperty(TOOLBAR_ICON) == null)
            button.addPropertyChangeListener("UI", event -> updateToolbarIcon(button));
        button.putClientProperty(TOOLBAR_ICON, resource);
        button.putClientProperty(TOOLBAR_ICON_CALLER, callerClass);
        updateToolbarIcon(button);
    }

    private static void updateToolbarIcon(AbstractButton button) {
        Icon icon = loadIcon((String) button.getClientProperty(TOOLBAR_ICON),
                (Class<?>) button.getClientProperty(TOOLBAR_ICON_CALLER));
        Icon disabled = UIManager.getLookAndFeel().getDisabledIcon(button, icon);
        button.setIcon(scaledIcon(icon));
        button.setDisabledIcon(scaledIcon(disabled));
    }

    private static Icon scaledIcon(Icon icon) {
        return icon instanceof ImageIcon ? new ScaledImageIcon((ImageIcon) icon) : icon;
    }

    public JTextField searchField(String prompt, int columns, String iconResource) {
        JTextField field = textField("", columns);
        field.setToolTipText(prompt);
        field.getAccessibleContext().setAccessibleName(prompt);
        field.putClientProperty("JTextField.placeholderText", prompt);
        field.putClientProperty("FlatLaf.style", "placeholderForeground: $TextField.foreground");
        Runnable updateIcon = () -> field.putClientProperty("JTextField.leadingIcon", scaledIcon(loadIcon(iconResource)));
        field.addPropertyChangeListener("UI", event -> updateIcon.run());
        updateIcon.run();
        return field;
    }

    public static Font monospaced(Component component) {
        return new Font(Font.MONOSPACED, Font.PLAIN, component.getFont().getSize());
    }

    public static Font monospaced(Font font) {
        return new Font(Font.MONOSPACED, font.getStyle(), font.getSize()).deriveFont(font.getSize2D());
    }

    public static void onTextChange(Runnable action, JTextComponent... fields) {
        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
        for (JTextComponent field : fields) field.getDocument().addDocumentListener(listener);
    }

    public static void bindKey(JComponent component, KeyStroke key, String name, Runnable action) {
        component.getInputMap().put(key, name);
        component.getActionMap().put(name, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                action.run();
            }
        });
    }

    public JPanel toolbarPanel() {
        return new ToolbarPanel();
    }

    public static <T> int rowOf(ListModel<T> model, Predicate<T> matches) {
        for (int row = 0; row < model.getSize(); row++)
            if (matches.test(model.getElementAt(row))) return row;
        return -1;
    }

    public static <T> void updateRows(DefaultListModel<T> model, List<T> values) {
        for (int i = 0; i < values.size(); i++) {
            if (i >= model.size()) model.addElement(values.get(i));
            else if (!Objects.equals(model.get(i), values.get(i))) model.set(i, values.get(i));
        }
        while (model.size() > values.size()) model.remove(model.size() - 1);
    }


}
