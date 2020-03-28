package net.emustudio.plugins.device.mits88disk;

import net.emustudio.plugins.device.mits88disk.cpmfs.DriveIO;
import net.emustudio.plugins.device.mits88disk.cpmfs.commands.CpmfsCommand;
import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.SubCommand;
import org.kohsuke.args4j.spi.SubCommandHandler;
import org.kohsuke.args4j.spi.SubCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.ResourceBundle;

public class CommandLine {
    private final static Logger LOGGER = LoggerFactory.getLogger(CommandLine.class);

    @Option(name = "--image", usage = "use given disk image", metaVar = "fileName", required = true)
    String imageFile;

    @Option(name = "--sectors", usage = "sectors per track (default " + DriveIO.SECTORS_PER_TRACK + ")", metaVar = "X")
    int sectorsPerTrack = DriveIO.SECTORS_PER_TRACK;

    @Option(name = "--sectorsize", usage = "sector size in bytes (default " + DriveIO.SECTOR_SIZE + ")", metaVar = "X")
    int sectorSize = DriveIO.SECTOR_SIZE;

    @Option(name = "--sectorskew", usage = "sector skew in bytes (default " + DriveIO.SECTOR_SKEW + ")", metaVar = "X")
    int sectorSkew = DriveIO.SECTOR_SKEW;

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
                System.out.println("MITS 88-DISK emuStudio plug-in, version " + getVersion());

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
            try {
                DriveIO driveIO = new DriveIO(
                    Path.of(imageFile), sectorSize, sectorsPerTrack, sectorSkew, StandardOpenOption.READ
                );
                command.execute(driveIO);
            } catch (IOException e) {
                LOGGER.error("Could not run disk command", e);
            }
        };
    }

    private static String getVersion() {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("net.sf.net.emustudio.devices.mits88disk.version");
            return bundle.getString("version");
        } catch (MissingResourceException e) {
            return "(unknown)";
        }
    }
}
