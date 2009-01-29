/*
 * SIODialog.java
 *
 * Created on Nedeľa, 2008, júl 27, 19:52
 * 
 * KISS, YAGNI
 */

package sio88.gui;

import javax.swing.GroupLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class SIODialog extends JDialog {
    /** Creates new form SIODialog */
    public SIODialog(java.awt.Frame parent, boolean modal, String devName, 
    		int port1CPU, int port2CPU) {
        super(parent, modal);
        initComponents();
        lblDev.setText(devName);
        lblPort1.setText(String.format("0x%X", port1CPU));
        lblPort2.setText(String.format("0x%X", port2CPU));
        this.setLocationRelativeTo(parent);
    }

    private void initComponents() {
        JLabel lblAttached = new JLabel("Attached device:");;
        JLabel lblPort1LBL = new JLabel("CPU port1: ");;
        JLabel lblPort2LBL = new JLabel("CPU port2: ");;

        lblDev = new JLabel("none");
        lblPort1 = new JLabel("0x10");
        lblPort2 = new JLabel("0x11");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("88-SIO run-time");

        lblPort1.setFont(lblPort1.getFont().deriveFont(lblPort1.getFont().getStyle() | java.awt.Font.BOLD));
        lblPort2.setFont(lblPort2.getFont().deriveFont(lblPort2.getFont().getStyle() | java.awt.Font.BOLD));
        lblDev.setFont(lblDev.getFont().deriveFont(lblDev.getFont().getStyle() | java.awt.Font.BOLD));

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            		.addGroup(layout.createSequentialGroup()
            				.addComponent(lblAttached)
            				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            				.addComponent(lblDev))
            		.addGroup(layout.createSequentialGroup()
            				.addComponent(lblPort1LBL)
            				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            				.addComponent(lblPort1))
            		.addGroup(layout.createSequentialGroup()
            				.addComponent(lblPort2LBL)
            				.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
            				.addComponent(lblPort2)))
            .addContainerGap());
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblAttached)
                    .addComponent(lblDev))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPort1LBL)
                    .addComponent(lblPort1))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPort2LBL)
                    .addComponent(lblPort2))
                .addContainerGap());
        pack();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private JLabel lblDev;
    private JLabel lblPort1;
    private JLabel lblPort2;

    // End of variables declaration//GEN-END:variables

}
