/*
 * DiskFrame.java
 *
 * Created on Streda, 2008, február 6, 8:01
 * 
 * KISS, YAGNI
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package disk_88.gui;

import disk_88.Drive;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import java.io.File;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class DiskFrame extends JFrame {
    private ArrayList<Drive> drives;
    private int driveInfoIndex = -1; // drive that wants to show current params
    
    /** Creates new form DiskFrame */
    public DiskFrame(ArrayList<Drive> drives) {
        initComponents();
        this.drives = drives;
        this.setLocationRelativeTo(null);
        
        Drive.DriveListener dl = new Drive.DriveListener() {
            public void driveSelect(Drive drive, boolean sel) {
                select(drive,sel);
            }
            public void driveParamsChanged(Drive drive) {
                updateDriveInfo(drive);
            }
        };
        for (int i = 0; i < drives.size(); i++) {
            Drive d = drives.get(i);
            d.removeAllListeners();
            d.addDriveListener(dl);
        }
    }

    private void updateDriveInfo(Drive d) {
        if (drives.indexOf(d) != driveInfoIndex) return;
        lblFlags.setText(Integer.toBinaryString(d.getFlags()) + "b");
        lblSector.setText(String.valueOf(d.getSector()));
        lblTrack.setText(String.valueOf(d.getTrack()));
        lblOffset.setText(String.valueOf(d.getOffset()));
        File f = d.getImageFile();
        if (f != null) txtImage.setText(f.getPath());
        else txtImage.setText("");
    }
    
    // calling from DiskImpl
    public void select(Drive dr, boolean sel) {
        String fil;
        int drive = drives.indexOf(dr);
        if (sel) fil = "/resources/on.gif";
        else fil = "/resources/off.gif";
        switch (drive) {
            case 0: btn0.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 1: btn1.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 2: btn2.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 3: btn3.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 4: btn3.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 5: btn5.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 6: btn6.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 7: btn7.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 8: btn8.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 9: btn9.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 10: btn10.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 11: btn11.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 12: btn12.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 13: btn13.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 14: btn14.setIcon(new ImageIcon(getClass().getResource(fil)));break;
            case 15: btn15.setIcon(new ImageIcon(getClass().getResource(fil)));break;
        }
    }

    private void initComponents() {
        drivesGroup = new ButtonGroup();
        panelSelectedDrives = new JPanel();
        btn0 = new JToggleButton("A");
        btn1 = new JToggleButton("B");
        btn2 = new JToggleButton("C");
        btn3 = new JToggleButton("D");
        btn4 = new JToggleButton("E");
        btn5 = new JToggleButton("F");
        btn6 = new JToggleButton("G");
        btn7 = new JToggleButton("H");
        btn8 = new JToggleButton("I");
        btn9 = new JToggleButton("J");
        btn10 = new JToggleButton("K");
        btn11 = new JToggleButton("L");
        btn12 = new JToggleButton("M");
        btn13 = new JToggleButton("N");
        btn14 = new JToggleButton("O");
        btn15 = new JToggleButton("P");
        driveInfoPanel = new JPanel();
        lblTrackLBL = new JLabel("Track:");
        lblSector = new JLabel("0");
        lblSectorLBL = new JLabel("Sector:");
        lblFlagsLBL = new JLabel("Flags:");
        lblOffsetLBL = new JLabel("Offset:");
        lblTrack = new JLabel("0");
        lblFlags = new JLabel("0");
        lblOffset = new JLabel("0");
        txtImage = new JTextArea();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MITS 88-DISK (floppy)");
        setResizable(false);

        panelSelectedDrives.setBorder(BorderFactory.createTitledBorder("Selected drives"));

        drivesGroup.add(btn0);
        btn0.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn0.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn0ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn1);
        btn1.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn1ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn2);
        btn2.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn2ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn3);
        btn3.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn3ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn4);
        btn4.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn4ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn5);
        btn5.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn5ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn6);
        btn6.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn6ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn7);
        btn7.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn7ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn8);
        btn8.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn8ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn9);
        btn9.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn9ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn10);
        btn10.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn10ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn11);
        btn11.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn11ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn12);
        btn12.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn12ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn13);
        btn13.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn13ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn14);
        btn14.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn14ActionPerformed(evt);
            }
        });

        drivesGroup.add(btn15);
        btn15.setIcon(new ImageIcon(getClass().getResource("/disk_88/resources/off.gif"))); // NOI18N
        btn15.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn15ActionPerformed(evt);
            }
        });

        GroupLayout panelDrivesLayout = new GroupLayout(panelSelectedDrives);
        panelSelectedDrives.setLayout(panelDrivesLayout);
        panelDrivesLayout.setHorizontalGroup(
        		panelDrivesLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(btn0,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        				.addComponent(btn8,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn1,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn9,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn2,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn10,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn3,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn11,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn4,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn12,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn5,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn13,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn6,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn14,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(btn7,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                		.addComponent(btn15,10, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap());
        panelDrivesLayout.setVerticalGroup(
        		panelDrivesLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(btn0)
        				.addComponent(btn1)
        				.addComponent(btn2)
        				.addComponent(btn3)
        				.addComponent(btn4)
        				.addComponent(btn5)
        				.addComponent(btn6)
        				.addComponent(btn7))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(panelDrivesLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(btn8)
        				.addComponent(btn9)
        				.addComponent(btn10)
        				.addComponent(btn11)
        				.addComponent(btn12)
        				.addComponent(btn13)
        				.addComponent(btn14)
        				.addComponent(btn15))
        		.addContainerGap());

        driveInfoPanel.setBorder(BorderFactory.createTitledBorder("Drive info"));
        lblSector.setFont(lblSector.getFont().deriveFont(lblSector.getFont().getStyle() | java.awt.Font.BOLD));
        lblTrack.setFont(lblTrack.getFont().deriveFont(lblTrack.getFont().getStyle() | java.awt.Font.BOLD));
        lblFlags.setFont(lblFlags.getFont().deriveFont(lblFlags.getFont().getStyle() | java.awt.Font.BOLD));
        lblOffset.setFont(lblOffset.getFont().deriveFont(lblOffset.getFont().getStyle() | java.awt.Font.BOLD));

        txtImage.setFont(txtImage.getFont().deriveFont(txtImage.getFont().getStyle() | java.awt.Font.BOLD));
        txtImage.setEditable(false);
        txtImage.setLineWrap(true);
        txtImage.setRows(3);
        txtImage.setWrapStyleWord(true);

        GroupLayout driveInfoPanelLayout = new GroupLayout(driveInfoPanel);
        driveInfoPanel.setLayout(driveInfoPanelLayout);
        driveInfoPanelLayout.setHorizontalGroup(
            driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(driveInfoPanelLayout.createSequentialGroup()
            	.addContainerGap()
	            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
	                .addComponent(lblFlagsLBL)
	                .addComponent(lblTrackLBL)
	                .addComponent(lblSectorLBL)
	                .addComponent(lblOffsetLBL))
	            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
	            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
	                .addComponent(lblFlags)
	                .addComponent(lblTrack)
	                .addComponent(lblSector)
	                .addComponent(lblOffset))
	            .addContainerGap())
	        .addGroup(driveInfoPanelLayout.createSequentialGroup()
	        		.addContainerGap()
	        		.addComponent(txtImage)
	        		.addContainerGap()));
        driveInfoPanelLayout.setVerticalGroup(
            driveInfoPanelLayout.createSequentialGroup()
            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblFlagsLBL)
                .addComponent(lblFlags))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblTrackLBL)
                .addComponent(lblTrack))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(lblSectorLBL)
                .addComponent(lblSector))
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(driveInfoPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblOffsetLBL)
                .addComponent(lblOffset))
            .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
            .addComponent(txtImage)
            .addContainerGap());

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(panelSelectedDrives, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(driveInfoPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addContainerGap());
        layout.setVerticalGroup(
            layout.createSequentialGroup()
            .addContainerGap()
            .addComponent(panelSelectedDrives, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(driveInfoPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
            .addContainerGap()
        );
        pack();
    }

    private void btn0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn0ActionPerformed
        driveInfoIndex = 0;
        updateDriveInfo((Drive)drives.get(0));
    }//GEN-LAST:event_btn0ActionPerformed

    private void btn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn1ActionPerformed
        driveInfoIndex = 1;
        updateDriveInfo((Drive)drives.get(1));
    }//GEN-LAST:event_btn1ActionPerformed

    private void btn2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn2ActionPerformed
        driveInfoIndex = 2;
        updateDriveInfo((Drive)drives.get(2));
    }//GEN-LAST:event_btn2ActionPerformed

    private void btn3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn3ActionPerformed
        driveInfoIndex = 3;
        updateDriveInfo((Drive)drives.get(3));
    }//GEN-LAST:event_btn3ActionPerformed

    private void btn4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn4ActionPerformed
        driveInfoIndex = 4;
        updateDriveInfo((Drive)drives.get(4));
    }//GEN-LAST:event_btn4ActionPerformed

    private void btn5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn5ActionPerformed
        driveInfoIndex = 5;
        updateDriveInfo((Drive)drives.get(5));
    }//GEN-LAST:event_btn5ActionPerformed

    private void btn6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn6ActionPerformed
        driveInfoIndex = 6;
        updateDriveInfo((Drive)drives.get(6));
    }//GEN-LAST:event_btn6ActionPerformed

    private void btn7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn7ActionPerformed
        driveInfoIndex = 7;
        updateDriveInfo((Drive)drives.get(7));
    }//GEN-LAST:event_btn7ActionPerformed

    private void btn8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn8ActionPerformed
        driveInfoIndex = 8;
        updateDriveInfo((Drive)drives.get(8));
    }//GEN-LAST:event_btn8ActionPerformed

    private void btn9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn9ActionPerformed
        driveInfoIndex = 9;
        updateDriveInfo((Drive)drives.get(9));
    }//GEN-LAST:event_btn9ActionPerformed

    private void btn10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn10ActionPerformed
        driveInfoIndex = 10;
        updateDriveInfo((Drive)drives.get(10));
    }//GEN-LAST:event_btn10ActionPerformed

    private void btn11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn11ActionPerformed
        driveInfoIndex = 11;
        updateDriveInfo((Drive)drives.get(11));
    }//GEN-LAST:event_btn11ActionPerformed

    private void btn12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn12ActionPerformed
        driveInfoIndex = 12;
        updateDriveInfo((Drive)drives.get(12));
    }//GEN-LAST:event_btn12ActionPerformed

    private void btn13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn13ActionPerformed
        driveInfoIndex = 13;
        updateDriveInfo((Drive)drives.get(13));
    }//GEN-LAST:event_btn13ActionPerformed

    private void btn14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn14ActionPerformed
        driveInfoIndex = 14;
        updateDriveInfo((Drive)drives.get(14));
    }//GEN-LAST:event_btn14ActionPerformed

    private void btn15ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn15ActionPerformed
        driveInfoIndex = 15;
        updateDriveInfo((Drive)drives.get(15));
    }//GEN-LAST:event_btn15ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JToggleButton btn0;
    private JToggleButton btn1;
    private JToggleButton btn10;
    private JToggleButton btn11;
    private JToggleButton btn12;
    private JToggleButton btn13;
    private JToggleButton btn14;
    private JToggleButton btn15;
    private JToggleButton btn2;
    private JToggleButton btn3;
    private JToggleButton btn4;
    private JToggleButton btn5;
    private JToggleButton btn6;
    private JToggleButton btn7;
    private JToggleButton btn8;
    private JToggleButton btn9;
    private JPanel driveInfoPanel;
    private ButtonGroup drivesGroup;
    private JLabel lblTrackLBL;
    private JLabel lblSectorLBL;
    private JLabel lblFlagsLBL;
    private JLabel lblOffsetLBL;
    private JPanel panelSelectedDrives;
    private JLabel lblFlags;
    private JLabel lblOffset;
    private JLabel lblSector;
    private JLabel lblTrack;
    private JTextArea txtImage;
}
