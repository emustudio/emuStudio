/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88dcdd.cmdline;

import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFileSystem;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.CpmFormat;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.DriveIO;
import net.emustudio.plugins.device.mits88dcdd.cpmfs.entry.CpmFile;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

@SuppressWarnings("unused")
@Command(name = "cpmfs", aliases = {"cpm"}, description = "CP/M filesystem commands")
public class Cpmfs {
    @ParentCommand
    private Runner runner;

    @Command(header = "Show file content", name = "cat", description = "Prints the file in CP/M disk image")
    public void cat(
            @Parameters(paramLabel = "FILE", description = "file name in CP/M disk image")
            String fileName) throws IOException {
        System.out.println(cpmfs().readFile(cpmfile(fileName)));
    }

    @Command(header = "List files", name = "list", aliases = {"ls"}, description = "Prints all valid files in CP/M disk image")
    public void ls() {
        System.out.println(CpmFile.getLongHeader());
        cpmfs()
                .listExistingFiles()
                .forEach(f -> System.out.println(f.toLongString()));
    }

    @Command(header = "Show files dates", name = "dates", description = "Prints all valid file names in CP/M disk image with dates")
    public void dates() {
        System.out.printf("%12s | %16s | %16s | %16s%n", "File name", "Create", "Modify", "Access");
        System.out.println("-------------+------------------+------------------+-----------------");
        cpmfs().listNativeDates().forEach(System.out::println);
    }

    @Command(header = "Copy files", name = "copy", aliases = {"cp"}, description = "Copy a file between CP/M disk image and host")
    public void copy(@Parameters(paramLabel = "SRC_FILE", index = "0", description = "source file (cpm:// prefix if in CP/M disk image)")
                     String src,
                     @Parameters(paramLabel = "DST_FILE", index = "1", description = "destination file (cpm:// prefix if in CP/M disk image")
                     String dst) throws IOException {
        boolean srcInCpm = src.toLowerCase(Locale.ENGLISH).startsWith("cpm://");
        boolean dstInCpm = dst.toLowerCase(Locale.ENGLISH).startsWith("cpm://");
        String realSrc = srcInCpm ? src.substring(6) : src;
        String realDst = dstInCpm ? dst.substring(6) : (dst.equals(".") ? realSrc : dst);
        if (dst.equals(".") && !srcInCpm) {
            // assume then dst is in CP/M
            dstInCpm = true;
        }
        if (!srcInCpm && !dstInCpm) {
            throw new IllegalArgumentException("Either SRC_FILE or DST_FILE must start with cpm:// (or DST_FILE must be a \".\")");
        }

        CpmFileSystem cpmfs = cpmfs();
        byte[] content;
        if (srcInCpm) {
            content = cpmfs.readFileBytes(realSrc);
        } else {
            content = Files.readAllBytes(Path.of(realSrc));
        }

        if (dstInCpm) {
            cpmfs.writeFileBytes(realDst, content);
        } else {
            Files.write(Path.of(realDst), content);
        }
    }

    @Command(header = "Volume information", name = "info", description = "Prints volume information")
    public void info() {
        CpmFileSystem cpmfs = cpmfs();
        System.out.println(cpmfs.cpmFormat);
        System.out.println("Disc label: " + cpmfs.getLabel());
        System.out.println("Number of files: " + cpmfs.listExistingFiles().count());

        List<String> passwords = cpmfs.listPasswords();
        if (!passwords.isEmpty()) {
            System.out.println("File passwords:");
            passwords.forEach(System.out::println);
        }
    }

    @Command(header = "Remove file", name = "remove", aliases = {"rm"}, description = "Remove file from CP/M disk image")
    public void rm(@Parameters(paramLabel = "FILE", description = "file name in CP/M disk image")
                   String fileName) {
        cpmfs().removeFile(cpmfile(fileName));
    }

    @Command(header = "Format CP/M disk image", name = "format", description = "Formats (completely erase) CP/M disk image")
    public void format() throws IOException {
        CpmFormat cpmFormat = runner.findFormat();
        DriveIO.format(runner.exclusive.dependent.imageFile, cpmFormat);
    }


    private CpmFileSystem cpmfs() {
        CpmFormat cpmFormat = runner.findFormat();
        try {
            return new CpmFileSystem(new DriveIO(runner.exclusive.dependent.imageFile, cpmFormat, READ, WRITE));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String cpmfile(String name) {
        return name.toLowerCase(Locale.ENGLISH).startsWith("cpm://") ? name.substring(6) : name;
    }
}
