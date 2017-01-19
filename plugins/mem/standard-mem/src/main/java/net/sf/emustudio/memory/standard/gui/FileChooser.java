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
package net.sf.emustudio.memory.standard.gui;

import emulib.runtime.UniversalFileFilter;
import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;

public class FileChooser {
    
    public static File selectFile(JDialog parent, String title) {
        JFileChooser f = new JFileChooser();
        UniversalFileFilter f1 = new UniversalFileFilter();
        UniversalFileFilter f2 = new UniversalFileFilter();

        f1.addExtension("hex");
        f1.addExtension("bin");
        f1.setDescription("Image file (*.hex, *.bin)");
        f2.addExtension("*");
        f2.setDescription("All files (*.*)");

        f.setDialogTitle(title);
        f.setAcceptAllFileFilterUsed(false);
        f.addChoosableFileFilter(f1);
        f.addChoosableFileFilter(f2);
        f.setFileFilter(f1);
        f.setApproveButtonText("Add");
        f.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = f.showOpenDialog(parent);
        f.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return f.getSelectedFile();
        }
        return null;
    }
    
    
}
