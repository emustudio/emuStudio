package net.emustudio.plugins.device.vt100.gui;

import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.vt100.TerminalSettings;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import javax.swing.*;
import javax.swing.GroupLayout;
import javax.swing.LayoutStyle;
import javax.swing.border.*;


public class SettingsDialog extends JDialog {
    private final TerminalSettings settings;
    private final Dialogs dialogs;

    public SettingsDialog(JFrame parent, TerminalSettings settings, Dialogs dialogs) {
        super(parent, true);

        this.settings = Objects.requireNonNull(settings);
        this.dialogs = Objects.requireNonNull(dialogs);
        initComponents();

        readSettings();
    }

    private void readSettings() {
        txtInputFile.setText(settings.getInputPath().toString());
        txtOutputFile.setText(settings.getOutputPath().toString());
        spnInputDelay.setValue(settings.getInputReadDelayMillis());
    }

    private void updateSettings() throws IOException {
        settings.setInputPath(Path.of(txtInputFile.getText()));
        settings.setOutputPath(Path.of(txtOutputFile.getText()));
        settings.setInputReadDelayMillis((Integer) spnInputDelay.getValue());
        settings.write();
    }

    private void initComponents() {
        JPanel panelRedirectIO = new JPanel();
        JLabel lblInputFile = new JLabel("Input file:");
        JLabel lblOutputFile = new JLabel("Output file:");
        JLabel lblRedirectIoNote = new JLabel("In No GUI mode, input/output will be redirected to files.");
        JLabel lblInputDelay = new JLabel("Input delay:");
        JLabel lblMs = new JLabel("ms");

        setTitle("VT100 Terminal Settings");
        setModal(true);
        Container contentPane = getContentPane();

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        btnSave.addActionListener(this::btnSaveActionPerformed);
        btnSave.setFont(btnSave.getFont().deriveFont(Font.BOLD));
        btnSave.setDefaultCapable(true);

        panelRedirectIO.setBorder(new TitledBorder(null, "Redirect I/O", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION,
                new Font("sansserif", Font.BOLD, 13)));

        GroupLayout panelIOLayout = new GroupLayout(panelRedirectIO);
        panelRedirectIO.setLayout(panelIOLayout);
        panelIOLayout.setHorizontalGroup(
                panelIOLayout.createParallelGroup()
                        .addGroup(panelIOLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(panelIOLayout.createParallelGroup()
                                        .addComponent(lblRedirectIoNote)
                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                .addGroup(panelIOLayout.createParallelGroup()
                                                        .addComponent(lblInputFile)
                                                        .addComponent(lblOutputFile)
                                                        .addComponent(lblInputDelay))
                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                .addGroup(panelIOLayout.createParallelGroup()
                                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                                .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addComponent(lblMs))
                                                        .addGroup(panelIOLayout.createSequentialGroup()
                                                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                                                        .addComponent(txtInputFile, GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                                                                        .addComponent(txtOutputFile))
                                                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                                                .addGroup(panelIOLayout.createParallelGroup()
                                                                        .addComponent(btnBrowseInputFile)
                                                                        .addComponent(btnBrowseOutputFile))))))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        panelIOLayout.setVerticalGroup(
                panelIOLayout.createParallelGroup()
                        .addGroup(GroupLayout.Alignment.TRAILING, panelIOLayout.createSequentialGroup()
                                .addComponent(lblRedirectIoNote, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputFile)
                                        .addComponent(txtInputFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnBrowseInputFile))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblOutputFile)
                                        .addComponent(txtOutputFile, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(btnBrowseOutputFile))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(panelIOLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(lblInputDelay)
                                        .addComponent(spnInputDelay, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblMs))
                                .addContainerGap())
        );

        GroupLayout contentPaneLayout = new GroupLayout(contentPane);
        contentPane.setLayout(contentPaneLayout);
        contentPaneLayout.setHorizontalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addGroup(contentPaneLayout.createParallelGroup()
                                        .addGroup(contentPaneLayout.createSequentialGroup()
                                                .addContainerGap()
                                                .addComponent(panelRedirectIO, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, contentPaneLayout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnSave, GroupLayout.PREFERRED_SIZE, 75, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())

        );
        contentPaneLayout.setVerticalGroup(
                contentPaneLayout.createParallelGroup()
                        .addGroup(contentPaneLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(panelRedirectIO, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnSave)
                                .addContainerGap())
        );
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void btnSaveActionPerformed(ActionEvent evt) {
        if (txtInputFile.getText().trim().equals(txtOutputFile.getText().trim())) {
            dialogs.showError("Input and output file names cannot point to the same file");
            txtInputFile.grabFocus();
            return;
        }
        try {
            updateSettings();
            dispose();
        } catch (IOException e) {
            dialogs.showError("Input or output file names (or both) do not exist. Please make sure they do.", "VT100 Terminal");
        }
    }

    private final JButton btnSave = new JButton("Save");
    private final JTextField txtInputFile = new JTextField();
    private final JTextField txtOutputFile = new JTextField();
    private final JButton btnBrowseInputFile = new JButton("Browse...");
    private final JButton btnBrowseOutputFile = new JButton("Browse...");
    private final JSpinner spnInputDelay = new JSpinner();
}
