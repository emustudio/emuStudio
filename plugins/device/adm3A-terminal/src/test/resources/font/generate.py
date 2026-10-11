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
CP437 symbols are emulator extensions. Box and block outlines fill the em
square vertically; DisplayCanvas stretches them over the full display cell.
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

# CP437 bytes 179..218: left, right, up, down; 0 = absent, 1 = single, 2 = double.
BOX_ARMS = (
    '0011', '1011', '2011', '1022', '1002', '2001', '2022', '0022',
    '2002', '2020', '1020', '2010', '1001', '0110', '1110', '1101',
    '0111', '1100', '1111', '0211', '0122', '0220', '0202', '2220',
    '2202', '0222', '2200', '2222', '2210', '1120', '2201', '1102',
    '0120', '0210', '0201', '0102', '1122', '2211', '1010', '0101',
)

# New non-Latin symbols use the same 5x7 dot grid as the ASCII characters.
SYMBOL_ROWS = {
    '₧': '1c 12 1c 10 17 14 17', 'ƒ': '06 08 08 1c 08 08 10',
    '⌐': '00 00 1f 10 10 00 00', 'α': '00 00 0d 12 12 12 0d',
    'Γ': '1f 10 10 10 10 10 10', 'π': '00 00 1f 0a 0a 0a 0b',
    'Σ': '1f 10 08 04 08 10 1f', 'σ': '00 00 0f 12 12 12 0c',
    'τ': '00 00 1f 04 04 04 03', 'Φ': '04 0e 15 15 15 0e 04',
    'Θ': '0e 11 11 1f 11 11 0e', 'Ω': '0e 11 11 11 0a 0a 1b',
    'δ': '06 08 04 0e 11 11 0e', '∞': '00 00 0a 15 15 0a 00',
    'φ': '00 04 0e 15 15 0e 04', 'ε': '00 00 0e 10 0c 10 0e',
    '∩': '00 0e 11 11 11 11 00', '≡': '00 1f 00 1f 00 1f 00',
    '≥': '10 08 04 08 10 00 1f', '≤': '01 02 04 02 01 00 1f',
    '⌠': '06 09 08 08 08 08 08', '⌡': '02 02 02 02 02 12 0c',
    '≈': '00 09 16 00 09 16 00', '∙': '00 00 00 04 00 00 00',
    '√': '01 01 02 12 0a 04 04', 'ⁿ': '0e 09 09 09 00 00 00',
    '■': '00 00 0e 0e 0e 00 00',
}


def rectangle(pen, left, bottom, right, top):
    pen.moveTo((left, bottom))
    pen.lineTo((right, bottom))
    pen.lineTo((right, top))
    pen.lineTo((left, top))
    pen.closePath()


def draw_box(pen, arms):
    left, right, up, down = (int(style) for style in arms)
    cx, cy = CELL_WIDTH / 2, ASCENT / 2

    def offsets(style):
        return (-X_PITCH, X_PITCH) if style == 2 else (0,)

    def join(offset, direction, first, second):
        if max(first, second) != 2:
            return 0
        if first and second:
            return direction * X_PITCH
        if offset == 0:
            return -direction * X_PITCH
        return direction * (1 if first else -1) * (1 if offset > 0 else -1) * X_PITCH

    for direction, style in ((-1, left), (1, right)):
        if style:
            thickness = X_PITCH * (0.65 if style == 2 else 1)
            for offset in offsets(style):
                endpoint = cx + join(offset, direction, up, down)
                edge = 0 if direction == -1 else CELL_WIDTH
                rectangle(pen, min(edge, endpoint - direction * thickness / 2), cy + offset - thickness / 2,
                          max(edge, endpoint - direction * thickness / 2), cy + offset + thickness / 2)
    for direction, style in ((1, up), (-1, down)):
        if style:
            thickness = X_PITCH * (0.65 if style == 2 else 1)
            for offset in offsets(style):
                endpoint = cy + join(offset, direction, right, left)
                edge = ASCENT if direction == 1 else 0
                rectangle(pen, cx + offset - thickness / 2, min(edge, endpoint - direction * thickness / 2),
                          cx + offset + thickness / 2, max(edge, endpoint - direction * thickness / 2))


def draw_extensions(font):
    for index, arms in enumerate(BOX_ARMS, 179):
        character = bytes([index]).decode('cp437')
        glyph = font.createChar(ord(character))
        glyph.foreground = fontforge.layer()
        pen = glyph.glyphPen()
        draw_box(pen, arms)
        del pen
        glyph.removeOverlap()
        glyph.correctDirection()
        glyph.width = CELL_WIDTH

    for index in range(176, 179):
        glyph = font.createChar(ord(bytes([index]).decode('cp437')))
        glyph.foreground = fontforge.layer()
        pen = glyph.glyphPen()
        for row in range(14):
            for column in range(7):
                if (column + 2 * row) % 4 < index - 175:
                    rectangle(pen, column * X_PITCH, row * ASCENT / 14,
                              (column + 1) * X_PITCH, (row + 1) * ASCENT / 14)
        del pen
        glyph.width = CELL_WIDTH

    for character, bounds in {
        '█': (0, 0, CELL_WIDTH, ASCENT), '▄': (0, 0, CELL_WIDTH, ASCENT / 2),
        '▌': (0, 0, CELL_WIDTH / 2, ASCENT), '▐': (CELL_WIDTH / 2, 0, CELL_WIDTH, ASCENT),
        '▀': (0, ASCENT / 2, CELL_WIDTH, ASCENT),
    }.items():
        glyph = font.createChar(ord(character))
        glyph.foreground = fontforge.layer()
        pen = glyph.glyphPen()
        rectangle(pen, *bounds)
        del pen
        glyph.width = CELL_WIDTH

    for character, rows in SYMBOL_ROWS.items():
        glyph = font.createChar(ord(character))
        draw_character(glyph, [0] + [int(row, 16) for row in rows.split()])

    supported = {glyph.unicode for glyph in font.glyphs()}
    assert all(ord(character) in supported for character in bytes(range(128, 256)).decode('cp437'))


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


def draw_character(glyph, rows):
    glyph.foreground = fontforge.layer()
    pen = glyph.glyphPen()
    for row, mask in enumerate(rows[1:], 1):
        center = (7.5 - row) * Y_PITCH
        join_rows(pen, rows[row - 1], mask, center)
        column = 0
        while column < 5:
            if not mask & (1 << (4 - column)):
                column += 1
                continue
            start = column
            while column < 5 and mask & (1 << (4 - column)):
                column += 1
            scanline(pen, (start + 1) * X_PITCH, (column + 1) * X_PITCH, center)
    del pen
    glyph.removeOverlap()
    glyph.correctDirection()
    glyph.width = CELL_WIDTH


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
    font.version = '1.4'
    font.comment = ('ASCII dot positions verified against ADM-3A 2513 ROMs; '
                    'Other characters retain emuStudio extensions, including CP437 box drawing. '
                    '5x7 glyphs, 7x9 cells, 137x240 dot pitch. '
                    'Connected vector strokes with rounded CRT spot caps. '
                    'Rebuild with generate.py; masks are in adm3a-charset.txt.')

    for glyph in font.glyphs():
        glyph.width = CELL_WIDTH
        # Non-standard characters keep their edited vector outlines in the SFD.
        if glyph.unicode not in masks:
            continue
        draw_character(glyph, masks[glyph.unicode])

    draw_extensions(font)

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
