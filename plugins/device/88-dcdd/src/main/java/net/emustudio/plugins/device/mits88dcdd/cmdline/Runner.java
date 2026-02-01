/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cmdline;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.plugins.device.mits88dcdd.Resources;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

@SuppressWarnings("unused")
@Command(
        name = "88-dcdd",
        mixinStandardHelpOptions = true,
        versionProvider = Runner.VersionProvider.class,
        description = "88-DCDD Altair floppy disk drive",
        scope = CommandLine.ScopeType.INHERIT,
        subcommands = {Cpmfs.class}
)
public class Runner implements Runnable {

    @CommandLine.ArgGroup(multiplicity = "1")
    public Exclusive exclusive;
    public CpmFileSystem cpmfs;
    @Option(names = {"-F", "--format-file"}, description = "disk format file (TOML)", paramLabel = "FILE")
    private Path formatFile = new File(System.getProperty("user.dir"))
            .toPath()
            .resolve("examples")
            .resolve("altair8800")
            .resolve("cpm-formats.toml");

    public static void main(String[] args) {
        CommandLine cmdline = new CommandLine(new Runner());
        cmdline.registerConverter(Path.class, Path::of);
        cmdline.getCommandSpec().parser().collectErrors(true);

        CommandLine.ParseResult result = cmdline.parseArgs(args);
        try {
            cmdline.execute(args);
        } catch (Exception e) {
            result.errors().forEach(System.err::println);
            System.exit(1);
        }
    }

    @Override
    public void run() {
        Optional
                .ofNullable(exclusive)
                .ifPresent(e -> {
                    if (e.listFormats) {
                        loadFormats().stream().map(f -> f.id).forEach(System.out::println);
                        return;
                    }
                    CpmFormat cpmFormat = findFormat();
                    try {
                        cpmfs = new CpmFileSystem(new DriveIO(exclusive.dependent.imageFile, cpmFormat, READ, WRITE));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
    }

    public List<CpmFormat> loadFormats() {
        try (FileConfig config = FileConfig.of(formatFile)) {
            config.load();
            return CpmFormat.fromConfig(config);
        }
    }

    public CpmFormat findFormat() {
        Optional<CpmFormat> format = loadFormats()
                .stream()
                .filter(f -> f.id.equals(exclusive.dependent.formatId))
                .findAny();
        if (format.isEmpty()) {
            throw new IllegalArgumentException("Unknown CP/M disk format ID!");
        }
        return format.get();
    }

    public static class VersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{Resources.getVersion()};
        }
    }

    static class Exclusive {
        @CommandLine.ArgGroup(exclusive = false, multiplicity = "1")
        public Dependent dependent;
        @Option(names = {"-l", "--list-formats"}, description = "lists available disk format IDs")
        private boolean listFormats;
    }

    static class Dependent {
        @Option(names = {"-f", "--format"}, description = "disk format ID", required = true, paramLabel = "FORMAT")
        public String formatId;

        @Option(names = {"-i", "--image"}, description = "disk image file", paramLabel = "FILE", required = true)
        public Path imageFile;
    }
}
