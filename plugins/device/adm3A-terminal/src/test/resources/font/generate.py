#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Peter Jakubčo
# SPDX-License-Identifier: GPL-3.0-or-later
"""Rebuild vector outlines from the ADM-3A character-generator row masks.

Run with FontForge's Python module installed:
python3 plugins/device/adm3A-terminal/src/test/resources/font/generate.py
The 5x7 character occupies a 7x9 cell. The operator's manual specifies a
0.075-inch width and 0.18-inch height; the widest glyph is therefore
685 by 1644 font units. Rounded scanline ends approximate the CRT spot;
the on/off dot positions come directly from the character masks.
"""

from pathlib import Path
import re
import shutil

import fontforge


DIRECTORY = Path(__file__).resolve().parent
X_PITCH = 137
Y_PITCH = 240
CELL_WIDTH = 7 * X_PITCH
ASCENT = 7 * Y_PITCH
SCANLINE_HEIGHT = 0.85 * Y_PITCH
BEZIER_CIRCLE = 0.5522847498307936


def scanline(pen, left, right, center):
    """Draw a video pulse with elliptical CRT spot caps."""
    rx = X_PITCH / 2
    ry = SCANLINE_HEIGHT / 2
    k = BEZIER_CIRCLE
    pen.moveTo((left + rx, center - ry))
    pen.lineTo((right - rx, center - ry))
    pen.curveTo((right - rx + k * rx, center - ry), (right, center - k * ry), (right, center))
    pen.curveTo((right, center + k * ry), (right - rx + k * rx, center + ry), (right - rx, center + ry))
    pen.lineTo((left + rx, center + ry))
    pen.curveTo((left + rx - k * rx, center + ry), (left, center + k * ry), (left, center))
    pen.curveTo((left, center - k * ry), (left + rx - k * rx, center - ry), (left + rx, center - ry))
    pen.closePath()


def join_rows(pen, previous, current, center):
    """Connect neighboring lit dots without changing the ROM row centers."""
    for column in range(5):
        if not current & (1 << (4 - column)):
            continue
        for above in range(max(0, column - 1), min(5, column + 2)):
            if previous & (1 << (4 - above)):
                pen.moveTo(((column + 1) * X_PITCH, center))
                pen.lineTo(((column + 2) * X_PITCH, center))
                pen.lineTo(((above + 2) * X_PITCH, center + Y_PITCH))
                pen.lineTo(((above + 1) * X_PITCH, center + Y_PITCH))
                pen.closePath()


def main():
    masks = {}
    for line in (DIRECTORY.parent / 'adm3a-charset.txt').read_text().splitlines():
        if line.startswith('#') or not line.strip():
            continue
        code, *rows = (int(value, 16) for value in line.split())
        assert len(rows) == 8 and rows[0] == 0 and all(0 <= row <= 31 for row in rows)
        masks[code] = rows
    assert set(masks) == set(range(32, 127))

    source = DIRECTORY / 'adm-3a.sfd'
    font = fontforge.open(str(source))
    font.layers[1].is_quadratic = False
    font.em = ASCENT
    font.ascent = ASCENT
    font.descent = 0
    for name in ('os2_typoascent', 'os2_winascent', 'hhea_ascent'):
        setattr(font, name, ASCENT)
    for name in ('os2_typodescent', 'os2_windescent', 'hhea_descent'):
        setattr(font, name, 0)
    font.version = '1.3'
    font.comment = ('ASCII dot positions verified against ADM-3A 2513 ROMs; '
                    'other characters retain emuStudio extensions. '
                    '5x7 glyphs, 7x9 cells, 137x240 dot pitch. '
                    'Connected vector strokes with rounded CRT spot caps. '
                    'Rebuild with generate.py; masks are in adm3a-charset.txt.')

    for glyph in font.glyphs():
        glyph.width = CELL_WIDTH
        # Non-standard characters keep their edited vector outlines in the SFD.
        if glyph.unicode not in masks:
            continue
        glyph.foreground = fontforge.layer()
        pen = glyph.glyphPen()
        for row, mask in enumerate(masks[glyph.unicode][1:], 1):
            center = (7.5 - row) * Y_PITCH
            join_rows(pen, masks[glyph.unicode][row - 1], mask, center)
            column = 0
            while column < 5:
                if not mask & (1 << (4 - column)):
                    column += 1
                    continue
                start = column
                while column < 5 and mask & (1 << (4 - column)):
                    column += 1
                scanline(pen, (start + 1) * X_PITCH, (column + 1) * X_PITCH,
                         center)
        del pen
        glyph.removeOverlap()
        glyph.correctDirection()
        glyph.width = CELL_WIDTH

    font.save(str(source))
    font.close()
    # Keep the embedded bitmap references aligned with the rebuilt vector grid.
    text = source.read_text()
    guides = []
    for x in range(8):
        guides.append(f'{x * X_PITCH} {-Y_PITCH} m 1\n {x * X_PITCH} {8 * Y_PITCH} l 1025\n')
    for y in range(9):
        guides.append(f'{-X_PITCH} {y * Y_PITCH} m 1\n {8 * X_PITCH} {y * Y_PITCH} l 1025\n')
    text = re.sub(r'^Grid\n.*?^EndSplineSet', 'Grid\n' + ''.join(guides) + 'EndSplineSet',
                  text, count=1, flags=re.MULTILINE | re.DOTALL)
    lines = text.splitlines()
    code = None
    for index, line in enumerate(lines):
        if line.startswith('Encoding:') and len(line.split()) == 4:
            code = int(line.split()[2])
        elif line.startswith('Image2:'):
            length = line.split()[2]
            # The legacy cedilla reference occupies bitmap columns 1..5.
            offset = 0 if code == 199 else X_PITCH
            lines[index] = f'Image2: image/png {length} {offset} {8 * Y_PITCH} {X_PITCH} {Y_PITCH}'
    text = '\n'.join(lines) + '\n'
    source.write_text(text)
    font = fontforge.open(str(source))
    font.generate(str(DIRECTORY / 'adm-3a.ttf'))
    font.close()
    shutil.copyfile(DIRECTORY / 'adm-3a.ttf', DIRECTORY.parents[2] /
                    'main/resources/net/emustudio/plugins/device/adm3a/gui/adm-3a.ttf')


if __name__ == '__main__':
    main()
