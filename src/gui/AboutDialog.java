/*
 * AboutDialog.java
 *
 * Created on Pondelok, 2008, február 4, 16:53
 */

package gui;

import java.awt.Font;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class AboutDialog extends javax.swing.JDialog {
    
    /** Creates new form AboutDialog */
    public AboutDialog(java.awt.Frame parent, boolean modal) {
        super(parent, modal);
        initComponents();
        this.setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        JLabel lblIcon = new JLabel();
        JLabel lblTitle = new JLabel();
        JLabel lblDescription = new JLabel();
        JLabel lblConsultantLBL = new JLabel();
        JLabel lblVersionLBL = new JLabel();
        JLabel lblVersion = new JLabel();
        JLabel lblConsultant = new JLabel();
        JLabel lblCopyright = new JLabel();
        JLabel lblLicense = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("About");
        
        lblIcon.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/altair.jpg"))); // NOI18N

        lblTitle.setFont(lblTitle.getFont().deriveFont(Font.BOLD,24));
        lblTitle.setText("emuStudio");

        lblDescription.setText("<html>Emulation platform for emulating various kinds of mainly older (e.g. 8-bit) architectures. It was designed for educational purposes. It is a property of Technical University of Košice.");

        lblConsultantLBL.setFont(lblConsultantLBL.getFont().deriveFont(lblConsultantLBL.getFont().getStyle() | Font.BOLD));
        lblConsultantLBL.setText("Consultant:");

        lblVersionLBL.setFont(lblVersionLBL.getFont().deriveFont(lblVersionLBL.getFont().getStyle() | Font.BOLD));
        lblVersionLBL.setText("Version:");

        lblVersion.setText("0.32b1");
        lblConsultant.setText("ing. Slavomir Šimoňák");

        lblCopyright.setFont(lblCopyright.getFont().deriveFont(lblCopyright.getFont().getStyle() | Font.BOLD));
        lblCopyright.setText("© Copyright 2006-2008, Peter Jakubčo");

        lblLicense.setText("<html>Full license agreement is written in file <strong>license.txt</strong>");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
        		layout.createSequentialGroup()
        			.addContainerGap()
        			.addComponent(lblIcon)
        			.addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
        			.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        					.addComponent(lblTitle)
        					.addComponent(lblDescription,GroupLayout.DEFAULT_SIZE,347, Short.MAX_VALUE)
        					.addComponent(lblCopyright)
        					.addGroup(layout.createSequentialGroup()
        							.addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        									.addComponent(lblVersionLBL)
        									.addComponent(lblConsultantLBL)
        									)
        							.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        						    .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        						    		.addComponent(lblVersion)
        						    		.addComponent(lblConsultant)
        						    		))
        				    .addComponent(lblLicense)
        					)
        			.addContainerGap()
        		);
        layout.setVerticalGroup(
        		layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        			.addComponent(lblIcon)
        			.addGroup(layout.createSequentialGroup()
        					.addComponent(lblTitle)
        					.addGap(10)
        					.addComponent(lblDescription)
        					.addGap(10)
        					.addComponent(lblCopyright)
        					.addGap(10)
        					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        							.addComponent(lblVersionLBL)
        							.addComponent(lblVersion))
        					.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        							.addComponent(lblConsultantLBL)
        							.addComponent(lblConsultant))
        					.addGap(20)
        				    .addComponent(lblLicense)
        					)
        		);
        pack();
    }
    
}
