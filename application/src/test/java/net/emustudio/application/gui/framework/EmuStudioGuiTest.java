/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.gui.components.ToolbarButton;
import net.emustudio.application.gui.components.ToolbarToggleButton;
import net.emustudio.application.settings.AppSettings;
import net.emustudio.emulib.ui.StandardIcons;
import net.miginfocom.swing.MigLayout;
import org.junit.After;
import org.junit.Test;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.MultiResolutionImage;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.emulib.ui.StandardIcons.*;
import static net.emustudio.emulib.ui.StandardIcons.ICON_GRID;
import static net.emustudio.emulib.ui.StandardIcons.ICON_OPEN_FILE;
import static net.emustudio.application.gui.framework.EmuStudioGui.FONT_COMMON;
import static net.emustudio.emulib.ui.Constants.FONT_MONOSPACED;
import static org.junit.Assert.*;
import static org.junit.Assert.assertSame;

public class EmuStudioGuiTest {
    private final String originalLookAndFeel = UIManager.getLookAndFeel().getClass().getName();

    @After
    public void restoreLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(originalLookAndFeel);
        UIManager.put(EmuStudioGui.customKey(ICON_RUN), null);
    }

    @Test
    public void bundledIconsKeepTheirSizeAndArtworkAcrossThemes() throws Exception {
        for (String theme : new String[]{UIManager.getCrossPlatformLookAndFeelClassName(),
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"}) {
            UIManager.setLookAndFeel(theme);
            for (Field field : StandardIcons.class.getFields()) {
                if (field.getName().startsWith("ICON_") && field.getType() == String.class) {
                    Icon icon = EmuStudioGui.loadIcon((String) field.get(null));
                    assertNotNull(field.getName(), icon);
                    assertEquals(field.getName(), 22, icon.getIconWidth());
                    assertEquals(field.getName(), 22, icon.getIconHeight());
                }
            }
            assertNotSame(UIManager.getIcon("Tree.openIcon"), EmuStudioGui.loadIcon(ICON_OPEN_FILE));
            assertNotSame(UIManager.getIcon("InternalFrame.closeIcon"),
                    EmuStudioGui.loadIcon(StandardIcons.ICON_REMOVE));
        }
    }

    @Test
    public void bundledIconsUseHighResolutionArtworkWithoutChangingLayoutSize() throws Exception {
        for (Field field : StandardIcons.class.getFields()) {
            if (field.getName().startsWith("ICON_") && field.getType() == String.class) {
                ImageIcon icon = (ImageIcon) EmuStudioGui.loadIcon((String) field.get(null));
                assertNotNull(field.getName(), icon);
                assertEquals(field.getName(), 22, icon.getIconWidth());
                assertEquals(field.getName(), 22, icon.getIconHeight());
                assertTrue(field.getName(), icon.getImage() instanceof MultiResolutionImage);
                MultiResolutionImage image = (MultiResolutionImage) icon.getImage();
                for (double scale : new double[]{1.25, 1.5, 2}) {
                    Image variant = image.getResolutionVariant(22 * scale, 22 * scale);
                    assertEquals(field.getName(), 44, variant.getWidth(null));
                    assertEquals(field.getName(), 44, variant.getHeight(null));
                }
            }
        }
    }

    @Test
    public void customIconCanBeOverriddenThroughUiManager() {
        Icon override = new ImageIcon(new byte[]{0});
        UIManager.put(EmuStudioGui.customKey(ICON_RUN), override);
        assertSame(override, EmuStudioGui.loadIcon(ICON_RUN));
        assertNotNull(EmuStudioGui.loadIcon(ICON_OPEN_FILE));
    }

    @Test
    public void createLogoJLabelUsesExpectedStyling() {
        JLabel label = new EmuStudioGui().createLogoJLabel();

        assertEquals(Color.WHITE, label.getBackground());
        assertTrue(label.getBorder() instanceof FadingBorder);
        assertFalse(label.isOpaque());
    }

    @Test
    public void initializeAppliesThemeAndCustomUiDefaults() {
        for (AppSettings.Theme theme : AppSettings.Theme.values()) {
            AppSettings settings = new AppSettings(Config.inMemory(), false, false);
            settings.set(AppSettings.KEY_THEME, theme.name());

            EmuStudioGui gui = new EmuStudioGui();
            gui.initialize(settings);

            assertEquals(theme, gui.getCurrentTheme());
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
        settings.set(AppSettings.KEY_THEME, AppSettings.Theme.DARK.name());

        EmuStudioGui gui = new EmuStudioGui();
        try {
            gui.setTheme(settings);
        } finally {
            frame.dispose();
        }

        assertEquals(AppSettings.Theme.DARK, gui.getCurrentTheme());
    }


    @Test
    public void toolbarFactoriesCreateConfiguredButtonsAndToggleButtons() {
        EmuStudioGui gui = new EmuStudioGui();
        AtomicInteger actionCount = new AtomicInteger();
        AtomicInteger itemCount = new AtomicInteger();

        JButton consumerButton = gui.toolbarButton(e -> actionCount.incrementAndGet(), ICON_SAVE, "Save");
        Action action = new AbstractAction("Run") {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionCount.incrementAndGet();
            }
        };
        JButton actionButton = gui.toolbarButton(action, ICON_RUN, "Run");
        JToggleButton toggleButton = gui.toolbarToggleButton(
                e -> actionCount.incrementAndGet(),
                e -> itemCount.incrementAndGet(),
                ICON_GRID,
                "Grid"
        );
        JToggleButton simpleToggle = gui.toolbarToggleButton(e -> actionCount.incrementAndGet(), ICON_GRID, "Simple");

        assertTrue(consumerButton instanceof ToolbarButton);
        assertEquals("Save", consumerButton.getToolTipText());
        assertTrue(actionButton instanceof ToolbarButton);
        assertEquals("Run", actionButton.getToolTipText());
        assertTrue(toggleButton instanceof ToolbarToggleButton);
        assertEquals("Grid", toggleButton.getToolTipText());
        assertTrue(simpleToggle instanceof ToolbarToggleButton);
        assertEquals("Simple", simpleToggle.getToolTipText());

        consumerButton.doClick();
        actionButton.doClick();
        toggleButton.doClick();
        simpleToggle.doClick();

        assertEquals(4, actionCount.get());
        assertEquals(1, itemCount.get());
        assertTrue(toggleButton.isSelected());
        assertTrue(simpleToggle.isSelected());
    }

    @Test
    public void toolbarAndSearchControlsKeepTheirStateAcrossThemeChanges() throws Exception {
        EmuStudioGui gui = new EmuStudioGui();
        JButton button = gui.toolbarTextButton(event -> {}, ICON_FIND, "Follow link");
        button.setToolTipText("Follow the selected link");
        JToggleButton toggle = gui.toolbarToggleButton(event -> {}, ICON_RECORD, "Record instructions");
        toggle.setSelected(true);
        toggle.setEnabled(false);
        EmuStudioGui.toolbarIcon(toggle, ICON_STOP);
        JTextField search = gui.searchField("Search annotations", 14, ICON_FIND);
        search.setText("entry");
        JPanel panel = gui.toolbarPanel();
        gui.toolbarRow(panel, button, toggle, search);

        for (String theme : new String[]{UIManager.getCrossPlatformLookAndFeelClassName(),
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"}) {
            UIManager.setLookAndFeel(theme);
            SwingUtilities.updateComponentTreeUI(panel);
            assertEquals("Follow link", button.getText());
            assertEquals("Follow link", button.getAccessibleContext().getAccessibleName());
            assertEquals("Follow the selected link", button.getToolTipText());
            assertTrue(button.isFocusable());
            assertTrue(toggle.isFocusable());
            assertTrue(toggle.isSelected());
            assertFalse(toggle.isEnabled());
            assertNotNull(toggle.getIcon());
            assertNotNull(toggle.getDisabledIcon());
            assertEquals("entry", search.getText());
            assertEquals("Search annotations", search.getAccessibleContext().getAccessibleName());
            assertNotNull(search.getClientProperty("JTextField.leadingIcon"));
        }
    }

    @Test
    public void labelsButtonsAndTextFactoriesApplyExpectedStyling() {
        EmuStudioGui gui = new EmuStudioGui();
        JLabel plain = gui.label("plain");
        JLabel bold = gui.labelBold("bold");
        JLabel title = gui.labelTitle("title");
        JButton primary = gui.buttonMakePrimary(new JButton("Primary"));
        JButton simpleButton = gui.button("Push");
        JTextField sizedField = gui.textField("custom", 8);
        JTextArea textArea = gui.textAreaReadOnly(20, 3);

        assertEquals("plain", plain.getText());
        assertEquals(Font.BOLD, bold.getFont().getStyle());
        assertEquals(Font.BOLD, title.getFont().getStyle());
        assertTrue(title.getFont().getSize() > plain.getFont().getSize());
        assertEquals(Font.BOLD, primary.getFont().getStyle());
        assertEquals("Push", simpleButton.getText());
        assertEquals(8, sizedField.getColumns());
        assertFalse(textArea.isEditable());
        assertEquals(20, textArea.getRows());
        assertEquals(3, textArea.getColumns());
    }

    @Test
    public void containerFactoriesAndStylingHelpersReturnConfiguredComponents() {
        EmuStudioGui gui = new EmuStudioGui();
        JTable table = new JTable(2, 2);
        JLabel left = new JLabel("left");
        JLabel right = new JLabel("right");
        JLabel top = new JLabel("top");
        JLabel bottom = new JLabel("bottom");

        JToolBar toolBar = gui.toolBar();
        JSplitPane leftRight = gui.splitPane(JSplitPane.HORIZONTAL_SPLIT, left, right, 0.25);
        JSplitPane topBottom = gui.splitPane(JSplitPane.VERTICAL_SPLIT, top, bottom, 0.75);
        JScrollPane scrollPane = gui.scrollPane(new JTextArea("scroll"));
        JPanel customPanel = gui.panel("fill", "[grow]", "[]");
        JPanel section = gui.section("Section", "fill", "[grow]", "[]");

        gui.styleTable(table);
        assertTrue(table.getRowHeight() >= 22 + 4);

        assertFalse(toolBar.isFloatable());
        assertTrue(toolBar.isRollover());
        assertFalse(toolBar.isBorderPainted());
        assertTrue(leftRight.isOneTouchExpandable());
        assertTrue(leftRight.isContinuousLayout());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, leftRight.getOrientation());
        assertEquals(0.25, leftRight.getResizeWeight(), 0.0);
        assertSame(left, leftRight.getLeftComponent());
        assertSame(right, leftRight.getRightComponent());
        assertEquals(JSplitPane.VERTICAL_SPLIT, topBottom.getOrientation());
        assertSame(top, topBottom.getTopComponent());
        assertSame(bottom, topBottom.getBottomComponent());
        assertEquals(0.75, topBottom.getResizeWeight(), 0.0);
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
        assertTrue(customPanel.getLayout() instanceof MigLayout);
        assertTrue(section.getBorder() instanceof TitledBorder);
        assertEquals("Section", ((TitledBorder) section.getBorder()).getTitle());
        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertEquals(new Dimension(0, 0), table.getIntercellSpacing());
    }
}
