/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.jcip.annotations.ThreadSafe;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

@ThreadSafe
public class DisplayPanel extends JPanel {
    private final static int CELL_SIZE_BITS = 32;
    private final static int CELL_SIZE_BYTES = CELL_SIZE_BITS / 8;

    private final static int PIXEL_SIZE = 10;
    private final static int PIXEL_SIZE_WITH_GAP = PIXEL_SIZE + 2;

    private final boolean[][] memory = new boolean[CELL_SIZE_BITS][CELL_SIZE_BITS];

    DisplayPanel() {
        super.setBackground(Color.BLACK);
        super.setDoubleBuffered(true);
    }

    void reset(MemoryContext<Byte> memory) {
        for (boolean[] memoryRow : this.memory) {
            Arrays.fill(memoryRow, false);
        }
        for (int i = 0; i < CELL_SIZE_BITS; i++) {
            Byte[] value = memory.read(i * CELL_SIZE_BYTES, CELL_SIZE_BYTES);
            writeRow(value, i);
        }
        repaint();
    }

    void writeRow(Byte[] value, int row) {
        int number = NumberUtils.readInt(value, NumberUtils.Strategy.REVERSE_BITS);
        for (int i = 0; i < CELL_SIZE_BITS; i++) {
            memory[row][i] = (((number >>> i) & 1) == 1);
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        Dimension size = getSize();
        int startX = size.width / 2 - (CELL_SIZE_BITS / 2) * PIXEL_SIZE_WITH_GAP - PIXEL_SIZE;
        int startY = size.height / 2 - (CELL_SIZE_BITS / 2) * PIXEL_SIZE_WITH_GAP;

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, size.width, size.height);

        for (int i = 0; i < memory.length; i++) {
            for (int j = 0; j < memory[i].length; j++) {
                if (memory[i][j]) {
                    g.setColor(Color.GREEN);
                } else {
                    g.setColor(Color.DARK_GRAY);
                }
                g.fillRect(
                    startX + j * PIXEL_SIZE_WITH_GAP,
                    startY + i * PIXEL_SIZE_WITH_GAP,
                    PIXEL_SIZE,
                    PIXEL_SIZE
                );
            }
        }
    }
}
