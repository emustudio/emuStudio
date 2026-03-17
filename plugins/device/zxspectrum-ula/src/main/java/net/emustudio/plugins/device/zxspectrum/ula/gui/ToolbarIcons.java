/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.zxspectrum.ula.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;

final class ToolbarIcons {
    private static final int SIZE = 18;

    private ToolbarIcons() {
    }

    static Icon keyboard() {
        return new BaseIcon() {
            @Override
            protected void paintIcon(Graphics2D graphics) {
                graphics.setColor(iconColor());
                graphics.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.draw(new RoundRectangle2D.Float(2, 4, 14, 10, 3, 3));
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 5; col++) {
                        graphics.fillRect(4 + col * 2, 6 + row * 2, 1, 1);
                    }
                }
                graphics.drawLine(5, 14, 13, 14);
            }
        };
    }

    static Icon volume() {
        return new BaseIcon() {
            @Override
            protected void paintIcon(Graphics2D graphics) {
                graphics.setColor(iconColor());
                graphics.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                Polygon speaker = new Polygon(
                        new int[]{3, 6, 9, 9, 6, 3},
                        new int[]{8, 8, 5, 13, 10, 10},
                        6
                );
                graphics.drawPolygon(speaker);
                graphics.draw(new Arc2D.Float(8, 5, 5, 8, -45, 90, Arc2D.OPEN));
                graphics.draw(new Arc2D.Float(10, 3, 6, 12, -45, 90, Arc2D.OPEN));
            }
        };
    }

    static Icon record() {
        return new BaseIcon() {
            @Override
            protected void paintIcon(Graphics2D graphics) {
                graphics.setColor(new Color(0xD8, 0x2E, 0x2F));
                graphics.fillOval(3, 3, 12, 12);
            }
        };
    }

    static Icon stop() {
        return new BaseIcon() {
            @Override
            protected void paintIcon(Graphics2D graphics) {
                graphics.setColor(iconColor());
                graphics.fillRoundRect(4, 4, 10, 10, 2, 2);
            }
        };
    }

    private abstract static class BaseIcon implements Icon {
        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g2d = (Graphics2D) graphics.create();
            try {
                g2d.translate(x, y);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                paintIcon(g2d);
            } finally {
                g2d.dispose();
            }
        }

        protected abstract void paintIcon(Graphics2D graphics);

        protected Color iconColor() {
            Color color = UIManager.getColor("Label.foreground");
            return color != null ? color : Color.BLACK;
        }
    }
}
