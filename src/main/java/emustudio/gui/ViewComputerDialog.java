/**
 * ViewArchDialog.java
 *
 * Created on Utorok, 2007, september 11, 15:42
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
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

import emulib.plugins.compiler.ICompiler;
import emulib.plugins.cpu.ICPU;
import emulib.plugins.device.IDevice;
import emulib.plugins.memory.IMemory;
import emulib.runtime.StaticDialogs;
import emustudio.architecture.ArchHandler;
import emustudio.architecture.Computer;
import emustudio.architecture.drawing.PreviewPanel;
import emustudio.main.Main;
import java.awt.event.*;
import javax.swing.*;

/**
 *
 * @author vbmacher
 */
@SuppressWarnings("serial")
public class ViewComputerDialog extends JDialog {

    private boolean easterClicked = false; // for easterEgg
    private ArchHandler arch;
    private String compilerName;
    private String cpuName;
    private String memoryName;
    private PreviewPanel pan;

    /**
     * Creates an instance of this dialog
     *
     * @param parent parent frame
     * @param modal whether this dialog should be modal
     */
    public ViewComputerDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        arch = Main.currentArch;
        initComponents();

        compilerName = arch.getCompilerName();
        memoryName = arch.getMemoryName();
        cpuName = arch.getCPUName();

        try {
            lblName.setText(arch.getComputerName());

            ICompiler compiler = arch.getComputer().getCompiler();
            if (compiler == null) {
                lblCompilerFileName.setText("Compiler is not used");
                lblCompilerName.setText("");
                lblCompilerVersion.setText("");
                txtCompilerCopyright.setText("");
                txtCompilerDescription.setText("");
            } else {
                lblCompilerFileName.setText(compilerName + ".jar");
                lblCompilerName.setText(compiler.getTitle());
                lblCompilerVersion.setText(compiler.getVersion());
                txtCompilerCopyright.setText(compiler.getCopyright());
                txtCompilerDescription.setText(compiler.getDescription());
            }

            ICPU cpu = arch.getComputer().getCPU();

            lblCPUFileName.setText(cpuName + ".jar");
            lblCPUName.setText(cpu.getTitle());
            lblCPUVersion.setText(cpu.getVersion());
            txtCPUCopyright.setText(cpu.getCopyright());
            txtCPUDescription.setText(cpu.getDescription());

            lblMemoryFileName.setText(memoryName + ".jar");

            IMemory memory = arch.getComputer().getMemory();

            if (memory != null) {
                lblMemoryName.setText(memory.getTitle());
                lblMemoryVersion.setText(memory.getVersion());
                txtMemoryCopyright.setText(memory.getCopyright());
                txtMemoryDescription.setText(memory.getDescription());
            }

            int j = arch.getComputer().getDeviceCount();
            Computer comp = arch.getComputer();
            for (int i = 0; i < j; i++) {
                cmbDevice.addItem(comp.getDevice(i).getTitle());
            }
        } catch (NullPointerException e) {
            StaticDialogs.showErrorMessage("Error: Can't get plug-ins information\n\n"
                    + e.getMessage());
        }

        if (cmbDevice.getItemCount() > 0) {
            cmbDevice.setSelectedIndex(0);
            showDevice(0);
        } else {
            cmbDevice.setEnabled(false);
        }

