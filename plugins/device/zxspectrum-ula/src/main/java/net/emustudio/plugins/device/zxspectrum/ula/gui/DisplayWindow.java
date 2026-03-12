/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;
import net.emustudio.plugins.device.zxspectrum.ula.audio.AudioSink;
import net.emustudio.plugins.device.zxspectrum.ula.audio.Beeper;
import net.emustudio.plugins.device.zxspectrum.ula.recording.DisplayRecordingSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.DISPLAY_FRAME_TSTATES;

public class DisplayWindow extends JDialog {
    public final static int MARGIN = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayWindow.class);
    private static final FileExtensionsFilter AVI_FILTER = new FileExtensionsFilter("AVI video", "avi");

    private final static int BOUND_X = (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_WIDTH + 2 * MARGIN);
    private final static int BOUND_Y = (int) (DisplayCanvas.ZOOM * DisplayCanvas.SCREEN_IMAGE_HEIGHT + 2 * MARGIN);

    private final DisplayCanvas canvas;
    private final ULA ula;
    private final Dialogs dialogs;
    private final KeyboardCanvas keyboardCanvas = new KeyboardCanvas(0);
    private final JButton btnRecord = createToolbarButton(ToolbarIcons.record(), "Start AVI recording");

    private DisplayRecordingSession recordingSession;
    private Path lastRecordingDirectory = Path.of(System.getProperty("user.dir"));

    public DisplayWindow(JFrame parent, ULA ula, Dialogs dialogs) {
        super(parent);
        this.ula = Objects.requireNonNull(ula);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.canvas = new DisplayCanvas(ula, keyboardCanvas);

        initComponents();
        setLocationRelativeTo(parent);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(WindowEvent winEvt) {
                canvas.ensureStarted();
                canvas.redrawNow();
            }

            public void windowActivated(WindowEvent winEvt) {
                canvas.ensureStarted();
                canvas.redrawNow();
            }

            public void windowClosed(WindowEvent winEvt) {
                stopRecording(false);
                canvas.close();
            }
        });
        KeyboardDispatcher keyboardDispatcher = new KeyboardDispatcher();
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(keyboardDispatcher);

        keyboardDispatcher.addOnKeyListener(keyboardCanvas);
        keyboardDispatcher.addOnKeyListener(ula);
    }

    public void destroy() {
        stopRecording(false);
        canvas.close();
        dispose();
    }

    private void initComponents() {
        setTitle("ZX Spectrum48K");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        canvas.setBounds(MARGIN, MARGIN, BOUND_X, BOUND_Y);

        JPopupMenu keyboardPopup = createVerticalSliderPopup(
                "Keyboard",
                keyboardCanvas.getAlpha(),
                value -> {
                    keyboardCanvas.setAlpha(value);
                    if (keyboardCanvas.isInteractiveInvisible()) {
                        keyboardCanvas.releaseMouseKeys(ula);
                    }
                    canvas.ensureStarted();
                    canvas.runPaintCycle();
                }
        );
        JPopupMenu volumePopup = createVerticalSliderPopup(
                "Volume",
                ula.getAudioVolumePercent(),
                ula::setAudioVolumePercent
        );

        JButton btnKeyboard = createToolbarButton(ToolbarIcons.keyboard(), "Keyboard opacity");
        btnKeyboard.addActionListener(e -> togglePopup(btnKeyboard, keyboardPopup, volumePopup));

        JButton btnVolume = createToolbarButton(ToolbarIcons.volume(), "Beeper volume");
        btnVolume.addActionListener(e -> togglePopup(btnVolume, volumePopup, keyboardPopup));

        btnRecord.addActionListener(e -> {
            keyboardPopup.setVisible(false);
            volumePopup.setVisible(false);
            if (recordingSession == null) {
                startRecording();
            } else {
                stopRecording(true);
            }
        });

        JToolBar toolbar = GUI.toolBar();
        toolbar.add(btnKeyboard);
        toolbar.add(btnVolume);
        toolbar.add(btnRecord);

        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar, BorderLayout.WEST);

        JPanel content = GUI.panel("insets 0", "[grow]", "[grow]0[40!]");
        content.add(canvas, "grow, wrap");
        content.add(bottomBar, "growx");

        setContentPane(content);
        pack();
    }

    public DisplayCanvas getCanvas() {
        return canvas;
    }

    private void startRecording() {
        try {
            canvas.ensureStarted();
            DisplayRecordingSession session = new DisplayRecordingSession(
                    Math.max(1, canvas.getWidth()),
                    Math.max(1, canvas.getHeight()),
                    DISPLAY_FRAME_TSTATES,
                    Beeper.ZX_SPECTRUM_FREQUENCY,
                    ula.getAudioSampleRate()
            );
            recordingSession = session;
            canvas.setFrameListener(session);
            ula.setRecordingSink(session);
            session.accept(canvas.captureFrame());
            btnRecord.setIcon(ToolbarIcons.stop());
            btnRecord.setToolTipText("Stop recording and save AVI");
        } catch (IOException e) {
            LOGGER.error("Could not start ZX Spectrum recording", e);
            dialogs.showError("Could not start recording. Please see log file for details.", "Recording");
        }
    }

    private void stopRecording(boolean saveToFile) {
        DisplayRecordingSession session = recordingSession;
        if (session == null) {
            return;
        }

        recordingSession = null;
        canvas.setFrameListener(null);
        ula.setRecordingSink(AudioSink.NULL);
        btnRecord.setIcon(ToolbarIcons.record());
        btnRecord.setToolTipText("Start AVI recording");

        try {
            if (!saveToFile) {
                session.discard();
                return;
            }

            Optional<Path> selectedFile = dialogs.chooseFile(
                    "Save recording", "Save", lastRecordingDirectory, true, AVI_FILTER
            );
            if (selectedFile.isPresent()) {
                Path target = session.moveTo(selectedFile.get());
                if (target.getParent() != null) {
                    lastRecordingDirectory = target.getParent();
                }
            } else {
                session.discard();
                dialogs.showInfo("Recording discarded.", "Recording");
            }
        } catch (IOException e) {
            LOGGER.error("Could not finish ZX Spectrum recording", e);
            try {
                session.discard();
            } catch (IOException discardError) {
                LOGGER.warn("Could not delete temporary ZX Spectrum recording", discardError);
            }
            dialogs.showError("Could not save recording. Please see log file for details.", "Recording");
        }
    }

    private JPopupMenu createVerticalSliderPopup(String title, int initialValue, java.util.function.IntConsumer onChange) {
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialValue);
        slider.setFocusable(false);

        JLabel valueLabel = new JLabel(initialValue + "%", SwingConstants.CENTER);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        ChangeListener changeListener = e -> {
            int value = slider.getValue();
            valueLabel.setText(value + "%");
            onChange.accept(value);
        };
        slider.addChangeListener(changeListener);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(6));
        panel.add(slider);
        panel.add(Box.createVerticalStrut(6));
        panel.add(valueLabel);

        JPopupMenu popup = new JPopupMenu();
        popup.add(panel);
        return popup;
    }

    private JButton createToolbarButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setFocusable(false);
        button.setToolTipText(tooltip);
        button.putClientProperty("JButton.buttonType", "toolbar");
        return button;
    }

    private void togglePopup(JButton button, JPopupMenu popup, JPopupMenu otherPopup) {
        if (otherPopup.isVisible()) {
            otherPopup.setVisible(false);
        }
        if (popup.isVisible()) {
            popup.setVisible(false);
            return;
        }
        Dimension preferredSize = popup.getPreferredSize();
        int x = Math.max(0, (button.getWidth() - preferredSize.width) / 2);
        int y = -preferredSize.height;
        popup.show(button, x, y);
    }
}
