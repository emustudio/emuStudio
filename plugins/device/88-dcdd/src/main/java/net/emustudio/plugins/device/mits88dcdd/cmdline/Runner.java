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
package net.emustudio.plugins.device.mits88dcdd.cmdline;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.emustudio.plugins.device.mits88dcdd.Resources;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

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
    subcommands = { Cpmfs.class }
)
public class Runner implements Runnable {

    @Option(names = {"-F", "--format-file"}, description = "disk format file (TOML)", paramLabel = "FILE")
    private Path formatFile = new File(System.getProperty("user.dir"))
        .toPath()
        .resolve("examples")
        .resolve("altair8800")
        .resolve("cpm-formats.toml");

    @Option(names = {"-f", "--format"}, description = "disk format ID", required = true, paramLabel = "FORMAT")
    private String formatId;

    // TODO: exclusive
    @Option(names = {"-l", "--list-formats"}, description = "lists available disk format IDs")
    private boolean listFormats;

    @Option(names = {"-i", "--image"}, description = "disk image file", paramLabel = "FILE", required = true)
    Path imageFile;

    public CpmFileSystem cpmfs;

    public static void main(String[] args) {
        CommandLine cmdline = new CommandLine(new Runner());
        cmdline.registerConverter(Path.class, Path::of);
        cmdline.getCommandSpec().parser().collectErrors(true);

        CommandLine.ParseResult result = cmdline.parseArgs(args);

        try {
            cmdline.execute(args);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    @Override
    public void run() {
        if (listFormats) {
            loadFormats().forEach(System.out::println);
        }
        CpmFormat cpmFormat = findFormat().orElseThrow();
        try {
            cpmfs = new CpmFileSystem(new DriveIO(imageFile, cpmFormat, READ, WRITE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<CpmFormat> loadFormats() {
        try (FileConfig config = FileConfig.of(formatFile)) {
            config.load();
            return CpmFormat.fromConfig(config);
        }
    }

    public Optional<CpmFormat> findFormat() {
        return loadFormats().stream().filter(f -> f.id.equals(formatId)).findAny();
    }

    public static class VersionProvider implements IVersionProvider {

        @Override
        public String[] getVersion() {
            return new String[]{Resources.getVersion()};
        }
    }
}