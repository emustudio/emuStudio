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
package net.emustudio.plugins.device.mits88dcdd;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.commands.CpmfsCommand;
import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import static net.emustudio.plugins.device.mits88dcdd.cpmfs.Formats.FORMATS;
import static net.emustudio.plugins.device.mits88dcdd.gui.Constants.DIALOG_TITLE;

public class CommandLine {
    private final static Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);

    @Option(name = "--image", usage = "use given disk image", metaVar = "fileName", required = true)
    String imageFile;

    @Option(name = "--format", usage = "disk format (default 'altair_floppy_cpm2')", metaVar = "X")
    String format = "altair_floppy_cpm2";

    @Option(name = "--help", help = true, usage = "output this message")
    private boolean help = false;

    @Argument(handler = SubCommandHandler.class, required = true, metaVar = "disk command (cpmfs)")
    @SubCommands(
        @SubCommand(impl = CpmfsCommand.class, name = "cpmfs")
    )
    Command command;

    public static Optional<Runnable> parse(String[] args) {
        CommandLine commandLine = new CommandLine();

        CmdLineParser parser = new CmdLineParser(commandLine, ParserProperties.defaults().withUsageWidth(120));
        try {
            parser.parseArgument(args);

            if (commandLine.help) {
                System.out.println(DIALOG_TITLE + " emuStudio plug-in, version " + Resources.getVersion());

                parser.printUsage(System.err);
                System.exit(0);
            }

            return Optional.of(commandLine.createCommand());
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            return Optional.empty();
        }
    }

    private Runnable createCommand() {
        return () -> {
            CpmFormat cpmFormat = FORMATS.get(format);
            try(DriveIO driveIO = new DriveIO(Path.of(imageFile), cpmFormat, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
                command.execute(driveIO);
            } catch (Exception e) {
                LOGGER.error("Could not run disk command", e);
            }
        };
    }
}
