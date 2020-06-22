package net.emustudio.application.gui.debugtable;

import net.emustudio.application.Constants;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

public class BooleanCellEditor extends AbstractCellEditor implements TableCellEditor, MouseListener {
    private final BooleanComponent component;
    private boolean isMouseEvent;

    public BooleanCellEditor() {
        component = new BooleanComponent(false);
        component.addMouseListener(this);
        InputMap im = component.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = component.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "click");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "click");

        am.put("click", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                component.setValue(!component.getValue());
            }
        });
    }

    @Override
    public boolean isCellEditable(EventObject e) {
        isMouseEvent = e instanceof MouseEvent;
        return true; //(e instanceof MouseEvent);
    }

    @Override
    public Object getCellEditorValue() {
        return component.getValue();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table,
                                                 Object value, boolean isSelected, int row, int column) {
        boolean state = (boolean) value;
        if (isMouseEvent) {
            state = !state;
        }
        component.setValue(state);
        component.setOpaque(isSelected);
        component.setBackground((row % 2 == 0) ? Constants.DEBUGTABLE_COLOR_ROW_ODD : Constants.DEBUGTABLE_COLOR_ROW_EVEN);
        return component;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        this.fireEditingStopped();
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.fireEditingStopped();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        this.fireEditingStopped();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        this.fireEditingStopped();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        this.fireEditingStopped();
    }
}
