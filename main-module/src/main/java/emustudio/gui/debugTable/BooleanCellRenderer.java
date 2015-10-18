package emustudio.gui.debugTable;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

public class BooleanCellRenderer extends JLabel implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        boolean boolValue = (value == null) ? false : (Boolean) value;

        if (boolValue) {
            setIcon(new ImageIcon(getClass().getResource("/emustudio/gui/breakpoint.png")));
        } else {
            setIcon(null);
        }
        setBackground((row % 2 == 0) ? Colors.ODD_ROW_COLOR : Colors.EVEN_ROW_COLOR);
        setText(" ");
        this.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        return this;
    }

    @Override
    public void paint(Graphics g) {
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paint(g);
    }
}
