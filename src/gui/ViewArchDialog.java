/*
 * ViewArchDialog.java
 *
 * Created on Utorok, 2007, september 11, 15:42
 * 
 * KISS,YAGNI
 */

package gui;

import architecture.ArchHandler;
import architecture.Main;
import architecture.drawing.PreviewPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.GroupLayout;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle;
import javax.swing.WindowConstants;

import runtime.StaticDialogs;

/**
 *
 * @author  vbmacher
 */
@SuppressWarnings("serial")
public class ViewArchDialog extends JDialog {
	private boolean easterClicked = false; // for easterEgg 
    private ArchHandler arch;
    private String compilerName;
    private String cpuName;
    private String memoryName;
    private Vector<String> devNames;
    private PreviewPanel pan;

    /** Creates new form ViewArchDialog */
    public ViewArchDialog(JFrame parent, boolean modal) {
        super(parent, modal);
        arch = Main.currentArch;
        initComponents();
        
        compilerName = arch.getCompilerName();
        memoryName = arch.getMemoryName();
        cpuName = arch.getCPUName();
        
        devNames = new Vector<String>();
        for (int i = 0; i < arch.getDevices().length; i++)
            devNames.add(arch.getDeviceName(i));
        
        try {
            lblName.setText(arch.getArchName());
            lblCompilerFileName.setText(compilerName+".jar");
            lblCompilerName.setText(arch.getCompiler().getTitle());
            lblCompilerVersion.setText(arch.getCompiler().getVersion());
            txtCompilerCopyright.setText(arch.getCompiler().getCopyright());
            txtCompilerDescription.setText(arch.getCompiler().getDescription());
            
            lblCPUFileName.setText(cpuName+".jar");
            lblCPUName.setText(arch.getCPU().getTitle());
            lblCPUVersion.setText(arch.getCPU().getVersion());
            txtCPUCopyright.setText(arch.getCPU().getCopyright());
            txtCPUDescription.setText(arch.getCPU().getDescription());
            
            lblMemoryFileName.setText(memoryName+".jar");
            lblMemoryName.setText(arch.getMemory().getTitle());
            lblMemoryVersion.setText(arch.getMemory().getVersion());
            txtMemoryCopyright.setText(arch.getMemory().getCopyright());
            txtMemoryDescription.setText(arch.getMemory().getDescription());

            for (int i = 0; i < devNames.size(); i++)
                cmbDevice.addItem(devNames.get(i));
        }
        catch (NullPointerException e) {
            StaticDialogs.showErrorMessage("Can't get plugins info:" + e.getMessage());
        }
        
        if (cmbDevice.getItemCount() > 0) showDevConfig(0);
        cmbDevice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int i = cmbDevice.getSelectedIndex();
                try { showDevConfig(i); }
                catch(Exception ex) {}
            }
        });
        pan = new PreviewPanel(arch.getSchema());
        scrollScheme.setViewportView(pan);
        scrollScheme.getHorizontalScrollBar().setUnitIncrement(10);
        scrollScheme.getVerticalScrollBar().setUnitIncrement(10);
        this.setLocationRelativeTo(null);
    }
    
    private void showDevConfig(int i) {
        lblDeviceFileName.setText(devNames.get(i) +".jar");
        lblDeviceName.setText(arch.getDevices()[i].getTitle());
        lblDeviceVersion.setText(arch.getDevices()[i].getVersion());
        txtDeviceCopyright.setText(arch.getDevices()[i].getCopyright());
        txtDeviceDescription.setText(arch.getDevices()[i].getDescription());
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
        setTitle("View current configuration");
        setAlwaysOnTop(true);

        lblName.setFont(lblName.getFont().deriveFont(lblName.getFont().getStyle() | java.awt.Font.BOLD));
        lblName.setText(null);
        lblName.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if ((e.getButton() == MouseEvent.BUTTON1) && (e.getClickCount() == 2)) {
					easterClicked = !easterClicked;
					if (easterClicked)
						panelCompiler.grabFocus();
				}
				e.consume();
			}
			@Override
			public void mouseEntered(MouseEvent e) {}
			@Override
			public void mouseExited(MouseEvent e) {}
			@Override
			public void mousePressed(MouseEvent e) {}
			@Override
			public void mouseReleased(MouseEvent e) {}
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
			public void keyReleased(KeyEvent e) {}
			@Override
			public void keyTyped(KeyEvent e) {}        	
        });
        GroupLayout compilerLayout = new GroupLayout(panelCompiler);
        panelCompiler.setLayout(compilerLayout);
        
        compilerLayout.setHorizontalGroup(compilerLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblFileNameLBL1)
        				.addComponent(lblPluginNameLBL1)
        				.addComponent(lblVersionLBL1)
        				.addComponent(lblCopyrightLBL1)
        				.addComponent(lblDescriptionLBL1))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblCompilerFileName)
        				.addComponent(lblCompilerName)
        				.addComponent(lblCompilerVersion)
        				.addComponent(txtCompilerCopyright)
        				.addComponent(scrollCompilerDescription))
        		.addContainerGap());
        compilerLayout.setVerticalGroup(
        		compilerLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblFileNameLBL1)
        				.addComponent(lblCompilerFileName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPluginNameLBL1)
        				.addComponent(lblCompilerName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblVersionLBL1)
        				.addComponent(lblCompilerVersion))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblCopyrightLBL1)
        				.addComponent(txtCompilerCopyright))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(compilerLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblDescriptionLBL1)
        				.addComponent(scrollCompilerDescription))
        		.addContainerGap());
        
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
        cpuLayout.setHorizontalGroup(cpuLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblFileNameLBL2)
        				.addComponent(lblPluginNameLBL2)
        				.addComponent(lblVersionLBL2)
        				.addComponent(lblCopyrightLBL2)
        				.addComponent(lblDescriptionLBL2))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblCPUFileName)
        				.addComponent(lblCPUName)
        				.addComponent(lblCPUVersion)
        				.addComponent(txtCPUCopyright)
        				.addComponent(scrollCpuDescription))
        		.addContainerGap());
        cpuLayout.setVerticalGroup(
        		cpuLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblFileNameLBL2)
        				.addComponent(lblCPUFileName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPluginNameLBL2)
        				.addComponent(lblCPUName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblVersionLBL2)
        				.addComponent(lblCPUVersion))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblCopyrightLBL2)
        				.addComponent(txtCPUCopyright))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(cpuLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblDescriptionLBL2)
        				.addComponent(scrollCpuDescription))
        		.addContainerGap());

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
        memoryLayout.setHorizontalGroup(memoryLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblFileNameLBL3)
        				.addComponent(lblPluginNameLBL3)
        				.addComponent(lblVersionLBL3)
        				.addComponent(lblCopyrightLBL3)
        				.addComponent(lblDescriptionLBL3))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblMemoryFileName)
        				.addComponent(lblMemoryName)
        				.addComponent(lblMemoryVersion)
        				.addComponent(txtMemoryCopyright)
        				.addComponent(scrollMemoryDescription))
        		.addContainerGap());
        memoryLayout.setVerticalGroup(
        		memoryLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblFileNameLBL3)
        				.addComponent(lblMemoryFileName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPluginNameLBL3)
        				.addComponent(lblMemoryName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblVersionLBL3)
        				.addComponent(lblMemoryVersion))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblCopyrightLBL3)
        				.addComponent(txtMemoryCopyright))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(memoryLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblDescriptionLBL3)
        				.addComponent(scrollMemoryDescription))
        		.addContainerGap());

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
        
        deviceLayout.setHorizontalGroup(deviceLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(lblDeviceLBL)
        				.addComponent(lblFileNameLBL4)
        				.addComponent(lblPluginNameLBL4)
        				.addComponent(lblVersionLBL4)
        				.addComponent(lblCopyrightLBL4)
        				.addComponent(lblDescriptionLBL4))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
        				.addComponent(cmbDevice)
        				.addComponent(lblDeviceFileName)
        				.addComponent(lblDeviceName)
        				.addComponent(lblDeviceVersion)
        				.addComponent(txtDeviceCopyright)
        				.addComponent(scrollDeviceDescription))
        		.addContainerGap());
        deviceLayout.setVerticalGroup(
        		deviceLayout.createSequentialGroup()
        		.addContainerGap()
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblDeviceLBL)
        				.addComponent(cmbDevice))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblFileNameLBL4)
        				.addComponent(lblDeviceFileName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblPluginNameLBL4)
        				.addComponent(lblDeviceName))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblVersionLBL4)
        				.addComponent(lblDeviceVersion))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblCopyrightLBL4)
        				.addComponent(txtDeviceCopyright))
        		.addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
        		.addGroup(deviceLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        				.addComponent(lblDescriptionLBL4)
        				.addComponent(scrollDeviceDescription))
        		.addContainerGap());

        tabbedPane.addTab("Devices", panelDevices);

        GroupLayout schemeLayout = new GroupLayout(panelScheme);
        panelScheme.setLayout(schemeLayout);
        schemeLayout.setHorizontalGroup(
            schemeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollScheme) //, GroupLayout.DEFAULT_SIZE, 567, Short.MAX_VALUE)
                .addContainerGap());
        schemeLayout.setVerticalGroup(
            schemeLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollScheme) //, GroupLayout.DEFAULT_SIZE, 283, Short.MAX_VALUE)
                .addContainerGap());

        tabbedPane.addTab("Abstract schema", panelScheme);

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(tabbedPane,100, 500, Short.MAX_VALUE)
                    .addComponent(lblName))
                .addContainerGap());
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblName)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(tabbedPane, GroupLayout.DEFAULT_SIZE, 350, Short.MAX_VALUE)
                .addContainerGap()));

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
