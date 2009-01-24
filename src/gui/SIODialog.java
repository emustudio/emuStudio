/*
 * SIODialog.java
 *
 * Created on Nedeľa, 2008, júl 27, 19:52
 * 
 * KISS, YAGNI
 */

package gui;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class SIODialog extends javax.swing.JDialog {

    /** Creates new form SIODialog */
    public SIODialog(java.awt.Frame parent, boolean modal, String devName) {
        super(parent, modal);
        initComponents();
        lblDev.setText(devName);
        this.setLocationRelativeTo(parent);
    }

    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        lblDev = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Virtual 88-SIO serial card plugin");

        jLabel1.setText("Attached device:");

        lblDev.setFont(lblDev.getFont().deriveFont(lblDev.getFont().getStyle() | java.awt.Font.BOLD));
        lblDev.setText("none");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblDev)
                .addContainerGap(129, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(lblDev))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel lblDev;
    // End of variables declaration//GEN-END:variables

}
