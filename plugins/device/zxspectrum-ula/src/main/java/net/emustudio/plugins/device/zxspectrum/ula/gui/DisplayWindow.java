/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.plugins.device.zxspectrum.ula.ULA;
import net.emustudio.plugins.device.zxspectrum.ula.audio.AudioSink;
import net.emustudio.plugins.device.zxspectrum.ula.recording.RecordingSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;
import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.DISPLAY_FRAME_TSTATES;

public class DisplayWindow extends DialogBase {
    private final static ImageIcon KEYBOARD_ICON = GUI.loadIcon("toolbar-keyboard.png");
    private final static ImageIcon VOLUME_ICON = GUI.loadIcon("toolbar-volume.png");
    private final static ImageIcon RECORD_ICON = GUI.loadIcon("toolbar-record.png");
    private final static ImageIcon STOP_RECORDING_ICON = GUI.loadIcon("toolbar-stop.png");

    public final static int MARGIN = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayWindow.class);
    private static final FileExtensionsFilter MP4_FILTER = new FileExtensionsFilter("MP4 video", "mp4");

    private final DisplayCanvas canvas;
    private final ULA ula;
    private final Dialogs dialogs;
    private final GUI gui;
    private final KeyboardCanvas keyboardCanvas = new KeyboardCanvas(0);
    private JButton btnRecord;

    private RecordingSession recordingSession;
    private Path lastRecordingDirectory = Path.of(System.getProperty("user.dir"));

    public DisplayWindow(JFrame parent, ULA ula, Dialogs dialogs, GUI gui) {
        super(parent, "ZX Spectrum48K", false);
        this.gui = gui;
        this.ula = Objects.requireNonNull(ula);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.canvas = new DisplayCanvas(ula, keyboardCanvas);

        buildContent();
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(WindowEvent winEvt) {
                canvas.redrawNow();
            }

            public void windowClosed(WindowEvent winEvt) {
                stopRecording(false);
                canvas.close();
            }
        });
        KeyboardDispatcher keyboardDispatcher = new KeyboardDispatcher(this);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(keyboardDispatcher);

        keyboardDispatcher.addOnKeyListener(keyboardCanvas);
        keyboardDispatcher.addOnKeyListener(ula);
    }

    @Override
    protected boolean shouldCloseOnEscape() {
        // ESC is used for the emulated ZX Spectrum keyboard, not for closing the window
        return false;
    }

    public void destroy() {
        stopRecording(false);
        canvas.close();
        dispose();
    }

    @Override
    protected JComponent initializeComponents() {

        JPopupMenu keyboardPopup = createVerticalSliderPopup(
                "Keyboard",
                keyboardCanvas.getAlpha(),
                value -> {
                    keyboardCanvas.setAlpha(value);
                    if (keyboardCanvas.isInteractiveInvisible()) {
                    keyboardCanvas.releaseMouseKeys(ula);
                }
                canvas.repaint();
                }
        );
        JPopupMenu volumePopup = createVerticalSliderPopup(
                "Volume",
                ula.getAudioVolumePercent(),
                ula::setAudioVolumePercent
        );

        JButton btnKeyboard = createToolbarButton(KEYBOARD_ICON, "Keyboard opacity", e ->
                togglePopup((AbstractButton) e.getSource(), keyboardPopup, volumePopup)
        );
        JButton btnVolume = createToolbarButton(VOLUME_ICON, "Beeper volume", e ->
                togglePopup((AbstractButton) e.getSource(), volumePopup, keyboardPopup)
        );
        btnRecord = createToolbarButton(RECORD_ICON, "Start video recording", e -> {
            keyboardPopup.setVisible(false);
            volumePopup.setVisible(false);
            if (recordingSession == null) {
                startRecording();
            } else {
                stopRecording(true);
            }
        });

        JToolBar toolbar = gui.toolBar();
        toolbar.add(btnKeyboard);
        toolbar.add(btnVolume);
        toolbar.add(btnRecord);

        JPanel bottomBar = gui.panel("insets 0", "[pref!]push", "[]");
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar);

        JPanel content = gui.panel("insets 0", "[grow]", "[grow]0[40!]");
        content.add(canvas, "grow, wrap");
        content.add(bottomBar, "growx");
        return content;
    }

    public DisplayCanvas getCanvas() {
        return canvas;
    }

    private void startRecording() {
        try {
            RecordingSession session = new RecordingSession(
                    Math.max(1, canvas.getWidth()),
                    Math.max(1, canvas.getHeight()),
                    DISPLAY_FRAME_TSTATES,
                    // Sample CPU frequency at recording-start so the muxer's frame-rate metadata
                    // matches the clock the user is currently running. RecordingSession expects an
                    // int (T-states/sec); cast is safe for any sane Spectrum-class clock (<2 GHz).
                    (int) Math.min(Integer.MAX_VALUE, ula.getCpuFrequencyHz()),
                    ula.getAudioSampleRate()
            );
            recordingSession = session;
            canvas.setFrameListener(session);
            ula.setRecordingSink(session);
            btnRecord.setIcon(STOP_RECORDING_ICON);
            btnRecord.setToolTipText("Stop recording and save video");
        } catch (IOException e) {
            LOGGER.error("Could not start ZX Spectrum recording", e);
            dialogs.showError("Could not start recording. Please see log file for details.", "Recording");
        }
    }

    private void stopRecording(boolean saveToFile) {
        RecordingSession session = recordingSession;
        if (session == null) {
            return;
        }

        recordingSession = null;
        canvas.setFrameListener(null);
        ula.flushRecordingBuffer();
        ula.setRecordingSink(AudioSink.NULL);
        btnRecord.setToolTipText("Start video recording");

        Optional<Path> selectedFile = saveToFile ? dialogs.chooseFile(
                "Save recording", "Save", lastRecordingDirectory, true, MP4_FILTER
        ) : Optional.empty();

        // Video export (encoding + audio muxing) is CPU-intensive and must not run on the EDT,
        // otherwise the ULA display freezes and the UI becomes unresponsive until the export finishes.
        btnRecord.setEnabled(false);
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                session.stop(selectedFile.orElse(null));
                return null;
            }

            @Override
            protected void done() {
                btnRecord.setEnabled(true);
                btnRecord.setIcon(RECORD_ICON);
                try {
                    get(); // propagate any exception from doInBackground
                    if (selectedFile.isPresent()) {
                        Path parent = selectedFile.get().getParent();
                        if (parent != null) {
                            lastRecordingDirectory = parent;
                        }
                    }
                } catch (Exception e) {
                    Throwable cause = (e instanceof java.util.concurrent.ExecutionException) ? e.getCause() : e;
                    LOGGER.error("Could not finish ZX Spectrum recording", cause);
                    dialogs.showError("Could not save recording. Please see log file for details.", "Recording");
                }
            }
        }.execute();
    }

    private JPopupMenu createVerticalSliderPopup(String title, int initialValue, java.util.function.IntConsumer onChange) {
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialValue);
        slider.setFocusable(false);

        JLabel titleLabel = gui.label(title);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel valueLabel = gui.label(initialValue + "%");
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ChangeListener changeListener = e -> {
            int value = slider.getValue();
            valueLabel.setText(value + "%");
            onChange.accept(value);
        };
        slider.addChangeListener(changeListener);

        JPanel panel = gui.panel("insets 8", "[grow]", "[]6[grow]6[]");
        panel.add(titleLabel, "growx, wrap");
        panel.add(slider, "align center, wrap");
        panel.add(valueLabel, "growx");

        JPopupMenu popup = new JPopupMenu();
        popup.add(panel);
        return popup;
    }

    private JButton createToolbarButton(Icon icon, String tooltip, java.util.function.Consumer<ActionEvent> action) {
        Action toolbarAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        };
        toolbarAction.putValue(SMALL_ICON, icon);
        toolbarAction.putValue(SHORT_DESCRIPTION, tooltip);
        return gui.toolbarButton(toolbarAction);
    }

    private void togglePopup(AbstractButton button, JPopupMenu popup, JPopupMenu otherPopup) {
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
