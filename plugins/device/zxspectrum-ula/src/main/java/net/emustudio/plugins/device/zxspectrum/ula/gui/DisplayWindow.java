/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.components.DialogBase;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;
import net.emustudio.emulib.runtime.ui.components.ToolbarButton;
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
import static net.emustudio.plugins.device.zxspectrum.bus.api.ZxParameters.ZX_48K_CPU_FREQUENCY;

public class DisplayWindow extends DialogBase {
    public final static int MARGIN = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(DisplayWindow.class);
    private static final FileExtensionsFilter MP4_FILTER = new FileExtensionsFilter("MP4 video", "mp4");


    private final DisplayCanvas canvas;
    private final ULA ula;
    private final Dialogs dialogs;
    private final KeyboardCanvas keyboardCanvas = new KeyboardCanvas(0);
    private ToolbarButton btnRecord;

    private RecordingSession recordingSession;
    private Path lastRecordingDirectory = Path.of(System.getProperty("user.dir"));

    public DisplayWindow(JFrame parent, ULA ula, Dialogs dialogs) {
        super(parent, "ZX Spectrum48K", false);
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

        ToolbarButton btnKeyboard = createToolbarButton(ToolbarIcons.keyboard(), "Keyboard opacity", e ->
                togglePopup((AbstractButton) e.getSource(), keyboardPopup, volumePopup)
        );
        ToolbarButton btnVolume = createToolbarButton(ToolbarIcons.volume(), "Beeper volume", e ->
                togglePopup((AbstractButton) e.getSource(), volumePopup, keyboardPopup)
        );
        btnRecord = createToolbarButton(ToolbarIcons.record(), "Start video recording", e -> {
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

        JPanel bottomBar = GUI.panel("insets 0", "[pref!]push", "[]");
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar);

        JPanel content = GUI.panel("insets 0", "[grow]", "[grow]0[40!]");
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
                    ZX_48K_CPU_FREQUENCY,
                    ula.getAudioSampleRate()
            );
            recordingSession = session;
            canvas.setFrameListener(session);
            ula.setRecordingSink(session);
            btnRecord.setIcon(ToolbarIcons.stop());
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
        ula.setRecordingSink(AudioSink.NULL);
        btnRecord.setIcon(ToolbarIcons.record());
        btnRecord.setToolTipText("Start video recording");

        Optional<Path> selectedFile = saveToFile ? dialogs.chooseFile(
                "Save recording", "Save", lastRecordingDirectory, true, MP4_FILTER
        ) : Optional.empty();

        try {
            session.stop(selectedFile);
            if (selectedFile.isPresent()) {
                Path parent = selectedFile.get().getParent();
                if (parent != null) {
                    lastRecordingDirectory = parent;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not finish ZX Spectrum recording", e);
            dialogs.showError("Could not save recording. Please see log file for details.", "Recording");
        }
    }

    private JPopupMenu createVerticalSliderPopup(String title, int initialValue, java.util.function.IntConsumer onChange) {
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialValue);
        slider.setFocusable(false);

        JLabel titleLabel = GUI.label(title);
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel valueLabel = GUI.label(initialValue + "%");
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        ChangeListener changeListener = e -> {
            int value = slider.getValue();
            valueLabel.setText(value + "%");
            onChange.accept(value);
        };
        slider.addChangeListener(changeListener);

        JPanel panel = GUI.panel("insets 8", "[grow]", "[]6[grow]6[]");
        panel.add(titleLabel, "growx, wrap");
        panel.add(slider, "align center, wrap");
        panel.add(valueLabel, "growx");

        JPopupMenu popup = new JPopupMenu();
        popup.add(panel);
        return popup;
    }

    private ToolbarButton createToolbarButton(Icon icon, String tooltip, java.util.function.Consumer<ActionEvent> action) {
        Action toolbarAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.accept(e);
            }
        };
        toolbarAction.putValue(SMALL_ICON, icon);
        toolbarAction.putValue(SHORT_DESCRIPTION, tooltip);
        return GUI.toolbarButton(toolbarAction);
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
