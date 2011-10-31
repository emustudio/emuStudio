/*
 * EmuFileFilter.java
 *
 * Created on Streda, 2007, august 8, 8:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2007-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package emustudio.gui.utils;

/**
 * File filter used in emuStudio.
 * 
 * @author vbmacher
 */
public class EmuFileFilter extends javax.swing.filechooser.FileFilter {
    private String[] exts;
    private String desc;

    /**
     * Creates new instance of the class.
     */
    public EmuFileFilter() {
        exts = new String[0];
    }

    /**
     * Add an extension to be recognized by this file filter. The asterisk ("*")
     * can be also recognized.
     * 
     * @param ext extension string, without the "." (dot)
     */
    public void addExtension(String ext) {
        int l=0;
        String[] tmp;
        if (exts != null) l = exts.length;
        tmp = new String[l+1];
        if (exts != null) System.arraycopy(exts,0,tmp,0,l);
        tmp[l] = ext;
        exts = tmp;
    }

    /**
     * Get first extension (usually the default one) of this file filter.
     * 
     * @return first extension accepted by this file filter
     */
    public String getFirstExtension() {
        if (exts != null)
            return exts[0];
        return null;
    }

    /**
     * Determine if given file is accepted by this file filter.
     * 
     * @param f a file
     * @return true if the file is accepted, false otherwise
     */
    @Override
    public boolean accept(java.io.File f) {
        if (f.isDirectory()) return true;
        String ext = getExtension(f);
        if (ext != null) {
            for (int i = 0; i < exts.length; i++)
                if (exts[i].equals(ext) || exts[i].equals("*")) return true;
        } else {
            for (int i = 0; i < exts.length; i++)
                if (exts[i].equals("*")) return true;
        }
        return false;
    }

    /**
     * Retrieve and return file extension.
     *
     * @param f Java File object
     * @return extension of the file. If the file has no extension, null is
     * returned.
     */
    public static String getExtension(java.io.File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 &&  i < s.length() - 1) 
            ext = s.substring(i+1).toLowerCase();
        return ext;
    }

    /**
     * Get description of this file type.
     * 
     * @return description of the file type
     */
    @Override
    public String getDescription() {
        return desc;
    }

    /**
     * Set description of this file type.
     * 
     * @param des description of the file type
     */
    public void setDescription(String des) {
        desc = des;
    }

    /**
     * Get extensions count within this filter.
     *
     * @return number of extensions acceptable by this filter
     */
    public int getExtensionsCount() {
        return exts.length;
    }
}
