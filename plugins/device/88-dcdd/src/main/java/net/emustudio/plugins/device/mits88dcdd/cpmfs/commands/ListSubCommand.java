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
package net.emustudio.plugins.device.mits88dcdd.cpmfs.commands;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFile;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;

import java.io.IOException;
import java.util.Collection;

public class ListSubCommand implements CpmfsCommand.CpmfsSubCommand {

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        printFiles(fileSystem.listValidFiles());
    }

    private static void printFiles(Collection<CpmFile> files) {
        for (CpmFile file : files) {
            System.out.println(file.toString());
        }
    }
}
