/*
 * ImageFilter.java
 *
 * Created on Piatok, 2008, februar 8, 19:48
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 *
 * Copyright (C) 2008-2010 Peter Jakubčo <pjakubco at gmail.com>
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

package disk_88.gui;

/**
 *
 * @author vbmacher
 */
public class ImageFilter extends javax.swing.filechooser.FileFilter {
    private String[] exts;
    private String desc;
    
    public void addExtension(String ext) {
        int l=0;
        String[] tmp;
        if (exts != null) l = exts.length;
        tmp = new String[l+1];
        if (exts != null) System.arraycopy(exts,0,tmp,0,l);
        tmp[l] = ext;
        exts = tmp;
    }

    public String getFirstExtension() {
        if (exts != null) return exts[0];
        return null;
    }

    public boolean accept(java.io.File f) {
        if (f.isDirectory()) return true;
        String ext = this.getExtension(f);
        if (ext != null) {
            for (int i = 0; i < exts.length; i++)
                if (exts[i].equals(ext) || exts[i].equals("*")) return true;
        } else {
            for (int i = 0; i < exts.length; i++)
                if (exts[i].equals("*")) return true;
        }
        return false;
    }

    public String getExtension(java.io.File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');
        if (i > 0 &&  i < s.length() - 1) 
            ext = s.substring(i+1).toLowerCase();
        return ext;
    }

    public String getDescription() {
        return desc;
    }

    public void setDescription(String des) {
        desc = des;
    }
}
