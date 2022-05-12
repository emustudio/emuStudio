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

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import org.kohsuke.args4j.Argument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class DownloadSubCommand implements CpmfsCommand.CpmfsSubCommand {
    private final static Logger LOGGER = LoggerFactory.getLogger(DownloadSubCommand.class);

    @Argument(required = true, metaVar = "source file name (in the disk image)", index = 0)
    String srcFileName;

    @Argument(metaVar = "destination file name (on host) - use \".\" (dot) to use the same name", index = 1)
    String dstFileName = ".";

    @Override
    public void execute(CpmFileSystem fileSystem) throws IOException {
        fileSystem.readContent(srcFileName).ifPresent(content -> {
            String realDstFileName = dstFileName.equals(".") ? srcFileName : dstFileName;
            try (Writer writer = new FileWriter(realDstFileName)) {
                writer.write(content);
            } catch (IOException e) {
                LOGGER.error("Could not write file content", e);
            }
        });
    }
}
