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
    @Option(name = "--blocklen", usage = "block length in bytes (default " + CpmFileSystem.BLOCK_LENGTH + ")", metaVar = "X")
    int blockLength = CpmFileSystem.BLOCK_LENGTH;

    @Option(name = "--dirtrack", usage = "directory track number (default " + CpmFileSystem.DIRECTORY_TRACK + ")", metaVar = "X")
    int directoryTrack = CpmFileSystem.DIRECTORY_TRACK;

    @Option(name = "--blockptrs2", usage = "use 2 bytes per block pointer (default "
        + (CpmFileSystem.BLOCKS_ARE_TWO_BYTES ? "2 bytes" : "1 byte") + ")")
    boolean blocksAreTwoBytes = CpmFileSystem.BLOCKS_ARE_TWO_BYTES;

    @Argument(handler = SubCommandHandler.class, metaVar = "CPMFS command (cat, ls, volinfo)", required = true)
    @SubCommands({
        @SubCommand(name = "cat", impl = CatSubCommand.class),
        @SubCommand(name = "ls", impl = ListSubCommand.class),
        @SubCommand(name = "volinfo", impl = InfoSubCommand.class),
        @SubCommand(name = "download", impl = DownloadSubCommand.class)
    })
    CpmfsSubCommand subcommand;

    @Override
    public void execute(DriveIO driveIO) throws IOException {
        CpmFileSystem fileSystem = new CpmFileSystem(driveIO, directoryTrack, blockLength, blocksAreTwoBytes);
        subcommand.execute(fileSystem);
    }

    interface CpmfsSubCommand {
        void execute(CpmFileSystem fileSystem) throws IOException;
    }
}
