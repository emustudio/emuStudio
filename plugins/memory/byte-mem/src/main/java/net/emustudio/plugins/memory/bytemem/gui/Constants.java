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
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.runtime.interaction.FileExtensionsFilter;

import java.awt.*;


public class Constants {
    public final static Font MEMORY_CELLS_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    public final static FileExtensionsFilter IMAGE_EXTENSION_FILTER = new FileExtensionsFilter(
        "Memory image", "hex", "bin", "com", "out"
    );
}
