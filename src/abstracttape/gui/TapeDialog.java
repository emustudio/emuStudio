/**
 * TapeDialog.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *   KISS, YAGNI
 */
package abstracttape.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractListModel;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.LayoutStyle;
import javax.swing.ListCellRenderer;
import javax.swing.WindowConstants;

import plugins.ISettingsHandler;

import runtime.StaticDialogs;

import abstracttape.impl.AbstractTape;
import abstracttape.impl.TapeContext;
import abstracttape.impl.TapeContext.TapeListener;

@SuppressWarnings("serial")
public class TapeDialog extends JDialog {
	private TapeContext tape;
	private TapeListModel listModel;

	private class TapeListModel extends AbstractListModel {
		@Override
		public Object getElementAt(int index) {
			String s = tape.getSymbolAt(index);
			return (s== null || s.equals("")) ? "<empty>": s;
		}
		@Override
		public int getSize() { return tape.getSize(); }
		public void fireChange() {
			this.fireContentsChanged(this, 0, tape.getSize()-1);
		}
	}
	
    public class TapeCellRenderer extends JLabel implements ListCellRenderer {
        public TapeCellRenderer() {  super(); setOpaque(true); }

		@Override
		public Component getListCellRendererComponent(JList list,
				Object value, int index, boolean isSelected,
				boolean cellHasFocus) {			
            if (tape.getPosVisible() && (tape.getPos() == index)) {
                this.setBackground(Color.BLUE);
                this.setForeground(Color.WHITE);
            } else { 
                this.setBackground(Color.WHITE);

                String s = tape.getSymbolAt(index);
                if (s == null || s.equals(""))
                	this.setForeground(Color.LIGHT_GRAY);
                else
                	this.setForeground(Color.BLACK);
            }
            if (value != null) setText(" " + value.toString());
            else setText("");
            return this;
        }
    }
	
	public TapeDialog(AbstractTape tape, ISettingsHandler settings, long hash) {
		super();
		this.tape = (TapeContext)tape.getNextContext();
		this.listModel = new TapeListModel();
		initComponents();
		this.setTitle(tape.getTitle());
		lstTape.setModel(listModel);
		lstTape.setCellRenderer(new TapeCellRenderer());
		this.tape.setListener(new TapeListener() {
			@Override
			public void tapeChanged(EventObject evt) {
			    listModel.fireChange();
			}
		});
		String s = settings.readSetting(hash, "alwaysOnTop");
		if (s == null || !s.equals("true"))
			this.setAlwaysOnTop(false);
		else
			this.setAlwaysOnTop(true);

		changeEditable();
	}
	
	public void changeEditable() {
		boolean b = tape.getEditable();
		btnAddFirst.setEnabled(b && !tape.isBounded());
		btnAddLast.setEnabled(b);
		btnRemove.setEnabled(b);
		btnEdit.setEnabled(b);
	}
	
	private void initComponents() {
		JScrollPane scrollTape = new JScrollPane();
        lstTape = new JList();
        btnAddFirst = new JButton("Add symbol");
        btnAddLast = new JButton("Add symbol");
        btnRemove = new JButton("Remove symbol");
        btnEdit = new JButton("Edit symbol");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        scrollTape.setViewportView(lstTape);

        btnAddFirst.setIcon(new ImageIcon(getClass().getResource("/abstracttape/resources/Up24.gif"))); // NOI18N
        btnAddFirst.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = JOptionPane.showInputDialog("Enter symbol value:");
				tape.addSymbolFirst(s);
			}
        });

        btnAddLast.setIcon(new ImageIcon(getClass().getResource("/abstracttape/resources/Down24.gif"))); // NOI18N
        btnAddLast.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String s = JOptionPane.showInputDialog("Enter symbol value:");
				tape.addSymbolLast(s);
			}
        });

        btnRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = lstTape.getSelectedIndex();
				if (i == -1) {
					StaticDialogs.showErrorMessage("A symbol must be selected !");
					return;
				}
				String s = JOptionPane.showInputDialog("Enter symbol value:");
				tape.editSymbol(i,s);
			}
        });
        btnEdit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int i = lstTape.getSelectedIndex();
				if (i == -1) {
					StaticDialogs.showErrorMessage("A symbol must be selected !");
					return;
				}
				tape.removeSymbol(i);
			}
        });
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
        	layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.TRAILING, false)
                .addComponent(btnEdit, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRemove, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAddLast, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnAddFirst, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scrollTape, GroupLayout.Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 148, Short.MAX_VALUE))
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnAddFirst)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollTape, GroupLayout.PREFERRED_SIZE, 200, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnAddLast)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRemove)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnEdit)
                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        pack();
	}

    private JButton btnAddFirst;
    private JButton btnAddLast;
    private JButton btnRemove;
    private JButton btnEdit;
    private JList lstTape;
}
