/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.framework;

import net.emustudio.application.gui.components.BrowseButton;
import net.emustudio.application.gui.components.FadingBorder;
import net.emustudio.application.gui.components.ToolbarButton;
import net.emustudio.application.gui.components.ToolbarToggleButton;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.nio.file.Path;
import java.util.function.Consumer;

import static java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE;

/**
 * Concrete implementation of the GUI interface using MigLayout for panel layouts.
 */
public class GuiImpl implements GUI {

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
        button.putClientProperty("JButton.buttonType", "borderless");
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
    public javax.swing.border.Border fadingBorder(int thickness, Color color) {
        return new FadingBorder(thickness, color);
    }
}

