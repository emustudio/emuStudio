/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.spaceinvaders;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.ui.GUI;
import net.emustudio.emulib.runtime.ui.Dialogs;
import net.emustudio.emulib.runtime.ui.components.FileExtensionsFilter;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

final class DisplayWindow extends JFrame {
    private static final ImageIcon VOLUME_ICON = GUI.loadIcon("toolbar-volume.png");
    private static final ImageIcon RECORD_ICON = GUI.loadIcon("toolbar-record.png");
    private static final ImageIcon STOP_ICON = GUI.loadIcon("toolbar-stop.png");
    private static final FileExtensionsFilter MP4_FILTER = new FileExtensionsFilter("MP4 video", "mp4");
    private static final System.Logger LOGGER = System.getLogger(DisplayWindow.class.getName());
    private final DisplayPanel display;
    private final SampleSoundOutput sound;
    private final Dialogs dialogs;
    private JButton btnRecord;
    private volatile RecordingSession recordingSession;
    private Path lastRecordingDirectory = Path.of(System.getProperty("user.dir"));

    DisplayWindow(JFrame parent, MemoryContext<Byte> memory, SpaceInvadersHardware hardware,
                  int scale, boolean colorOverlay, SampleSoundOutput sound, GUI gui, Dialogs dialogs) {
        super("Space Invaders");
        this.sound = sound;
        this.dialogs = dialogs;
        display = new DisplayPanel(memory, hardware, scale, colorOverlay);
        add(display, BorderLayout.CENTER);
        add(createSoundBar(sound, gui), BorderLayout.SOUTH);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setResizable(true);
        pack();
        setLocationRelativeTo(parent);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopRecording(false);
            }
        });
    }

    private JPanel createSoundBar(SampleSoundOutput sound, GUI gui) {
        int initialVolume = sound.getVolumePercent();
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 100, initialVolume);
        slider.setFocusable(false);
        JLabel title = gui.label("Volume");
        title.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel value = gui.label(initialVolume + "%");
        value.setHorizontalAlignment(SwingConstants.CENTER);
        slider.addChangeListener(e -> {
            int volume = slider.getValue();
            value.setText(volume + "%");
            sound.setVolumePercent(volume);
        });
        JPanel controls = gui.panel("insets 8", "[grow]", "[]6[grow]6[]");
        controls.add(title, "growx, wrap");
        controls.add(slider, "align center, wrap");
        controls.add(value, "growx");
        JPopupMenu popup = new JPopupMenu();
        popup.add(controls);

        Action volumeAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (popup.isVisible()) {
                    popup.setVisible(false);
                } else {
                    AbstractButton button = (AbstractButton) e.getSource();
                    Dimension size = popup.getPreferredSize();
                    popup.show(button, Math.max(0, (button.getWidth() - size.width) / 2), -size.height);
                }
            }
        };
        volumeAction.putValue(Action.SMALL_ICON, VOLUME_ICON);
        volumeAction.putValue(Action.SHORT_DESCRIPTION, "Audio volume");
        JToolBar toolbar = gui.toolBar();
        toolbar.add(gui.toolbarButton(volumeAction));
        Action recordAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popup.setVisible(false);
                if (recordingSession == null) {
                    startRecording();
                } else {
                    stopRecording(true);
                }
            }
        };
        recordAction.putValue(Action.SMALL_ICON, RECORD_ICON);
        recordAction.putValue(Action.SHORT_DESCRIPTION, "Start video recording");
        btnRecord = gui.toolbarButton(recordAction);
        toolbar.add(btnRecord);

        JPanel bottomBar = gui.panel("insets 0", "[pref!]push", "[40!]");
        bottomBar.setBorder(new BevelBorder(BevelBorder.LOWERED));
        bottomBar.add(toolbar);
        return bottomBar;
    }

    void frameReady() {
        RecordingSession session = recordingSession;
        if (session != null) {
            if (!session.capture(display.captureFrame(session.width, session.height),
                    sound.captureAudio(RecordingSession.AUDIO_FRAMES_PER_VIDEO_FRAME)) && session.getFailure() != null) {
                SwingUtilities.invokeLater(() -> {
                    if (recordingSession == session) {
                        stopRecording(false);
                        dialogs.showError("Recording stopped: " + session.getFailure().getMessage(), "Recording");
                    }
                });
            }
        }
        display.repaint();
    }

    private void startRecording() {
        try {
            // Encode game pixels; scaling the window adds no detail and makes H.264 much slower.
            recordingSession = new RecordingSession(DisplayPanel.WIDTH, DisplayPanel.HEIGHT);
            btnRecord.setIcon(STOP_ICON);
            btnRecord.setToolTipText("Stop recording and save video");
        } catch (IOException e) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not start Space Invaders recording", e);
            dialogs.showError("Could not start recording. Please see log file for details.", "Recording");
        }
    }

    private void stopRecording(boolean saveToFile) {
        RecordingSession session = recordingSession;
        if (session == null) {
            return;
        }
        recordingSession = null;
        btnRecord.setToolTipText("Start video recording");
        Optional<Path> target = saveToFile ? dialogs.chooseFile(
                "Save recording", "Save", lastRecordingDirectory, true, MP4_FILTER
        ) : Optional.empty();
        btnRecord.setEnabled(false);
        btnRecord.setToolTipText("Saving video...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws IOException {
                session.stop(target.orElse(null));
                return null;
            }

            @Override
            protected void done() {
                btnRecord.setEnabled(true);
                btnRecord.setIcon(RECORD_ICON);
                btnRecord.setToolTipText("Start video recording");
                try {
                    get();
                    target.map(Path::getParent).ifPresent(parent -> lastRecordingDirectory = parent);
                } catch (Exception e) {
                    LOGGER.log(System.Logger.Level.ERROR, "Could not finish Space Invaders recording", e);
                    if (saveToFile) {
                        dialogs.showError("Could not save recording. Please see log file for details.", "Recording");
                    }
                }
            }
        }.execute();
    }

    @Override
    public void dispose() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::dispose);
            return;
        }
        stopRecording(false);
        super.dispose();
    }
}
