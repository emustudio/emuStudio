/*
 * KISS, DRY, YAGNI
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package emustudio.drawing;

import java.io.File;
import javax.swing.filechooser.FileFilter;

class ImageFileFilter extends FileFilter {
    private final String formatName;
    private final String suffix;

    public ImageFileFilter(String formatName, String suffix) {
        this.formatName = formatName;
        this.suffix = suffix;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        return f.getName().toUpperCase().endsWith("." + suffix.toUpperCase());
    }

    @Override
    public String getDescription() {
        return formatName + " image";
    }

    public String getFormatName() {
        return formatName;
    }

    public String getSuffix() {
        return suffix;
    }

}
