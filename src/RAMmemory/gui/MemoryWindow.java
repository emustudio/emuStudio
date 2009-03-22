/**
 * MemoryWindow.java
 * 
 * (c) Copyright 2009, P. Jakubčo
 * 
 *  KISS, YAGNI
 */

package RAMmemory.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.LayoutStyle;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.table.AbstractTableModel;

import plugins.memory.IMemoryContext.IMemListener;

import RAMmemory.impl.RAMContext;
import RAMmemory.impl.RAMInstruction;

@SuppressWarnings("serial")
public class MemoryWindow extends JFrame {
	private RAMContext mem;
	private RAMTableModel ram;

	private class RAMTableModel extends AbstractTableModel {
		@Override
		public int getColumnCount() { return 2; }
		@Override
		public int getRowCount() { return mem.getSize(); }
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {
			if (columnIndex == 0) return rowIndex;
			else {
				RAMInstruction i = (RAMInstruction)mem.read(rowIndex);
				return i.getInstr() + " " + i.getOperand();
			}
		}
	    @Override
	    public Class<?> getColumnClass(int columnIndex) {
	        return (columnIndex == 0)?Integer.class:String.class;
	    }
	    @Override
	    public String getColumnName(int col) {
	        return (col==0)?"Addr":"Instruction";
	    }
	}
	
	public MemoryWindow(RAMContext mem) {
		this.mem = mem;
		initComponents();
		
		ram = new RAMTableModel();
		tableProgram.setModel(ram);
		refillTable();
		mem.addMemoryListener(new IMemListener() {
			@Override
			public void memChange(EventObject evt, int pos) {
				refillTable();
			}
		});
	}
	
	// TODO: complexity
	public void refillTable() {
		int add=0,mul=0,div=0,sub=0,load=0,store=0,
		    jmp=0,jz=0,halt=0,read=0,write=0;
		int count=0,i,j;
		
		ram.fireTableDataChanged();

		j = mem.getSize();
		for (i = 0; i < j; i++) {
			count++;
			switch(((RAMInstruction)mem.read(i)).getInstr()) {
			case RAMInstruction.LOAD:  load++;
			case RAMInstruction.STORE: store++;
			case RAMInstruction.READ:  read++;
			case RAMInstruction.WRITE: write++;
			case RAMInstruction.ADD:   add++;
			case RAMInstruction.SUB:   sub++;
			case RAMInstruction.MUL:   mul++;
			case RAMInstruction.DIV:   div++;
			case RAMInstruction.JMP:   jmp++;
			case RAMInstruction.JZ:    jz++;
			case RAMInstruction.HALT:  halt++;
			}
		}
		lblCount.setText(String.valueOf(count));
		lblLOAD.setText(String.valueOf(load));
		lblSTORE.setText(String.valueOf(store));
		lblREAD.setText(String.valueOf(read));
		lblWRITE.setText(String.valueOf(write));
		lblADD.setText(String.valueOf(add));
		lblSUB.setText(String.valueOf(sub));
		lblMUL.setText(String.valueOf(mul));
		lblDIV.setText(String.valueOf(div));
		lblJMP.setText(String.valueOf(jmp));
		lblJZ.setText(String.valueOf(jz));
		lblHALT.setText(String.valueOf(halt));
	}
	
