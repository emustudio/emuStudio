/*
 * LoadingDialog.java
 *
 * Created on Utorok, 2008, september 16, 15:55
 */

package gui;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class LoadingDialog extends JDialog {

    /** Creates new form LoadingDialog */
    public LoadingDialog() {
        super();
        initComponents();
        this.setLocationRelativeTo(null);
    }

    private void initComponents() {

        JLabel lblLoading = new JLabel();
        JLabel lblWarning = new JLabel();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setResizable(false);

        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setIcon(new ImageIcon(getClass().getResource("/resources/emuStudio/motherboard-icon.gif"))); // NOI18N
        lblLoading.setText("Loading architecture, please wait...");

        lblWarning.setText("<html>If you see some errors during the loading, check your abstract scheme or plugins.");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
            .addContainerGap()
            .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblWarning, GroupLayout.PREFERRED_SIZE, 338, GroupLayout.PREFERRED_SIZE)
                .addComponent(lblLoading))
            .addContainerGap());
        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblLoading)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblWarning)
                .addContainerGap()
        );

        pack();
    }

}
