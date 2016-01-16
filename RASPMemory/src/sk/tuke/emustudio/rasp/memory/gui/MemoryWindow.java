/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sk.tuke.emustudio.rasp.memory.gui;

import emulib.plugins.memory.Memory;
import emulib.runtime.StaticDialogs;
import emulib.runtime.UniversalFileFilter;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import sk.tuke.emustudio.rasp.memory.MemoryItem;
import sk.tuke.emustudio.rasp.memory.OperandType;
import sk.tuke.emustudio.rasp.memory.RASPInstruction;
import sk.tuke.emustudio.rasp.memory.RASPInstructionImpl;
import sk.tuke.emustudio.rasp.memory.NumberMemoryItem;
import sk.tuke.emustudio.rasp.memory.impl.RASPMemoryContextImpl;

/**
 * GUI window representing memory.
 *
 * @author miso
 */
public class MemoryWindow extends javax.swing.JFrame {

    private final RASPMemoryContextImpl memory;
    private final RASPTableModel tableModel;
    private File recentOpenPath;

    /**
     * Creates new form MemoryWindow
     *
     * @param context the memory context
     */
    public MemoryWindow(RASPMemoryContextImpl context) {
        this.memory = context;
        this.recentOpenPath = new File(System.getProperty("user.home"));

        initComponents();
        tableModel = new RASPTableModel(memory);
        memoryTable.setModel(tableModel);
        updateTable();
        memory.addMemoryListener(new Memory.MemoryListener() {

            @Override
            public void memoryChanged(int i) {
                updateTable();
            }

            @Override
            public void memorySizeChanged() {
                updateTable();
            }
        });
    }

