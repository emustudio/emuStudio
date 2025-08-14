/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;

import java.awt.*;
import java.util.List;

import static net.emustudio.plugins.memory.bytemem.loaders.Loader.IMAGE_LOADERS;


public class Constants {
    public final static Color ROM_COLOR = new Color(0xE8, 0x68, 0x50);
    public final static Color BANK_COLOR = new Color(0xFF, 0xE6, 0xBF);

    public final static FileExtensionsFilter IMAGE_EXTENSION_FILTER = new FileExtensionsFilter(
            "Memory image", List.copyOf(IMAGE_LOADERS.keySet())
    );
}
