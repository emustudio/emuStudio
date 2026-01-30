/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.util.function.Consumer;

import net.emustudio.application.settings.AppSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.emustudio.emulib.runtime.interaction.GuiConstants.*;
import static net.emustudio.emulib.runtime.interaction.GuiUtils.loadIcon;

/**
 * Central UI configuration for emuStudio.
 * Provides theme management and common UI utilities.
 */
public class EmuStudioUI {
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
//        UIManager.put("Button.opaque", true);

        UIManager.put("CheckBox.font", FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.font", FONT_COMMON);
        UIManager.put("CheckBoxMenuItem.acceleratorFont", FONT_COMMON);

        UIManager.put("ColorChooser.font", FONT_COMMON);
        UIManager.put("ComboBox.font", FONT_COMMON);

//        UIManager.put("TabbedPane.selected", UIManager.get("Panel.background"));
//        UIManager.put("TabbedPane.background", UIManager.get("Panel.background"));
//        UIManager.put("TabbedPane.contentAreaColor", UIManager.get("Panel.background"));
//        UIManager.put("TabbedPane.contentOpaque", true);
//        UIManager.put("TabbedPane.opaque", true);
//        UIManager.put("TabbedPane.tabsOpaque", true);
//        UIManager.put("TabbedPane.font", FONT_TITLE_BORDER);
//        UIManager.put("TabbedPane.smallFont", FONT_COMMON);

        UIManager.put("EditorPane.font", FONT_MONOSPACED);
        UIManager.put("FormattedTextField.font", FONT_COMMON);
        UIManager.put("IconButton.font", FONT_COMMON);

        UIManager.put("InternalFrame.optionDialogTitleFont", FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.paletteTitleFont", FONT_TITLE_BORDER);
        UIManager.put("InternalFrame.titleFont", FONT_TITLE_BORDER);
        //   UIManager.put("InternalFrame.opaque", true);

        UIManager.put("Label.font", FONT_COMMON);
        // UIManager.put("Label.opaque", true);

        UIManager.put("List.font", FONT_MONOSPACED);
//        UIManager.put("List.rendererUseUIBorder", true);
        //  UIManager.put("List.focusCellHighlightBorder", null);

        UIManager.put("Menu.acceleratorFont", FONT_COMMON);
        UIManager.put("Menu.font", FONT_COMMON);

        UIManager.put("MenuBar.font", FONT_COMMON);

        UIManager.put("MenuItem.acceleratorFont", FONT_COMMON);
        UIManager.put("MenuItem.font", FONT_COMMON);

        UIManager.put("OptionPane.buttonFont", FONT_COMMON);
        UIManager.put("OptionPane.font", FONT_COMMON);
        UIManager.put("OptionPane.messageFont", FONT_COMMON);

        UIManager.put("Panel.font", FONT_COMMON);
        //UIManager.put("Panel.opaque", true);

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
//        UIManager.put("Table.focusCellHighlightBorder", null);

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

    // Common UI factory methods

    public static JButton button(String text) {
        return new JButton(text);
    }

    public static JButton button(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.addActionListener(e -> action.run());
        return btn;
    }

    /**
     * Toolbar toggle button - a JToggleButton ready to add to a toolbar.
     * Properties:
     * - button text is hidden
     * - tooltip is set from Action.getValue(SHORT_DESCRIPTION) by default
     * - is not focusable
     * - icon is set from icon resource path
     * - button action is external
     *
     * @param action       button action
     * @param itemAction   item state change action
     * @param iconResource icon resource path
     * @param tooltipText  tooltip text
     * @return the toolbar toggle button
     */
    public static JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, Consumer<ItemEvent> itemAction,
                                                    String iconResource, String tooltipText) {
        JToggleButton button = new JToggleButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        });
        button.setIcon(loadIcon(iconResource));
        button.setToolTipText(tooltipText);
        button.setFocusable(false);
        button.addItemListener(itemAction::accept);
        return button;
    }

    public static JToggleButton toolbarToggleButton(Consumer<ActionEvent> action, String iconResource,
                                                    String tooltipText) {
        return toolbarToggleButton(action, e -> {
        }, iconResource, tooltipText);
    }

    public static JLabel label(String text) {
        return new JLabel(text);
    }

    public static JLabel boldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    public static JLabel titleLabel(String text) {
        JLabel label = new JLabel(text);
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2));
        return label;
    }

    public static JTextField textField() {
        return new JTextField(20);
    }

    public static JTextField textField(String text) {
        return new JTextField(text, 20);
    }

    public static JTextArea textArea(int rows, int cols) {
        JTextArea area = new JTextArea(rows, cols);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public static JScrollPane scrollPane(Component view) {
        return new JScrollPane(view);
    }

    /**
     * Apply modern FlatLaf styling to a toolbar.
     */
    public static void styleToolbar(JToolBar toolbar) {
        toolbar.setFloatable(false);
        toolbar.setRollover(true);
        toolbar.setBorderPainted(false);
    }

    /**
     * Apply modern styling to a button to make it a primary action button.
     */
    public static void makePrimaryButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "borderless");
        button.setFont(button.getFont().deriveFont(Font.BOLD));
    }

    /**
     * Apply modern styling to a split pane.
     */
    public static void styleSplitPane(JSplitPane splitPane) {
        splitPane.setBorder(null);
        splitPane.setOneTouchExpandable(true);
        splitPane.setContinuousLayout(true);
    }

    /**
     * Apply modern styling to a scroll pane.
     */
    public static void styleScrollPane(JScrollPane scrollPane) {
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    /**
     * Apply modern styled border to a panel.
     */
    public static void stylePanel(JPanel panel, String title) {
        if (title != null && !title.isEmpty()) {
            panel.setBorder(BorderFactory.createTitledBorder(title));
        }
    }

    /**
     * Make a button flat/borderless for toolbar use.
     */
    public static void makeFlatButton(AbstractButton button) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
    }

    /**
     * Apply rounded corners to a component.
     */
    public static void applyRoundedCorners(JComponent component) {
        component.putClientProperty("JComponent.roundRect", true);
    }

    /**
     * Make a table modern looking.
     */
    public static void styleTable(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setRowHeight(table.getRowHeight() + 4);
    }

    /**
     * Style a list for modern appearance.
     */
    public static void styleList(JList<?> list) {
        list.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    public static Frame findOwnerFrame() {
        for (Frame f : Frame.getFrames()) {
            if (f != null && f.isDisplayable()) return f;
        }
        return null; // fallback
    }


    public static void enableWaylandXwaylandLayoutSync(JComponent root, JDialog dialog) {
        // Bežíme len na Waylande
        if (!"wayland".equalsIgnoreCase(System.getenv("XDG_SESSION_TYPE"))) {
            return;
        }
        LOGGER.info("Enabling Wayland-Xwayland layout sync for dialog titled '{}'", dialog.getTitle());

        final String title = dialog.getTitle();
        final Dimension[] lastClient = { null };

        new Timer(200, e -> {
            if (!dialog.isDisplayable() || !dialog.isShowing() || root == null) {
                return;
            }

            Dimension client = readXwininfoClientSizeByTitle(title);
            if (client == null || client.equals(lastClient[0])) {
                return;
            }
            lastClient[0] = client;

            System.out.println("Setting dialog internal size to: " + client.width + "x" + client.height);

            dialog.setBounds(0,0, client.width, client.height);
            System.out.println("  Dialog bounds set to: " + dialog.getBounds() + " , content pane: " + dialog.getContentPane().getBounds());
       //     dialog.getContentPane().setBounds(0,0,client.width, client.height);
        //    dialog.getContentPane().revalidate();
         //   dialog.getContentPane().doLayout();
          //  dialog.repaint();

            // Important: We do not change window size, only internal root component,
            // so Swing layout can count with real space.
           // root.setBounds(0, 0, client.width, client.height);

            // re-layout
        //    root.doLayout();
          //  root.revalidate();
           // root.repaint();
        }).start();
    }

    private static Dimension readXwininfoClientSizeByTitle(String title) {
        try {
            Process p = new ProcessBuilder("xwininfo", "-name", title).start();
            String out = new String(p.getInputStream().readAllBytes());
            p.waitFor();

            int w = -1, h = -1;
            for (String line : out.split("\n")) {
                line = line.trim();
                if (line.startsWith("Width:")) {
                    w = Integer.parseInt(line.substring("Width:".length()).trim());
                } else if (line.startsWith("Height:")) {
                    h = Integer.parseInt(line.substring("Height:".length()).trim());
                }
            }

            if (w > 0 && h > 0) {
                return new Dimension(w, h);
            }
        } catch (Exception ignore) {
            // xwininfo can fail for a while until window doesn't exist / is not mapped
        }
        return null;
    }
}
