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

import net.emustudio.plugins.device.mits88dcdd.Command;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;

import java.io.IOException;

public class CpmfsCommand implements Command {

    @Argument(handler = SubCommandHandler.class, metaVar = "CPMFS command (cat, ls, volinfo, download, upload)", required = true)
    @SubCommands({
        @SubCommand(name = "cat", impl = CatSubCommand.class),
        @SubCommand(name = "ls", impl = ListSubCommand.class),
        @SubCommand(name = "volinfo", impl = InfoSubCommand.class),
        @SubCommand(name = "download", impl = DownloadSubCommand.class),
        @SubCommand(name = "upload", impl = UploadSubCommand.class)
    })
    CpmfsSubCommand subcommand;

    @Override
    public void execute(DriveIO driveIO) throws IOException {
        CpmFileSystem fileSystem = new CpmFileSystem(driveIO);
        subcommand.execute(fileSystem);
    }

    interface CpmfsSubCommand {
        void execute(CpmFileSystem fileSystem) throws IOException;
    }
}
