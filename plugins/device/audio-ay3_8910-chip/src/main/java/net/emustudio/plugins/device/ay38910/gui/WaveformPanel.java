/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.ay38910.gui;

import net.emustudio.plugins.device.ay38910.Ay38910Chip;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

final class WaveformPanel extends JPanel {
    private static final Color BACKGROUND = new Color(14, 18, 28);
    private static final Color GRID = new Color(45, 57, 79);
    private static final Color MIDLINE = new Color(107, 122, 147);
    private static final Color WAVEFORM = new Color(77, 214, 178);

    private final Ay38910Chip chip;
    private volatile BufferedImage offscreenImage;
    private volatile Dimension renderSize = new Dimension();
    private volatile boolean displayable;
    private ScheduledExecutorService refreshExecutor;

    WaveformPanel(Ay38910Chip chip) {
        this.chip = Objects.requireNonNull(chip);
        setOpaque(true);
        setBackground(BACKGROUND);
        setPreferredSize(new Dimension(720, 320));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                renderSize = event.getComponent().getSize();
            }
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        displayable = true;
        renderSize = getSize();
    }

    @Override
    public void removeNotify() {
        displayable = false;
        super.removeNotify();
    }

    synchronized void startRefreshing(int refreshMs) {
        if (refreshExecutor != null) {
            return;
        }

        refreshExecutor = Executors.newSingleThreadScheduledExecutor(WaveformPanel::newRefreshThread);
        refreshExecutor.scheduleWithFixedDelay(this::refreshSamples, 0, refreshMs, TimeUnit.MILLISECONDS);
    }

    synchronized void stopRefreshing() {
        ScheduledExecutorService executor = refreshExecutor;
        refreshExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        BufferedImage img = offscreenImage;
        if (img != null) {
            g.drawImage(img, 0, 0, null);
        }
    }

    private void paintGrid(Graphics2D g2, int width, int height, int midY) {
        g2.setColor(GRID);
        for (int x = 0; x < width; x += Math.max(1, width / 10)) {
            g2.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += Math.max(1, height / 8)) {
            g2.drawLine(0, y, width, y);
        }

        g2.setColor(MIDLINE);
        g2.drawLine(0, midY, width, midY);
    }

    private void paintWaveform(Graphics2D g2, int width, int height, int midY, short[] samples) {
        if (samples.length < 2) {
            return;
        }

        int drawableHeight = Math.max(1, (height / 2) - 12);
        double xScale = (double) (width - 1) / (samples.length - 1);

        g2.setColor(WAVEFORM);
        g2.setStroke(new BasicStroke(1.8f));

        int lastX = 0;
        int lastY = sampleToY(samples[0], midY, drawableHeight);
        for (int i = 1; i < samples.length; i++) {
            int x = (int) Math.round(i * xScale);
            int y = sampleToY(samples[i], midY, drawableHeight);
            g2.drawLine(lastX, lastY, x, y);
            lastX = x;
            lastY = y;
        }
    }

    private int sampleToY(short sample, int midY, int drawableHeight) {
        double normalized = sample / (double) Short.MAX_VALUE;
        return midY - (int) Math.round(normalized * drawableHeight);
    }

    private void refreshSamples() {
        short[] samples = chip.copyRecentWaveform();
        if (!displayable) {
            return;
        }

        Dimension size = renderSize;
        int width = size.width;
        int height = size.height;
        if (width <= 0 || height <= 0) {
            return;
        }

        int midY = height / 2;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BACKGROUND);
            g2.fillRect(0, 0, width, height);

            paintGrid(g2, width, height, midY);
            paintWaveform(g2, width, height, midY, samples);
        } finally {
            g2.dispose();
        }

        offscreenImage = img;
        SwingUtilities.invokeLater(() -> {
            if (isDisplayable()) {
                repaint();
            }
        });
    }

    private static Thread newRefreshThread(Runnable runnable) {
        Thread thread = new Thread(runnable, "ay38910-waveform");
        thread.setDaemon(true);
        return thread;
    }
}
