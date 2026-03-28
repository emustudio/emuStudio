/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui;

import net.emustudio.application.gui.components.BrowseButton;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.gui.components.ToolbarButton;
import net.emustudio.application.gui.components.ToolbarToggleButton;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.miginfocom.swing.MigLayout;
import org.junit.Test;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_GRID;
import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_OPEN_FILE;
import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_RUN;
import static net.emustudio.application.gui.framework.EmuStudioUI.ICON_SAVE;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GUIImplTest {

    @Test
    public void toolbarFactoriesCreateConfiguredButtonsAndToggleButtons() {
        GUIImpl gui = new GUIImpl();
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
    public void labelsButtonsAndTextFactoriesApplyExpectedStyling() {
        GUIImpl gui = new GUIImpl();
        AtomicInteger clicks = new AtomicInteger();

        JLabel plain = gui.label("plain");
        JLabel bold = gui.labelBold("bold");
        JLabel title = gui.labelTitle("title");
        JLabel padded = gui.labelPadded("pad", 1, 2, 3, 4);
        JButton primary = gui.buttonMakePrimary(new JButton("Primary"));
        JButton simpleButton = gui.button("Push");
        JButton runnableButton = gui.button("Run", clicks::incrementAndGet);
        JButton iconButton = gui.button(ICON_OPEN_FILE, "Open", clicks::incrementAndGet);
        JButton listenerButton = gui.button("Listen", e -> clicks.incrementAndGet());
        JMenuItem menuItem = gui.menuItem(new AbstractAction("Menu") {
            @Override
            public void actionPerformed(ActionEvent e) {
                clicks.incrementAndGet();
            }
        });
        JTextField defaultField = gui.textField("default");
        JTextField sizedField = gui.textField("custom", 8);
        JTextArea textArea = gui.textAreaReadOnly(20, 3);

        assertEquals("plain", plain.getText());
        assertEquals(Font.BOLD, bold.getFont().getStyle());
        assertEquals(Font.BOLD, title.getFont().getStyle());
        assertTrue(title.getFont().getSize() > plain.getFont().getSize());
        assertTrue(padded.getBorder() instanceof EmptyBorder);
        assertEquals("borderless", primary.getClientProperty("JButton.buttonType"));
        assertEquals(Font.BOLD, primary.getFont().getStyle());
        assertEquals("Push", simpleButton.getText());
        assertEquals("Menu", menuItem.getText());
        assertEquals(20, defaultField.getColumns());
        assertEquals(8, sizedField.getColumns());
        assertFalse(textArea.isEditable());

        runnableButton.doClick();
        iconButton.doClick();
        listenerButton.doClick();
        menuItem.doClick();

        assertEquals(4, clicks.get());
    }

    @Test
    public void browseButtonsReusePreviousSelectionAsBaseDirectory() {
        GUIImpl gui = new GUIImpl();
        Dialogs dialogs = mock(Dialogs.class);
        List<Path> approvedPaths = new ArrayList<>();
        Path defaultBase = Path.of(System.getProperty("user.dir"));
        Path firstDirectory = Path.of("first");
        Path secondDirectory = Path.of("second");
        Path selectedFile = Path.of("program.asm");
        FileExtensionsFilter filter = new FileExtensionsFilter("Assembly", "asm");

        when(dialogs.chooseDirectory("Directories", "Select", defaultBase)).thenReturn(java.util.Optional.of(firstDirectory));
        when(dialogs.chooseDirectory("Directories", "Select", firstDirectory)).thenReturn(java.util.Optional.of(secondDirectory));
        when(dialogs.chooseFile(eq("Files"), eq("Open"), eq(defaultBase), eq(true), any(FileExtensionsFilter[].class)))
                .thenReturn(java.util.Optional.of(selectedFile));

        JButton directories = gui.buttonBrowseDirectories(dialogs, "Directories", "Select", approvedPaths::add);
        JButton files = gui.buttonBrowseFiles(dialogs, "Files", "Open", true, approvedPaths::add, filter);

        assertTrue(directories instanceof BrowseButton);
        assertTrue(files instanceof BrowseButton);

        directories.doClick();
        directories.doClick();
        files.doClick();

        assertEquals(List.of(firstDirectory, secondDirectory, selectedFile), approvedPaths);
    }

    @Test
    public void containerFactoriesAndStylingHelpersReturnConfiguredComponents() {
        GUIImpl gui = new GUIImpl();
        JTable table = new JTable(2, 2);
        JList<String> list = new JList<>(new String[]{"a", "b"});
        JLabel left = new JLabel("left");
        JLabel right = new JLabel("right");
        JLabel top = new JLabel("top");
        JLabel bottom = new JLabel("bottom");

        JToolBar toolBar = gui.toolBar();
        JToolBar verticalToolBar = gui.toolBarVertical();
        JSplitPane splitPane = gui.splitPane();
        JSplitPane leftRight = gui.splitPaneLeftToRight(left, right, 0.25);
        JSplitPane topBottom = gui.splitPaneTopToBottom(top, bottom, 0.75);
        JScrollPane scrollPane = gui.scrollPane(new JTextArea("scroll"));
        JPanel verticalPanel = gui.panelVertical();
        JPanel customPanel = gui.panel("fill", "[grow]", "[]");
        JPanel horizontalPanel = gui.panelHorizontal();
        JPanel buttonsPanel = gui.panelButtons();
        JPanel section = gui.section("Section", "fill", "[grow]", "[]");
        Border border = gui.fadingBorder(6, Color.BLUE);

        gui.styleTable(table);
        gui.styleList(list);

        assertFalse(toolBar.isFloatable());
        assertTrue(toolBar.isRollover());
        assertFalse(toolBar.isBorderPainted());
        assertEquals(JToolBar.VERTICAL, verticalToolBar.getOrientation());
        assertTrue(splitPane.isOneTouchExpandable());
        assertTrue(splitPane.isContinuousLayout());
        assertEquals(JSplitPane.HORIZONTAL_SPLIT, leftRight.getOrientation());
        assertSame(left, leftRight.getLeftComponent());
        assertSame(right, leftRight.getRightComponent());
        assertEquals(JSplitPane.VERTICAL_SPLIT, topBottom.getOrientation());
        assertSame(top, topBottom.getTopComponent());
        assertSame(bottom, topBottom.getBottomComponent());
        assertTrue(scrollPane.getBorder() instanceof EmptyBorder);
        assertTrue(verticalPanel.getLayout() instanceof MigLayout);
        assertTrue(customPanel.getLayout() instanceof MigLayout);
        assertTrue(horizontalPanel.getLayout() instanceof MigLayout);
        assertTrue(buttonsPanel.getLayout() instanceof MigLayout);
        assertTrue(section.getBorder() instanceof TitledBorder);
        assertEquals("Section", ((TitledBorder) section.getBorder()).getTitle());
        assertFalse(table.getShowHorizontalLines());
        assertFalse(table.getShowVerticalLines());
        assertEquals(new Dimension(0, 0), table.getIntercellSpacing());
        assertTrue(list.getBorder() instanceof EmptyBorder);
        assertTrue(border instanceof FadingBorder);
    }
}
