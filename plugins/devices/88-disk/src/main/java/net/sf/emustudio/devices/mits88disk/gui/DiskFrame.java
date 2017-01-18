/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.mits88disk.gui;

import net.sf.emustudio.devices.mits88disk.impl.Drive;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.util.List;
import java.util.Objects;

import static emulib.runtime.RadixUtils.formatBinaryString;

public class DiskFrame extends JFrame {
    private final static String GUI_PATH = "/net/sf/emustudio/devices/mits88disk/gui/";

    private final List<Drive> drives;

    private class GUIDriveListener implements Drive.DriveListener {
        private final int index;

        private GUIDriveListener(int index) {
            this.index = index;
        }

        @Override
        public void driveSelect(final boolean selected) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    select(index, selected);
                }
            });
        }

        @Override
        public void driveParamsChanged(final Drive.DriveParameters parameters) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateDriveInfo(parameters);
                }
            });
        }
    }

    public DiskFrame(List<Drive> drives) {
        this.drives = Objects.requireNonNull(drives);

        initComponents();
        this.setLocationRelativeTo(null);

        for (int index = 0; index < drives.size(); index++) {
            drives.get(index).setDriveListener(new GUIDriveListener(index));
        }        
    }

    private void updateDriveInfo(Drive drive) {
        updateDriveInfo(drive.getDriveParameters());
    }

    private void updateDriveInfo(Drive.DriveParameters parameters) {
        lblPort1Status.setText(formatBinaryString(parameters.port1status, 8));
        lblPort2Status.setText(formatBinaryString(parameters.port2status, 8));
        
        lblSector.setText(String.valueOf(parameters.sector));
        lblTrack.setText(String.valueOf(parameters.track));
        lblOffset.setText(String.valueOf(parameters.sectorOffset));

        if (parameters.mountedFloppy != null) {
            txtMountedImage.setText(parameters.mountedFloppy.getPath());
        } else {
            txtMountedImage.setText("none");
        }
    }

    public void select(int driveIndex, boolean selected) {
        String resourceFile = selected ? GUI_PATH + "on.gif" : GUI_PATH + "off.gif";
        if (driveIndex >= 0 && driveIndex < driveButtons.length) {
            driveButtons[driveIndex].setIcon(new ImageIcon(getClass().getResource(resourceFile)));
        }
    }
    

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        javax.swing.ButtonGroup buttonGroup1 = new javax.swing.ButtonGroup();
        javax.swing.JPanel jPanel1 = new javax.swing.JPanel();
        javax.swing.JPanel jPanel2 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel1 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel2 = new javax.swing.JLabel();
        lblPort1Status = new javax.swing.JLabel();
        lblPort2Status = new javax.swing.JLabel();
        javax.swing.JPanel jPanel3 = new javax.swing.JPanel();
        javax.swing.JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
        txtMountedImage = new javax.swing.JTextArea();
        javax.swing.JPanel jPanel4 = new javax.swing.JPanel();
        javax.swing.JLabel jLabel3 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel4 = new javax.swing.JLabel();
        javax.swing.JLabel jLabel5 = new javax.swing.JLabel();
        lblOffset = new javax.swing.JLabel();
        lblSector = new javax.swing.JLabel();
        lblTrack = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MITS 88-DISK (floppy)");
        setResizable(false);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Disk selection", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", Font.BOLD, 11))); // NOI18N

        buttonGroup1.add(btn0);
        btn0.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn0.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn0.setText("A");
        btn0.addActionListener(this::btn0ActionPerformed);

        buttonGroup1.add(btn1);
        btn1.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn1.setText("B");
        btn1.addActionListener(this::btn1ActionPerformed);

        buttonGroup1.add(btn2);
        btn2.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn2.setText("C");
        btn2.addActionListener(this::btn2ActionPerformed);

        buttonGroup1.add(btn3);
        btn3.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn3.setText("D");
        btn3.addActionListener(this::btn3ActionPerformed);

        buttonGroup1.add(btn4);
        btn4.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn4.setText("E");
        btn4.addActionListener(this::btn4ActionPerformed);

        buttonGroup1.add(btn5);
        btn5.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn5.setText("F");
        btn5.addActionListener(this::btn5ActionPerformed);

        buttonGroup1.add(btn6);
        btn6.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn6.setText("G");
        btn6.addActionListener(this::btn6ActionPerformed);

        buttonGroup1.add(btn7);
        btn7.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn7.setText("H");
        btn7.addActionListener(this::btn7ActionPerformed);

        buttonGroup1.add(btn8);
        btn8.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn8.setText("I");
        btn8.addActionListener(this::btn8ActionPerformed);

        buttonGroup1.add(btn9);
        btn9.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn9.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn9.setText("J");
        btn9.addActionListener(this::btn9ActionPerformed);

        buttonGroup1.add(btn10);
        btn10.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn10.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn10.setText("K");
        btn10.addActionListener(this::btn10ActionPerformed);

        buttonGroup1.add(btn11);
        btn11.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn11.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn11.setText("L");
        btn11.addActionListener(this::btn11ActionPerformed);

        buttonGroup1.add(btn12);
        btn12.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn12.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn12.setText("M");
        btn12.addActionListener(this::btn12ActionPerformed);

        buttonGroup1.add(btn13);
        btn13.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn13.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn13.setText("N");
        btn13.addActionListener(this::btn13ActionPerformed);

        buttonGroup1.add(btn14);
        btn14.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn14.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn14.setText("O");
        btn14.addActionListener(this::btn14ActionPerformed);

        buttonGroup1.add(btn15);
        btn15.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        btn15.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/sf/emustudio/devices/mits88disk/gui/off.gif"))); // NOI18N
        btn15.setText("P");
        btn15.addActionListener(this::btn15ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn0)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn7))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btn8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn12)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn13)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn15)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn0)
                    .addComponent(btn1)
                    .addComponent(btn2)
                    .addComponent(btn3)
                    .addComponent(btn4)
                    .addComponent(btn5)
                    .addComponent(btn6)
                    .addComponent(btn7))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn8)
                    .addComponent(btn9)
                    .addComponent(btn10)
                    .addComponent(btn11)
                    .addComponent(btn12)
                    .addComponent(btn13)
                    .addComponent(btn14)
                    .addComponent(btn15))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Flags and settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", Font.BOLD, 11))); // NOI18N

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel1.setText("Port 1:");

        jLabel2.setFont(jLabel2.getFont().deriveFont(jLabel2.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel2.setText("Port 2:");

        lblPort1Status.setFont(new java.awt.Font("Monospaced", Font.BOLD, 12)); // NOI18N
        lblPort1Status.setText("11100111");

        lblPort2Status.setFont(new java.awt.Font("Monospaced", Font.BOLD, 12)); // NOI18N
        lblPort2Status.setText("00000000");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblPort2Status))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblPort1Status)))
                .addContainerGap(119, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lblPort1Status))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(lblPort2Status))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Mounted image", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", Font.BOLD, 11))); // NOI18N

        txtMountedImage.setEditable(false);
        txtMountedImage.setBackground(javax.swing.UIManager.getDefaults().getColor("TextField.disabledBackground"));
        txtMountedImage.setColumns(20);
        txtMountedImage.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        txtMountedImage.setLineWrap(true);
        txtMountedImage.setRows(5);
        jScrollPane1.setViewportView(txtMountedImage);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addContainerGap())
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Position", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", Font.BOLD, 11))); // NOI18N

        jLabel3.setFont(jLabel3.getFont().deriveFont(jLabel3.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel3.setText("Track:");

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel4.setText("Sector:");

        jLabel5.setFont(jLabel5.getFont().deriveFont(jLabel5.getFont().getStyle() & ~java.awt.Font.BOLD));
        jLabel5.setText("Offset:");

        lblOffset.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblOffset.setText("0");

        lblSector.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblSector.setText("0");

        lblTrack.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        lblTrack.setText("0");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblOffset)
                    .addComponent(lblTrack)
                    .addComponent(lblSector))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(lblTrack))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(lblSector))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(lblOffset))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn0ActionPerformed
        updateDriveInfo(drives.get(0));
    }//GEN-LAST:event_btn0ActionPerformed

    private void btn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn1ActionPerformed
        updateDriveInfo(drives.get(1));
    }//GEN-LAST:event_btn1ActionPerformed

    private void btn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn2ActionPerformed
        updateDriveInfo(drives.get(2));
    }//GEN-LAST:event_btn2ActionPerformed

    private void btn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn3ActionPerformed
        updateDriveInfo(drives.get(3));
    }//GEN-LAST:event_btn3ActionPerformed

    private void btn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn4ActionPerformed
        updateDriveInfo(drives.get(4));
    }//GEN-LAST:event_btn4ActionPerformed

    private void btn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn5ActionPerformed
        updateDriveInfo(drives.get(5));
    }//GEN-LAST:event_btn5ActionPerformed

    private void btn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn6ActionPerformed
        updateDriveInfo(drives.get(6));
    }//GEN-LAST:event_btn6ActionPerformed

    private void btn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn7ActionPerformed
        updateDriveInfo(drives.get(7));
    }//GEN-LAST:event_btn7ActionPerformed

    private void btn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn8ActionPerformed
        updateDriveInfo(drives.get(8));
    }//GEN-LAST:event_btn8ActionPerformed

    private void btn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn9ActionPerformed
        updateDriveInfo(drives.get(9));
    }//GEN-LAST:event_btn9ActionPerformed

    private void btn10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn10ActionPerformed
        updateDriveInfo(drives.get(10));
    }//GEN-LAST:event_btn10ActionPerformed

    private void btn11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn11ActionPerformed
        updateDriveInfo(drives.get(11));
    }//GEN-LAST:event_btn11ActionPerformed

    private void btn12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn12ActionPerformed
        updateDriveInfo(drives.get(12));
    }//GEN-LAST:event_btn12ActionPerformed

    private void btn13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn13ActionPerformed
        updateDriveInfo(drives.get(13));
    }//GEN-LAST:event_btn13ActionPerformed

    private void btn14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn14ActionPerformed
        updateDriveInfo(drives.get(14));
    }//GEN-LAST:event_btn14ActionPerformed

    private void btn15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn15ActionPerformed
        updateDriveInfo(drives.get(15));
    }//GEN-LAST:event_btn15ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private final javax.swing.JToggleButton btn0 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn1 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn10 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn11 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn12 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn13 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn14 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn15 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn2 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn3 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn4 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn5 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn6 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn7 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn8 = new javax.swing.JToggleButton();
    private final javax.swing.JToggleButton btn9 = new javax.swing.JToggleButton();
    private javax.swing.JLabel lblOffset;
    private javax.swing.JLabel lblPort1Status;
    private javax.swing.JLabel lblPort2Status;
    private javax.swing.JLabel lblSector;
    private javax.swing.JLabel lblTrack;
    private javax.swing.JTextArea txtMountedImage;
    // End of variables declaration//GEN-END:variables

    private final JToggleButton[] driveButtons = new JToggleButton[] {
            btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9, btn10, btn11, btn12, btn13, btn14, btn15
    };

}
