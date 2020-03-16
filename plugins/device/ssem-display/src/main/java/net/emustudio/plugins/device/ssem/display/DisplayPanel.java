/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.ssem.display;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.jcip.annotations.ThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

@ThreadSafe
public class DisplayPanel extends JPanel {
    private final static int PIXEL_SIZE = 10;
    private final static int PIXEL_SIZE_PLUS_GAP = PIXEL_SIZE + 2;
    private final static int CELL_SIZE = 32;
    private final static int ROWS = 32;

    private final boolean[][] memory = new boolean[CELL_SIZE][CELL_SIZE];

    DisplayPanel() {
        super.setBackground(Color.BLACK);
        super.setDoubleBuffered(true);
    }

    void writeRow(Byte[] value, int row) {
        int number = NumberUtils.readInt(value, NumberUtils.Strategy.BIG_ENDIAN);
        Boolean[] bits = String
            .format("%" + CELL_SIZE + "s", Integer.toBinaryString(number))
            .chars()
            .mapToObj(c -> c == '1').toArray(Boolean[]::new);

        for (int i = 0; i < ROWS; i++) {
            memory[row][i] = bits[i];
        }
        repaint();
    }

    void clear() {
        for (boolean[] memoryRow : memory) {
            Arrays.fill(memoryRow, false);
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Dimension size = getSize();
        int startX = size.width / 2 - (CELL_SIZE / 2) * PIXEL_SIZE_PLUS_GAP - PIXEL_SIZE;
        int startY = size.height / 2 - (ROWS / 2) * PIXEL_SIZE_PLUS_GAP;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, size.width, size.height);

        for (int i = 0; i < memory.length; i++) {
            for (int j = 0; j < memory[i].length; j++) {
                if (memory[i][j]) {
                    g.setColor(Color.GREEN);
                    g.fillRect(
                        startX + j * PIXEL_SIZE_PLUS_GAP,
                        startY + i * PIXEL_SIZE_PLUS_GAP,
                        PIXEL_SIZE,
                        PIXEL_SIZE
                    );
                } else {
                    g.setColor(Color.DARK_GRAY);
                    g.fillRect(
                        startX + j * PIXEL_SIZE_PLUS_GAP,
                        startY + i * PIXEL_SIZE_PLUS_GAP,
                        PIXEL_SIZE,
                        PIXEL_SIZE
                    );
                }
            }
        }
    }

}
