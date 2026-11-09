/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.io.FileLoader;
import net.emustudio.emulib.io.FileType;

import java.awt.*;
import java.util.Arrays;
import java.util.List;



public class Constants {
    public final static Color ROM_COLOR = new Color(0xE8, 0x68, 0x50);
    public final static Color BANK_COLOR = new Color(0xFF, 0xE6, 0xBF);

    public final static List<FileType> IMAGE_FILE_TYPES = List.of(new FileType(
            "Memory image", Arrays.stream(FileLoader.Format.values()).flatMap(f -> f.getExtensions().stream()).toArray(String[]::new)
    ));
}
