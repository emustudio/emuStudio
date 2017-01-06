/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2016, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.ssem.display;

import emulib.runtime.NumberUtils;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class DisplayPanel extends JPanel {
    private final static int PIXEL_SIZE = 10;
    private final static int PIXEL_SIZE_PLUS_GAP = PIXEL_SIZE + 2;
    private final static int CELL_SIZE = 32;
    private final static int ROWS = 32;

    private final boolean[][] memory = new boolean[CELL_SIZE][CELL_SIZE];

    public DisplayPanel() {
        super.setBackground(Color.BLACK);
        super.setDoubleBuffered(true);
    }

    public void writeRow(Byte[] value, int row) {
        int number = NumberUtils.readInt(value, NumberUtils.Strategy.REVERSE_BITS);
        Boolean[] bits = String.format("%" + CELL_SIZE + "s", Integer.toBinaryString(number)).chars()
                .mapToObj(c -> c == '1')
                .collect(Collectors.toList())
                .toArray(new Boolean[0]);
        
        for (int i = 0; i < ROWS; i++) {
            memory[row][i] = bits[i];
        }
        repaint();
    }

    public void clear() {
        for (boolean[] memoryRow : memory) {
            for (int j = 0; j < memoryRow.length; j++) {
                memoryRow[j] = false;
            }
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
                          startX + (CELL_SIZE - j) * PIXEL_SIZE_PLUS_GAP,
                          startY + i * PIXEL_SIZE_PLUS_GAP,
                          PIXEL_SIZE,
                          PIXEL_SIZE
                  );
                } else {
                  g.setColor(Color.WHITE);
                  g.drawRect(
                          startX + (CELL_SIZE - j) * PIXEL_SIZE_PLUS_GAP,
                          startY + i * PIXEL_SIZE_PLUS_GAP,
                          PIXEL_SIZE,
                          PIXEL_SIZE
                  );
                }
            }
        }
    }

}
