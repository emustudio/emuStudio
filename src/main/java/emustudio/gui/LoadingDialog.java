/*
 * KISS, YAGNI, DRY
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
package emustudio.gui;

import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

@SuppressWarnings("serial")
public class LoadingDialog extends JDialog {

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
        setTitle("emuStudio");

        lblLoading.setFont(lblLoading.getFont().deriveFont(lblLoading.getFont().getStyle() | java.awt.Font.BOLD));
        lblLoading.setIcon(new ImageIcon(getClass()
                .getResource("/emustudio/gui/loading.gif"))); // NOI18N
        lblLoading.setText("Loading computer, please wait...");

        lblWarning.setFont(lblWarning.getFont().deriveFont(java.awt.Font.PLAIN));
        lblWarning.setText("If you see some errors, please look at the emustudio.log.");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(lblWarning, GroupLayout.PREFERRED_SIZE, 338,
                GroupLayout.PREFERRED_SIZE).addComponent(lblLoading))
                .addContainerGap());
        layout.setVerticalGroup(
                layout.createSequentialGroup().addContainerGap()
                .addComponent(lblLoading).addPreferredGap(LayoutStyle
                .ComponentPlacement.UNRELATED).addComponent(lblWarning)
                .addContainerGap(lblWarning.getPreferredSize().height,
                lblWarning.getPreferredSize().height).addContainerGap());

        pack();
    }
}
