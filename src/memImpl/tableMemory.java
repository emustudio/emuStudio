/*
 * tableMemory.java
 *
 * Created on Nede�a, 2007, okt�ber 28, 13:06
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package memImpl;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import plugins.memory.*;
/**
 *
 * @author vbmacher
 */
public class tableMemory extends JTable {
    private memoryTableModel memModel;
    private IMemory mem;
    private JScrollPane paneMemory;
    
    /** Creates a new instance of tableMemory */
    public tableMemory(IMemory mem, memoryTableModel memModel, JScrollPane pm) {
        this.mem = mem;
        this.paneMemory = pm;
        this.memModel = memModel;
        this.setModel(this.memModel);
        this.setFont(new Font("Monospaced",Font.PLAIN,12));
        this.setCellSelectionEnabled(true);
        this.setFocusCycleRoot(true);
        this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.getTableHeader().setFont(new Font("Monospaced",Font.PLAIN,12));
        this.setDefaultRenderer(Object.class, new MemCellRenderer(this));
    }
    
    // riadkovy header
    public class MemRowHeaderRenderer extends JLabel implements ListCellRenderer {
        public MemRowHeaderRenderer(JTable table) {
            JTableHeader header = table.getTableHeader();
            setOpaque(true);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            setHorizontalAlignment(CENTER);
            setForeground(header.getForeground());
            setBackground(header.getBackground());
            setFont(header.getFont());
        }
  
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }
    
    /* farebnost a zobrazenie buniek v operacnej pamati */
    public class MemCellRenderer extends JLabel implements TableCellRenderer {
        private JList rowHeader;
        private String adresses[];
        private int currentPage;
        private tableMemory tm;

        public MemCellRenderer(tableMemory tm) {
            this.tm = tm;
            currentPage = memModel.getPage();
            adresses = new String[memModel.getRowCount()];
            for (int i = 0; i < adresses.length; i++)
                adresses[i] = String.format("%1$04Xh", 
                        memModel.getColumnCount() * i + memModel.getColumnCount()
                        * memModel.getRowCount() * currentPage);
            this.setOpaque(true);
            rowHeader = new JList(adresses);
            this.setFont(new Font("Monospaced",Font.PLAIN,12));
            
            FontMetrics fm = rowHeader.getFontMetrics(rowHeader.getFont());
            int char_width = 12;
            if (fm != null) char_width = fm.stringWidth("F");
            
            rowHeader.setFixedCellWidth(char_width * 5);
            rowHeader.setFixedCellHeight(getRowHeight());
            rowHeader.setCellRenderer(new MemRowHeaderRenderer(this.tm));
            setHorizontalAlignment(CENTER);
            paneMemory.setRowHeaderView(rowHeader);
        }
        
        private void remakeAdresses() {
            if (currentPage == memModel.getPage()) return;
            currentPage = memModel.getPage();
            for (int i = 0; i < adresses.length; i++)
                adresses[i] = String.format("%1$04Xh",
                        memModel.getColumnCount() * i + memModel.getColumnCount()
                        * memModel.getRowCount() * currentPage);
            rowHeader.setListData(adresses);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, 
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                this.setBackground(tm.getSelectionBackground());
                this.setForeground(tm.getSelectionForeground());
            } else {
                if (memModel.isROMAt(row,column) == true)
                    this.setBackground(Color.RED); 
                else this.setBackground(Color.WHITE); 
                this.setForeground(Color.BLACK); 
            }
            remakeAdresses();
            setText(value.toString());
            return this;
        }
    }

    
}
