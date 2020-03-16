/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
package net.emustudio.plugins.memory.standard.gui;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FileChooser {

    public static File selectFile(JDialog parent, String title) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter f1 = new FileNameExtensionFilter("Image file (*.hex, *.bin)", "bin", "hex");
        FileNameExtensionFilter f2 = new FileNameExtensionFilter("All files (*.*)", "*");

        fileChooser.setDialogTitle(title);
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.addChoosableFileFilter(f1);
        fileChooser.addChoosableFileFilter(f2);
        fileChooser.setFileFilter(f1);
        fileChooser.setApproveButtonText("Add");
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));

        int returnVal = fileChooser.showOpenDialog(parent);
        fileChooser.setVisible(true);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return fileChooser.getSelectedFile();
        }
        return null;
    }
}
