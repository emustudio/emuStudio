/*
 * emuFileFilter.java
 *
 * Created on Streda, 2007, august 8, 8:58
 *
 * KEEP IT SIMPLE, STUPID
 * some things just: YOU AREN'T GONNA NEED IT
 */

package gui.utils;

/**
 *
 * @author vbmacher
 */
public class emuFileFilter extends javax.swing.filechooser.FileFilter {
    private String[] exts;
    private String desc;
    
    /** Creates a new instance of emuFileFilter */
    public emuFileFilter() {
    }
    
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
                if (exts[i].equals(ext) || exts[i] == "*") return true;
        } else {
            for (int i = 0; i < exts.length; i++)
                if (exts[i] == "*") return true;
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