    /**
     * Update the table.
     */
    public final void updateTable() {
        tableModel.fireTableDataChanged();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        memoryTable = new javax.swing.JTable();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("RASP Memory");

        memoryTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Address", "Cell value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jScrollPane1.setViewportView(memoryTable);
        if (memoryTable.getColumnModel().getColumnCount() > 0) {
            memoryTable.getColumnModel().getColumn(0).setResizable(false);
            memoryTable.getColumnModel().getColumn(1).setResizable(false);
        }

        jButton1.setBackground(new java.awt.Color(0, 0, 0));
        jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sk/tuke/emustudio/rasp/memory/gui/open-folder.png"))); // NOI18N
        jButton1.setBorder(null);
        jButton1.setBorderPainted(false);
        jButton1.setContentAreaFilled(false);
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onOpenClick(evt);
            }
        });

        jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/sk/tuke/emustudio/rasp/memory/gui/Recycle_Bin_Full.png"))); // NOI18N
        jButton2.setBorderPainted(false);
        jButton2.setContentAreaFilled(false);
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                onClearClick(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 253, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 485, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void onOpenClick(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onOpenClick
        JFileChooser chooser = new JFileChooser();
        UniversalFileFilter binaryFileFilter = new UniversalFileFilter();
        UniversalFileFilter allFilesFilter = new UniversalFileFilter();
        binaryFileFilter.addExtension("bin");
        binaryFileFilter.setDescription("Compiled program for RASP (*.bin)");

        chooser.setDialogTitle("Load memory with compiled program file");
        chooser.addChoosableFileFilter(binaryFileFilter);
        chooser.setFileFilter(binaryFileFilter);
        chooser.setApproveButtonText("Load");
        chooser.setCurrentDirectory(recentOpenPath);

        int chooserReturnValue = chooser.showOpenDialog(this);
        if (chooserReturnValue == JFileChooser.APPROVE_OPTION) {
            String filePath = chooser.getSelectedFile().getAbsolutePath();
            recentOpenPath = chooser.getCurrentDirectory();
            try {
                memory.loadFromFile(filePath);
                updateTable();
            } catch (IOException | ClassNotFoundException ex) {
                StaticDialogs.showErrorMessage("File " + filePath + " can't be read: " + ex.getMessage());
            }
        }
    }//GEN-LAST:event_onOpenClick

    private void onClearClick(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_onClearClick
        memory.clear();
    }//GEN-LAST:event_onClearClick

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
////        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
////         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
////         */
////        try {
////            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
////                if ("Nimbus".equals(info.getName())) {
////                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
////                    break;
////                }
////            }
////        } catch (ClassNotFoundException ex) {
////            java.util.logging.Logger.getLogger(MemoryWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
////        } catch (InstantiationException ex) {
////            java.util.logging.Logger.getLogger(MemoryWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
////        } catch (IllegalAccessException ex) {
////            java.util.logging.Logger.getLogger(MemoryWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
////        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
////            java.util.logging.Logger.getLogger(MemoryWindow.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
////        }
////        //</editor-fold>

        /**
         * Testing method it constructs a mock compiler result file (mock
         * compiled program), here is the example program: READ 1 LOAD =1 STORE
         * 2 LOAD 1 SUB =1 JGTZ OK JMP FINISH OK: LOAD 2 MUL 1 STORE 2 LOAD 1
         * SUB =1 STORE 1 SUB =1 JGTZ OK JMP FINISH FINISH: WRITE 2 HALT
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {

                //program as array of items
                MemoryItem[] memoryItems = new MemoryItem[]{
                    new RASPInstructionImpl(RASPInstruction.READ, OperandType.REGISTER),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.LOAD, OperandType.CONSTANT),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.STORE, OperandType.REGISTER),
                    new NumberMemoryItem(2),
                    new RASPInstructionImpl(RASPInstruction.LOAD, OperandType.REGISTER),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.SUB, OperandType.CONSTANT),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.JGTZ, OperandType.REGISTER),
                    new NumberMemoryItem(19),
                    new RASPInstructionImpl(RASPInstruction.JMP, OperandType.REGISTER),
                    new NumberMemoryItem(37),
                    new RASPInstructionImpl(RASPInstruction.LOAD, OperandType.REGISTER),
                    new NumberMemoryItem(2),
                    new RASPInstructionImpl(RASPInstruction.MUL, OperandType.REGISTER),
                    new NumberMemoryItem(2),
                    new RASPInstructionImpl(RASPInstruction.STORE, OperandType.REGISTER),
                    new NumberMemoryItem(2),
                    new RASPInstructionImpl(RASPInstruction.LOAD, OperandType.REGISTER),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.SUB, OperandType.CONSTANT),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.STORE, OperandType.REGISTER),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.SUB, OperandType.CONSTANT),
                    new NumberMemoryItem(1),
                    new RASPInstructionImpl(RASPInstruction.JGTZ, OperandType.REGISTER),
                    new NumberMemoryItem(19),
                    new RASPInstructionImpl(RASPInstruction.JMP, OperandType.REGISTER),
                    new NumberMemoryItem(37),
                    new RASPInstructionImpl(RASPInstruction.WRITE, OperandType.REGISTER),
                    new NumberMemoryItem(2),
                    new RASPInstructionImpl(RASPInstruction.HALT, OperandType.REGISTER),
                    new NumberMemoryItem(0)
                };

                //construct HashMap with labels
                HashMap<Integer, String> labels = new HashMap<>();
                labels.put(19, "OK");
                labels.put(37, "FINISH");

                //prepare program start attribute
                Integer programStart = 5;

                //construct list with memory items
                ArrayList<MemoryItem> memory = new ArrayList<>(Arrays.asList(memoryItems));

                //save program to file
                try {
                    FileOutputStream fileOutputStream = new FileOutputStream("example.bin");
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                    try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(bufferedOutputStream)) {
                        objectOutputStream.writeObject(labels);
                        objectOutputStream.writeObject(programStart);
                        objectOutputStream.writeObject(memory);
                    }

                } catch (FileNotFoundException ex) {
                    Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(MemoryWindow.class.getName()).log(Level.SEVERE, null, ex);
                }

                new MemoryWindow(new RASPMemoryContextImpl()).setVisible(true);

            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable memoryTable;
    // End of variables declaration//GEN-END:variables
}
