/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.audiotape_player.gui;

import net.emustudio.plugins.device.audiotape_player.AutomationEvent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Panel that draws the vertical timeline of automation events.
 */
class TimelinePanel extends JPanel {
    static final int EVENT_ROW_HEIGHT = 50;
    private static final int TIMELINE_WIDTH = 400;
    private static final Color COLOR_PAST = new Color(180, 180, 180);
    private static final Color COLOR_ACTIVE = new Color(50, 150, 50);
    private static final Color COLOR_FUTURE = new Color(220, 220, 220);
    private static final Color COLOR_CONNECTOR = new Color(100, 100, 100);
    private static final Color COLOR_TEXT = Color.BLACK;
    private static final Color COLOR_ACTIVE_TEXT = Color.WHITE;
    private static final Color COLOR_SELECTED_BG = new Color(100, 150, 255, 50);

    private final List<AutomationEvent> events;
    private final IntSupplier activeIndexSupplier;
    int selectedIndex = -1;

    @FunctionalInterface
    interface IntSupplier {
        int getAsInt();
    }

    TimelinePanel(List<AutomationEvent> events, IntSupplier activeIndexSupplier) {
        this.events = events;
        this.activeIndexSupplier = activeIndexSupplier;
        setBackground(Color.WHITE);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedIndex = e.getY() / EVENT_ROW_HEIGHT;
                if (clickedIndex >= 0 && clickedIndex < events.size()) {
                    selectedIndex = clickedIndex;
                } else {
                    selectedIndex = -1;
                }
                repaint();
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        int height = Math.max(events.size() * EVENT_ROW_HEIGHT, 300);
        return new Dimension(TIMELINE_WIDTH, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int circleRadius = 12;
        int lineX = 40;
        int activeIndex = activeIndexSupplier.getAsInt();
        for (int i = 0; i < events.size(); i++) {
            int y = i * EVENT_ROW_HEIGHT + EVENT_ROW_HEIGHT / 2;
            AutomationEvent event = events.get(i);

            // Selection highlight
            if (i == selectedIndex) {
                g2.setColor(COLOR_SELECTED_BG);
                g2.fillRect(0, i * EVENT_ROW_HEIGHT, getWidth(), EVENT_ROW_HEIGHT);
            }

            // Connector line
            if (i > 0) {
                g2.setColor(COLOR_CONNECTOR);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(lineX, (i - 1) * EVENT_ROW_HEIGHT + EVENT_ROW_HEIGHT / 2 + circleRadius, lineX, y - circleRadius);
            }

            // Circle
            Color circleColor;
            Color textColor;
            if (i < activeIndex) {
                circleColor = COLOR_PAST;
                textColor = COLOR_TEXT;
            } else if (i == activeIndex) {
                circleColor = COLOR_ACTIVE;
                textColor = COLOR_ACTIVE_TEXT;
            } else {
                circleColor = COLOR_FUTURE;
                textColor = COLOR_TEXT;
            }
            g2.setColor(circleColor);
            g2.fillOval(lineX - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);
            g2.setColor(COLOR_CONNECTOR);
            g2.drawOval(lineX - circleRadius, y - circleRadius, circleRadius * 2, circleRadius * 2);

            // Index number inside circle
            g2.setColor(textColor);
            g2.setFont(g2.getFont().deriveFont(Font.BOLD, 11f));
            String indexStr = String.valueOf(i + 1);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(indexStr, lineX - fm.stringWidth(indexStr) / 2, y + fm.getAscent() / 2 - 1);

            // Event description
            g2.setColor(COLOR_TEXT);
            g2.setFont(g2.getFont().deriveFont(i == activeIndex ? Font.BOLD : Font.PLAIN, 13f));
            g2.drawString(event.getDescription(), lineX + circleRadius + 12, y + 5);
        }
        g2.dispose();
    }
}