        cmbDevice.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                int i = cmbDevice.getSelectedIndex();
                try {
                    showDevice(i);
                } catch (Exception ex) {
                }
            }
        });
        pan = new PreviewPanel(arch.getSchema());
        scrollScheme.setViewportView(pan);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        this.setLocationRelativeTo(null);
    }

    /**
     * Shows i-th device from the combo box.
     *
     * @param i the device index to show
     */
    private void showDevice(int i) {
        IDevice device = arch.getComputer().getDevice(i);
        lblDeviceFileName.setText(arch.getDeviceName(i) + ".jar");
        lblDeviceName.setText(device.getTitle());
        lblDeviceVersion.setText(device.getVersion());
        txtDeviceCopyright.setText(device.getCopyright());
        txtDeviceDescription.setText(device.getDescription());
    }

    private void initComponents() {
        lblName = new JLabel();
        JTabbedPane tabbedPane = new JTabbedPane();
        final JPanel panelCompiler = new JPanel();
        JLabel lblFileNameLBL1 = new JLabel();
        JLabel lblPluginNameLBL1 = new JLabel();
        JLabel lblVersionLBL1 = new JLabel();
        JLabel lblDescriptionLBL1 = new JLabel();
        lblCompilerVersion = new JLabel();
        lblCompilerName = new JLabel();
        JLabel lblCopyrightLBL1 = new JLabel();
        txtCompilerCopyright = new JTextArea();
        lblCompilerFileName = new JLabel();
        JScrollPane scrollCompilerDescription = new JScrollPane();
        txtCompilerDescription = new JTextArea();
        JPanel panelCPU = new JPanel();
        JLabel lblFileNameLBL2 = new JLabel();
        JLabel lblPluginNameLBL2 = new JLabel();
        JLabel lblVersionLBL2 = new JLabel();
        JLabel lblDescriptionLBL2 = new JLabel();
        lblCPUVersion = new JLabel();
        lblCPUName = new JLabel();
        JLabel lblCopyrightLBL2 = new JLabel();
        txtCPUCopyright = new JTextArea();
        lblCPUFileName = new JLabel();
        JScrollPane scrollCpuDescription = new JScrollPane();
        txtCPUDescription = new JTextArea();
        JPanel panelMemory = new JPanel();
        JLabel lblFileNameLBL3 = new JLabel();
        JLabel lblPluginNameLBL3 = new JLabel();
        JLabel lblVersionLBL3 = new JLabel();
        JLabel lblDescriptionLBL3 = new JLabel();
        lblMemoryFileName = new JLabel();
        lblMemoryName = new JLabel();
        JLabel lblCopyrightLBL3 = new JLabel();
        txtMemoryCopyright = new JTextArea();
        lblMemoryVersion = new JLabel();
        JScrollPane scrollMemoryDescription = new JScrollPane();
        txtMemoryDescription = new JTextArea();
        JPanel panelDevices = new JPanel();
        JLabel lblFileNameLBL4 = new JLabel();
        JLabel lblPluginNameLBL4 = new JLabel();
        JLabel lblVersionLBL4 = new JLabel();
        JLabel lblDescriptionLBL4 = new JLabel();
        lblDeviceFileName = new JLabel();
        lblDeviceName = new JLabel();
        JLabel lblCopyrightLBL4 = new JLabel();
        txtDeviceCopyright = new JTextArea();
        lblDeviceVersion = new JLabel();
        JScrollPane scrollDeviceDescription = new JScrollPane();
        txtDeviceDescription = new JTextArea();
        JLabel lblDeviceLBL = new JLabel();
        cmbDevice = new JComboBox();
        JPanel panelScheme = new JPanel();
        scrollScheme = new JScrollPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("View computer");
        setAlwaysOnTop(true);

        lblName.setFont(lblName.getFont().deriveFont(lblName.getFont().getStyle() | java.awt.Font.BOLD));
        lblName.setText(null);
        lblName.addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
                    easterClicked = !easterClicked;
                    if (easterClicked) {
                        panelCompiler.grabFocus();
                    }
                }
                e.consume();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });

        tabbedPane.setFocusable(false);

        lblFileNameLBL1.setText("File name:");
        lblPluginNameLBL1.setText("Plugin name:");
        lblVersionLBL1.setText("Version:");
        lblDescriptionLBL1.setText("Description:");
        lblCopyrightLBL1.setText("Copyright:");

        lblCompilerVersion.setFont(lblCompilerVersion.getFont().deriveFont(lblCompilerVersion.getFont().getStyle() | java.awt.Font.BOLD));
        lblCompilerName.setFont(lblCompilerName.getFont().deriveFont(lblCompilerName.getFont().getStyle() | java.awt.Font.BOLD));
        lblCompilerFileName.setFont(lblCompilerFileName.getFont().deriveFont(lblCompilerFileName.getFont().getStyle() | java.awt.Font.BOLD));

        txtCompilerCopyright.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtCompilerCopyright.setEditable(false);
        txtCompilerCopyright.setLineWrap(true);
        txtCompilerCopyright.setRows(3);
        txtCompilerCopyright.setWrapStyleWord(true);

        txtCompilerDescription.setEditable(false);
        txtCompilerDescription.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtCompilerDescription.setLineWrap(true);
        txtCompilerDescription.setRows(5);
        txtCompilerDescription.setWrapStyleWord(true);
        txtCompilerDescription.setOpaque(false);
        scrollCompilerDescription.setViewportView(txtCompilerDescription);

        panelCompiler.addKeyListener(new KeyListener() {

            @Override
            public void keyPressed(KeyEvent e) {
                if (easterClicked && e.isAltDown() && (e.getKeyCode() == KeyEvent.VK_A)) {
                    StaticDialogs.showMessage("Easter egg: Welcome, vbmacher!");
                }
                e.consume();
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }
        });
        GroupLayout compilerLayout = new GroupLayout(panelCompiler);
        panelCompiler.setLayout(compilerLayout);

        compilerLayout.setHorizontalGroup(compilerLayout.createSequentialGroup().addContainerGap().addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblFileNameLBL1).addComponent(lblPluginNameLBL1).addComponent(lblVersionLBL1).addComponent(lblCopyrightLBL1).addComponent(lblDescriptionLBL1)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblCompilerFileName).addComponent(lblCompilerName).addComponent(lblCompilerVersion).addComponent(txtCompilerCopyright).addComponent(scrollCompilerDescription)).addContainerGap());
        compilerLayout.setVerticalGroup(
                compilerLayout.createSequentialGroup().addContainerGap().addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblFileNameLBL1).addComponent(lblCompilerFileName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblPluginNameLBL1).addComponent(lblCompilerName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblVersionLBL1).addComponent(lblCompilerVersion)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCopyrightLBL1).addComponent(txtCompilerCopyright)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDescriptionLBL1).addComponent(scrollCompilerDescription)).addContainerGap());

        tabbedPane.addTab("Compiler", panelCompiler);
        lblFileNameLBL2.setText("File name:");
        lblPluginNameLBL2.setText("Plugin name:");
        lblVersionLBL2.setText("Version:");
        lblDescriptionLBL2.setText("Description:");
        lblCopyrightLBL2.setText("Copyright:");

        lblCPUVersion.setFont(lblCPUVersion.getFont().deriveFont(lblCPUVersion.getFont().getStyle() | java.awt.Font.BOLD));
        lblCPUName.setFont(lblCPUName.getFont().deriveFont(lblCPUName.getFont().getStyle() | java.awt.Font.BOLD));
        lblCPUFileName.setFont(lblCPUFileName.getFont().deriveFont(lblCPUFileName.getFont().getStyle() | java.awt.Font.BOLD));

        txtCPUCopyright.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtCPUCopyright.setEditable(false);
        txtCPUCopyright.setLineWrap(true);
        txtCPUCopyright.setRows(3);
        txtCPUCopyright.setWrapStyleWord(true);

        txtCPUDescription.setEditable(false);
        txtCPUDescription.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtCPUDescription.setLineWrap(true);
        txtCPUDescription.setRows(5);
        txtCPUDescription.setWrapStyleWord(true);
        txtCPUDescription.setOpaque(false);
        scrollCpuDescription.setViewportView(txtCPUDescription);

        GroupLayout cpuLayout = new GroupLayout(panelCPU);
        panelCPU.setLayout(cpuLayout);
        cpuLayout.setHorizontalGroup(cpuLayout.createSequentialGroup().addContainerGap().addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblFileNameLBL2).addComponent(lblPluginNameLBL2).addComponent(lblVersionLBL2).addComponent(lblCopyrightLBL2).addComponent(lblDescriptionLBL2)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblCPUFileName).addComponent(lblCPUName).addComponent(lblCPUVersion).addComponent(txtCPUCopyright).addComponent(scrollCpuDescription)).addContainerGap());
        cpuLayout.setVerticalGroup(
                cpuLayout.createSequentialGroup().addContainerGap().addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblFileNameLBL2).addComponent(lblCPUFileName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblPluginNameLBL2).addComponent(lblCPUName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblVersionLBL2).addComponent(lblCPUVersion)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCopyrightLBL2).addComponent(txtCPUCopyright)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDescriptionLBL2).addComponent(scrollCpuDescription)).addContainerGap());

        tabbedPane.addTab("CPU", panelCPU);
        lblFileNameLBL3.setText("File name:");
        lblPluginNameLBL3.setText("Plugin name:");
        lblVersionLBL3.setText("Version:");
        lblDescriptionLBL3.setText("Description:");
        lblCopyrightLBL3.setText("Copyright:");

        lblMemoryFileName.setFont(lblMemoryFileName.getFont().deriveFont(lblMemoryFileName.getFont().getStyle() | java.awt.Font.BOLD));
        lblMemoryName.setFont(lblMemoryName.getFont().deriveFont(lblMemoryName.getFont().getStyle() | java.awt.Font.BOLD));
        lblMemoryVersion.setFont(lblMemoryVersion.getFont().deriveFont(lblMemoryVersion.getFont().getStyle() | java.awt.Font.BOLD));

        txtMemoryCopyright.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtMemoryCopyright.setEditable(false);
        txtMemoryCopyright.setLineWrap(true);
        txtMemoryCopyright.setRows(3);
        txtMemoryCopyright.setWrapStyleWord(true);

        txtMemoryDescription.setEditable(false);
        txtMemoryDescription.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtMemoryDescription.setLineWrap(true);
        txtMemoryDescription.setRows(5);
        txtMemoryDescription.setWrapStyleWord(true);
        txtMemoryDescription.setOpaque(false);
        scrollMemoryDescription.setViewportView(txtMemoryDescription);

        GroupLayout memoryLayout = new GroupLayout(panelMemory);
        panelMemory.setLayout(memoryLayout);
        memoryLayout.setHorizontalGroup(memoryLayout.createSequentialGroup().addContainerGap().addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblFileNameLBL3).addComponent(lblPluginNameLBL3).addComponent(lblVersionLBL3).addComponent(lblCopyrightLBL3).addComponent(lblDescriptionLBL3)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblMemoryFileName).addComponent(lblMemoryName).addComponent(lblMemoryVersion).addComponent(txtMemoryCopyright).addComponent(scrollMemoryDescription)).addContainerGap());
        memoryLayout.setVerticalGroup(
                memoryLayout.createSequentialGroup().addContainerGap().addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblFileNameLBL3).addComponent(lblMemoryFileName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblPluginNameLBL3).addComponent(lblMemoryName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblVersionLBL3).addComponent(lblMemoryVersion)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCopyrightLBL3).addComponent(txtMemoryCopyright)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDescriptionLBL3).addComponent(scrollMemoryDescription)).addContainerGap());

        tabbedPane.addTab("Memory", panelMemory);
        lblFileNameLBL4.setText("File name:");
        lblPluginNameLBL4.setText("Plugin name:");
        lblVersionLBL4.setText("Version:");
        lblDescriptionLBL4.setText("Description:");
        lblCopyrightLBL4.setText("Copyright:");

        lblDeviceFileName.setFont(lblDeviceFileName.getFont().deriveFont(lblDeviceFileName.getFont().getStyle() | java.awt.Font.BOLD));
        lblDeviceName.setFont(lblDeviceName.getFont().deriveFont(lblDeviceName.getFont().getStyle() | java.awt.Font.BOLD));
        lblDeviceVersion.setFont(lblDeviceVersion.getFont().deriveFont(lblDeviceVersion.getFont().getStyle() | java.awt.Font.BOLD));

        txtDeviceCopyright.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtDeviceCopyright.setEditable(false);
        txtDeviceCopyright.setLineWrap(true);
        txtDeviceCopyright.setRows(3);
        txtDeviceCopyright.setWrapStyleWord(true);

        txtDeviceDescription.setEditable(false);
        txtDeviceDescription.setFont(new java.awt.Font("Monospaced", 0, 12));
        txtDeviceDescription.setLineWrap(true);
        txtDeviceDescription.setRows(5);
        txtDeviceDescription.setWrapStyleWord(true);
        txtDeviceDescription.setOpaque(false);
        scrollDeviceDescription.setViewportView(txtDeviceDescription);

        lblDeviceLBL.setText("Device:");

        GroupLayout deviceLayout = new GroupLayout(panelDevices);
        panelDevices.setLayout(deviceLayout);

        deviceLayout.setHorizontalGroup(deviceLayout.createSequentialGroup().addContainerGap().addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(lblDeviceLBL).addComponent(lblFileNameLBL4).addComponent(lblPluginNameLBL4).addComponent(lblVersionLBL4).addComponent(lblCopyrightLBL4).addComponent(lblDescriptionLBL4)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(cmbDevice).addComponent(lblDeviceFileName).addComponent(lblDeviceName).addComponent(lblDeviceVersion).addComponent(txtDeviceCopyright).addComponent(scrollDeviceDescription)).addContainerGap());
        deviceLayout.setVerticalGroup(
                deviceLayout.createSequentialGroup().addContainerGap().addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDeviceLBL).addComponent(cmbDevice)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblFileNameLBL4).addComponent(lblDeviceFileName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblPluginNameLBL4).addComponent(lblDeviceName)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblVersionLBL4).addComponent(lblDeviceVersion)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblCopyrightLBL4).addComponent(txtDeviceCopyright)).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(lblDescriptionLBL4).addComponent(scrollDeviceDescription)).addContainerGap());

        tabbedPane.addTab("Devices", panelDevices);

        GroupLayout schemeLayout = new GroupLayout(panelScheme);
        panelScheme.setLayout(schemeLayout);
        schemeLayout.setHorizontalGroup(
                schemeLayout.createSequentialGroup().addContainerGap().addComponent(scrollScheme) //, GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap());
        schemeLayout.setVerticalGroup(
                schemeLayout.createSequentialGroup().addContainerGap().addComponent(scrollScheme) //, GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addContainerGap());

        tabbedPane.addTab("Abstract schema", panelScheme);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup().addContainerGap().addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING).addComponent(tabbedPane, 100, 500, Short.MAX_VALUE).addComponent(lblName)).addContainerGap());
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING).addGroup(layout.createSequentialGroup().addContainerGap().addComponent(lblName).addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE).addContainerGap()));

        pack();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    JComboBox cmbDevice;
    JTextArea txtCPUCopyright;
    JLabel lblCPUFileName;
    JLabel lblCPUName;
    JLabel lblCPUVersion;
    JTextArea txtCompilerCopyright;
    JLabel lblCompilerFileName;
    JLabel lblCompilerName;
    JLabel lblCompilerVersion;
    JTextArea txtDeviceCopyright;
    JLabel lblDeviceFileName;
    JLabel lblDeviceName;
    JLabel lblDeviceVersion;
    JTextArea txtMemoryCopyright;
    JLabel lblMemoryFileName;
    JLabel lblMemoryName;
    JLabel lblMemoryVersion;
    JLabel lblName;
    JScrollPane scrollScheme;
    JTextArea txtCPUDescription;
    JTextArea txtCompilerDescription;
    JTextArea txtDeviceDescription;
    JTextArea txtMemoryDescription;
    // End of variables declaration//GEN-END:variables
}
