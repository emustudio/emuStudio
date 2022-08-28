package net.emustudio.plugins.device.zxspectrum.display;

import net.emustudio.emulib.runtime.helpers.RadixUtils;
import net.emustudio.emulib.runtime.interaction.Dialogs;
import net.emustudio.plugins.device.zxspectrum.display.io.*;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Objects;
import java.util.StringTokenizer;

class ZxSpectrumDisplayGui extends JDialog implements OutputProvider, Keyboard.KeyboardListener {
    private final Dialogs dialogs;

    private final ImageIcon blueIcon;
    private final ImageIcon redIcon;

    private final Display canvas;
    private final Keyboard keyboard;

    private ZxSpectrumDisplayGui(JFrame parent, Keyboard keyboard, Dialogs dialogs) {
        super(parent);
        this.dialogs = Objects.requireNonNull(dialogs);

        URL blueIconURL = getClass().getResource(
            "/net/emustudio/plugins/device/zxspectrum/display/16_circle_blue.png"
        );
        URL redIconURL = getClass().getResource(
            "/net/emustudio/plugins/device/zxspectrum/display/16_circle_red.png"
        );

        blueIcon = new ImageIcon(Objects.requireNonNull(blueIconURL));
        redIcon = new ImageIcon(Objects.requireNonNull(redIconURL));

        this.keyboard = keyboard;

        setTitle("BrainDuck Terminal");
        initComponents();
        setLocationRelativeTo(parent);

        canvas = new Display();
        scrollPane.setViewportView(canvas);
        canvas.start();
    }

    @Override
    public void readEnded() {
        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.repaint();
        btnASCII.setEnabled(false);
    }

    @Override
    public void reset() {
        canvas.clearScreen();
    }

    @Override
    public void write(int character) {
        writeStarted();
        Cursor cursor = canvas.getTextCanvasCursor();
        switch (character) {
            case 5: // HERE IS
                // insertHereIs();
                break;
            case 6:
                // print COMMA
                cursor.printComma(canvas);
                break;
            case 7: // BELL
                break;
            case 8: // BACKSPACE
                cursor.moveBackwards();
                canvas.writeAtCursor(' ');
                break;
            case 9:
                cursor.moveForwards();
                break;
            case 0x0A: // line feed
            case 0x1A: // clear screen
                //clearScreen();
                cursor.moveDown(canvas);
                break;
            case 0x0B: // VT
                cursor.moveUp();
                break;
            case 0x0C: // delete
                canvas.writeAtCursor(' ');
                break;
            case 0x0D: // CARRIAGE RETURN
                cursor.carriageReturn();
                cursor.moveDown(canvas); // TODO
                break;

            case 0x17: // 23
                cursor.moveForwardsTab();
                break;

            case 0x16:
                // AT
//                ignore = true;
//                ignorec = 0;
                break;

            case 0x0E: // SO
            case 0x0F: // SI
                break;
            case 0x1B: // initiates load cursor operation
            case 0x1E: // homes cursor
                //   cursor.home();
                break;
            case 127:
                canvas.writeAtCursor('\u00a9');
                cursor.moveForwardsRolling(canvas);
                break;

            case 0x1C:
                canvas.clearScreen();
                break;

            case 0x82:
                canvas.writeAtCursor('\u2590');
                return;
            case 0x83:
                canvas.writeAtCursor('\u2580');
                return;
            case 0x85:
                canvas.writeAtCursor('\u2599');
                return;
            case 0x86:
                canvas.writeAtCursor('\u259C');
                return;
            case 0x89:
            case 0x8C:
            case 0x8A:
                canvas.writeAtCursor(' '); // Ᾱ0
                return;
            case 0xDE:
                canvas.writeAtCursor('*');
                return;
        }

        if (character >= 32) {
            canvas.writeAtCursor((char) character);
            cursor.moveForwardsRolling(canvas);
        }
        repaint();
    }

    @Override
    public void showGUI() {
        this.setVisible(true);
    }

    @Override
    public void close() {
        canvas.stop();
        GUIUtils.removeListenerRecursively(this, keyboard);
        dispose();
    }


    static ZxSpectrumDisplayGui create(JFrame parent, Keyboard keyboard, Dialogs dialogs) {
        ZxSpectrumDisplayGui dialog = new ZxSpectrumDisplayGui(parent, keyboard, dialogs);
        GUIUtils.addListenerRecursively(dialog, dialog.keyboard);
        return dialog;
    }

    private void writeStarted() {
        lblStatusIcon.setIcon(redIcon);
        lblStatusIcon.repaint();
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    private void initComponents() {

        javax.swing.JPanel panelStatus = new javax.swing.JPanel();
        lblStatusIcon = new javax.swing.JLabel();
        btnASCII = new javax.swing.JButton();
        scrollPane = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        lblStatusIcon.setIcon(blueIcon);
        lblStatusIcon.setToolTipText("Waiting for input? (red - yes, blue - no)");
        lblStatusIcon.setVerticalAlignment(javax.swing.SwingConstants.TOP);

        btnASCII.setFont(btnASCII.getFont());
        btnASCII.setIcon(new javax.swing.ImageIcon(getClass().getResource("/net/emustudio/plugins/device/zxspectrum/display/16_ascii.png")));
        btnASCII.setToolTipText("Input by ASCII code");
        btnASCII.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        btnASCII.setEnabled(false);
        btnASCII.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        btnASCII.addActionListener(this::btnASCIIActionPerformed);

        javax.swing.GroupLayout panelStatusLayout = new javax.swing.GroupLayout(panelStatus);
        panelStatus.setLayout(panelStatusLayout);
        panelStatusLayout.setHorizontalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelStatusLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(lblStatusIcon, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnASCII)
                    .addContainerGap(664, Short.MAX_VALUE))
        );
        panelStatusLayout.setVerticalGroup(
            panelStatusLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(lblStatusIcon, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnASCII, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE)
        );

        scrollPane.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(panelStatus, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(scrollPane)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(scrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 407, Short.MAX_VALUE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(panelStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }

    private void btnASCIIActionPerformed(java.awt.event.ActionEvent evt) {
        dialogs
            .readString("Enter ASCII codes separated with spaces:", "Add ASCII codes")
            .ifPresent(asciiCodes -> {
                StringTokenizer tokenizer = new StringTokenizer(asciiCodes);

                RadixUtils radixUtils = RadixUtils.getInstance();
                try {
                    while (tokenizer.hasMoreTokens()) {
                        int ascii = radixUtils.parseRadix(tokenizer.nextToken());
                        keyboard.keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, ascii, (char) ascii));
                    }
                } catch (NumberFormatException ex) {
                    dialogs.showError("Invalid number format in the input: " + ex.getMessage(), "Add ASCII codes");
                }
            });
    }

    private javax.swing.JButton btnASCII;
    private javax.swing.JLabel lblStatusIcon;
    private javax.swing.JScrollPane scrollPane;
}
