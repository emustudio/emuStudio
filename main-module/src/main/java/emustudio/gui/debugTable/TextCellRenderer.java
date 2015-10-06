package emustudio.gui.debugTable;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;

class TextCellRenderer extends JLabel implements TableCellRenderer {
    private final DebugTableModel model;
    public int height = 17;

    public TextCellRenderer(DebugTableModel model) {
        super();
        this.model = model;
        setOpaque(true);
        setFont(getFont().deriveFont(getFont().getStyle() & ~java.awt.Font.BOLD));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        if (model.isCurrent(row)) {
            setBackground(Color.RED);
            setForeground(Color.WHITE);
        } else {
            setBackground((row % 2 == 0) ? Color.WHITE : DebugTableImpl.EVEN_ROW_COLOR);
            setForeground(Color.BLACK);
        }
        if (value != null) {
            setText(" " + value.toString());
        } else {
            setText(" ");
        }
        height = getPreferredSize().height + 1;
        return this;
    }

}