	public void initComponents() {
        toolMemory = new JToolBar();
        btnLoad = new JButton();
        btnClear = new JButton();
        scrollProgram = new JScrollPane();
        tableProgram = new JTable();
        lblTapeContent = new JLabel("Tape content:");
        panelInfo = new JPanel();
        panelComplexity = new JPanel();
        lblSimpleLBL = new JLabel("Simple:");
        JLabel lblLogaritmicLBL = new JLabel("Logaritmic:");
        lblLogaritmic = new JLabel("0");
        lblSimple = new JLabel("0");
        panelInstr = new JPanel();
        JLabel lblCountLBL = new JLabel("Instructions count:");
        lblCount = new JLabel("0");
        JLabel lblAddLBL = new JLabel("ADD:");
        JLabel lblSubLBL = new JLabel("SUB:");
        JLabel lblMulLBL = new JLabel("MUL:");
        JLabel lblDivLBL = new JLabel("DIV:");
        lblMUL = new JLabel("0");
        lblSUB = new JLabel("0");
        lblADD = new JLabel("0");
        lblDIV = new JLabel("0");
        JLabel lblJmpLBL = new JLabel("JMP:");
        JLabel lblJzLBL = new JLabel("JZ:");
        JLabel lblHaltLBL = new JLabel("HALT:");
        JLabel lblLoadLBL = new JLabel("LOAD:");
        JLabel lblStoreLBL = new JLabel("STORE:");
        JLabel lblReadLBL = new JLabel("READ:");
        JLabel lblWriteLBL = new JLabel("WRITE:");
        lblHALT = new JLabel("0");
        lblJZ = new JLabel("0");
        lblJMP = new JLabel("0");
        lblSTORE = new JLabel("0");
        lblLOAD = new JLabel("0");
        lblWRITE = new JLabel("0");
        lblREAD = new JLabel("0");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        toolMemory.setFloatable(false);
        toolMemory.setRollover(true);

        btnLoad.setIcon(new ImageIcon(getClass().getResource("/resources/Open24.gif"))); // NOI18N
        btnLoad.setFocusable(false);
        btnLoad.setHorizontalTextPosition(SwingConstants.CENTER);
        btnLoad.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnLoad.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
			}
        });
        toolMemory.add(btnLoad);

        btnClear.setIcon(new ImageIcon(getClass().getResource("/resources/Delete24.gif"))); // NOI18N
        btnClear.setFocusable(false);
        btnClear.setHorizontalTextPosition(SwingConstants.CENTER);
        btnClear.setVerticalTextPosition(SwingConstants.BOTTOM);
        btnClear.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mem.clearMemory();
			}
        });
        toolMemory.add(btnClear);

        scrollProgram.setViewportView(tableProgram);

        panelInfo.setBorder(BorderFactory.createTitledBorder("Instructions Info"));
        panelComplexity.setBorder(BorderFactory.createTitledBorder("Time Complexity"));
        lblLogaritmic.setFont(lblLogaritmic.getFont().deriveFont(lblLogaritmic.getFont().getStyle() | java.awt.Font.BOLD));
        lblSimple.setFont(lblSimple.getFont().deriveFont(lblSimple.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout panelComplLayout = new GroupLayout(panelComplexity);
        panelComplexity.setLayout(panelComplLayout);
        panelComplLayout.setHorizontalGroup(
            panelComplLayout.createSequentialGroup()
            .addContainerGap()
            .addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblLogaritmicLBL)
                .addComponent(lblSimpleLBL))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblSimple)
                .addComponent(lblLogaritmic))
            .addContainerGap(134, Short.MAX_VALUE));
        
        panelComplLayout.setVerticalGroup(
        	panelComplLayout.createSequentialGroup()
        	.addContainerGap()
            .addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblSimpleLBL)
                .addComponent(lblSimple))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(panelComplLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblLogaritmicLBL)
                .addComponent(lblLogaritmic))
            .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

        panelInstr.setBorder(BorderFactory.createTitledBorder("Instructions usage"));

        lblCount.setFont(lblCount.getFont().deriveFont(lblCount.getFont().getStyle() | java.awt.Font.BOLD));
        lblMUL.setFont(lblMUL.getFont().deriveFont(lblMUL.getFont().getStyle() | java.awt.Font.BOLD));
        lblSUB.setFont(lblSUB.getFont().deriveFont(lblSUB.getFont().getStyle() | java.awt.Font.BOLD));
        lblADD.setFont(lblADD.getFont().deriveFont(lblADD.getFont().getStyle() | java.awt.Font.BOLD));
        lblDIV.setFont(lblDIV.getFont().deriveFont(lblDIV.getFont().getStyle() | java.awt.Font.BOLD));

        lblHALT.setFont(lblHALT.getFont().deriveFont(lblHALT.getFont().getStyle() | java.awt.Font.BOLD));
        lblJZ.setFont(lblJZ.getFont().deriveFont(lblJZ.getFont().getStyle() | java.awt.Font.BOLD));
        lblJMP.setFont(lblJMP.getFont().deriveFont(lblJMP.getFont().getStyle() | java.awt.Font.BOLD));
        lblSTORE.setFont(lblSTORE.getFont().deriveFont(lblSTORE.getFont().getStyle() | java.awt.Font.BOLD));
        lblLOAD.setFont(lblLOAD.getFont().deriveFont(lblLOAD.getFont().getStyle() | java.awt.Font.BOLD));
        lblWRITE.setFont(lblWRITE.getFont().deriveFont(lblWRITE.getFont().getStyle() | java.awt.Font.BOLD));
        lblREAD.setFont(lblREAD.getFont().deriveFont(lblREAD.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout panelInstrLayout = new GroupLayout(panelInstr);
        panelInstr.setLayout(panelInstrLayout);
        panelInstrLayout.setHorizontalGroup(
        	panelInstrLayout.createSequentialGroup()
        	.addContainerGap()
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addGroup(panelInstrLayout.createSequentialGroup()
        				.addComponent(lblCountLBL)
        				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        				.addComponent(lblCount))
        		.addGroup(panelInstrLayout.createSequentialGroup()
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblAddLBL)
        				.addComponent(lblSubLBL)
        				.addComponent(lblMulLBL)
        				.addComponent(lblDivLBL))
        			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblADD)
        				.addComponent(lblSUB)
        				.addComponent(lblMUL)
        				.addComponent(lblDIV))
        			.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblJmpLBL)
        				.addComponent(lblJzLBL)
        				.addComponent(lblHaltLBL))
        			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblJMP)
        				.addComponent(lblJZ)
        				.addComponent(lblHALT))
        			.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblLoadLBL)
        				.addComponent(lblStoreLBL)
        				.addComponent(lblReadLBL)
        				.addComponent(lblWriteLBL))
        			.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        			.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
          				.addComponent(lblLOAD)
           				.addComponent(lblSTORE)
           				.addComponent(lblREAD)
           				.addComponent(lblWRITE))))
           	.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
        
        panelInstrLayout.setVerticalGroup(
        	panelInstrLayout.createSequentialGroup()
        	.addContainerGap()
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(lblCountLBL)
        		.addComponent(lblCount))
        	.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(lblAddLBL)
        		.addComponent(lblADD)
        		.addComponent(lblJmpLBL)
        		.addComponent(lblJMP)
        		.addComponent(lblLoadLBL)
        		.addComponent(lblLOAD))
        	.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(lblSubLBL)
        		.addComponent(lblSUB)
        		.addComponent(lblJzLBL)
        		.addComponent(lblJZ)
        		.addComponent(lblStoreLBL)
        		.addComponent(lblSTORE))
        	.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(lblMulLBL)
        		.addComponent(lblMUL)
        		.addComponent(lblHaltLBL)
        		.addComponent(lblHALT)
        		.addComponent(lblReadLBL)
        		.addComponent(lblREAD))
        	.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        	.addGroup(panelInstrLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        		.addComponent(lblDivLBL)
        		.addComponent(lblDIV)
        		.addComponent(lblWriteLBL)
        		.addComponent(lblWRITE))
        	.addContainerGap());

        GroupLayout panelInfoLayout = new GroupLayout(panelInfo);
        panelInfo.setLayout(panelInfoLayout);
        panelInfoLayout.setHorizontalGroup(
        	panelInfoLayout.createSequentialGroup()
        	.addContainerGap()
        	.addGroup(panelInfoLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        		.addComponent(panelInstr)
        		.addComponent(panelComplexity))
        	.addContainerGap());
        
        panelInfoLayout.setVerticalGroup(
        	panelInfoLayout.createSequentialGroup()
        	.addContainerGap()
        	.addComponent(panelInstr)
        	.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        	.addComponent(panelComplexity)
        	.addContainerGap());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(toolMemory, GroupLayout.DEFAULT_SIZE, 554, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTapeContent)
                .addContainerGap(444, Short.MAX_VALUE))
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollProgram, GroupLayout.PREFERRED_SIZE, 233, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(toolMemory, GroupLayout.PREFERRED_SIZE, 31, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblTapeContent)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(panelInfo, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollProgram, GroupLayout.DEFAULT_SIZE, 302, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
	}
	
    private JButton btnClear;
    private JButton btnLoad;
    private JLabel lblHALT;
    private JLabel lblJZ;
    private JLabel lblJMP;
    private JLabel lblSTORE;
    private JLabel lblLOAD;
    private JLabel lblSimpleLBL;
    private JLabel lblWRITE;
    private JLabel lblREAD;
    private JPanel panelComplexity;
    private JPanel panelInstr;
    private JScrollPane scrollProgram;
    private JLabel lblADD;
    private JLabel lblCount;
    private JLabel lblDIV;
    private JLabel lblLogaritmic;
    private JLabel lblMUL;
    private JLabel lblSUB;
    private JLabel lblSimple;
    private JLabel lblTapeContent;
    private JTable tableProgram;
    private JPanel panelInfo;
    private JToolBar toolMemory;
}
