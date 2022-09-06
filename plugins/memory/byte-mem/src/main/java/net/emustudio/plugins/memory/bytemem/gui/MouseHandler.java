package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.plugins.memory.bytemem.gui.model.MemoryTableModel;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Objects;

public class MouseHandler extends MouseAdapter {
    private final MemoryTableModel tableModel;
    private final Runnable updateMemoryValue;

    public MouseHandler(MemoryTableModel tableModel, Runnable updateMemoryValue) {
        this.tableModel = Objects.requireNonNull(tableModel);
        this.updateMemoryValue = Objects.requireNonNull(updateMemoryValue);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int page = tableModel.getPage();
        int rotation = e.getWheelRotation();
        try {
            tableModel.setPage(page + rotation);
        } catch (IndexOutOfBoundsException ignored) {
            if (rotation < 0) {
                tableModel.setPage(tableModel.getPageCount() - 1);
            } else {
                tableModel.setPage(0);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        super.mousePressed(e);
        updateMemoryValue.run();
    }
}
