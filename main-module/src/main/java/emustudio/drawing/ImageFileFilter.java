/*
 * KISS, YAGNI, DRY
 *
 * (c) Copyright 2006-2017, Peter Jakubčo
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
package emustudio.drawing;

import javax.swing.filechooser.FileFilter;
import java.io.File;

class ImageFileFilter extends FileFilter {
    private final String formatName;
    private final String suffix;

    ImageFileFilter(String formatName, String suffix) {
        this.formatName = formatName;
        this.suffix = suffix;
    }

    @Override
    public boolean accept(File f) {
        return f.isDirectory() || f.getName().toUpperCase().endsWith("." + suffix.toUpperCase());
    }

    @Override
    public String getDescription() {
        return formatName + " image";
    }

    String getFormatName() {
        return formatName;
    }

    String getSuffix() {
        return suffix;
    }

}
