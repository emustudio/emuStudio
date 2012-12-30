/*
 * EmuFileFilter.java
 *
 * Created on Streda, 2007, august 8, 8:58
 *
 * Copyright (C) 2007-2012 Peter Jakubčo
 * KISS, YAGNI, DRY
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
package net.sf.emustudio.memory.standard.gui.utils;

import java.io.File;
import javax.swing.filechooser.FileFilter;

public class EmuFileFilter extends FileFilter {
    private String[] extensions;
    private String description;

    public void addExtension(String extension) {
        int length = 0;
        String[] tmp;
        if (extensions != null) {
            length = extensions.length;
        }
        tmp = new String[length + 1];
        if (extensions != null) {
            System.arraycopy(extensions, 0, tmp, 0, length);
        }
        tmp[length] = extension;
        extensions = tmp;
    }

    public String getFirstExtension() {
        if (extensions != null) {
            return extensions[0];
        }
        return null;
    }

    @Override
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }
        String ext = this.getExtension(f);
        if (ext != null) {
            for (String extension : extensions) {
                if (extension.equals(ext) || extension.equals("*")) {
                    return true;
                }
            }
        } else {
            for (String extension : extensions) {
                if (extension.equals("*")) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getExtension(File file) {
        String extension = null;
        String s = file.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            extension = s.substring(i + 1).toLowerCase();
        }
        return extension;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
