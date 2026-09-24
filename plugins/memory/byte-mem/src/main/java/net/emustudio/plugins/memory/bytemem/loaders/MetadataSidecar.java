/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Comparator;
import java.util.Map;

public final class MetadataSidecar {
    private static final String HEADER = "# emuStudio memory metadata v1";

    private MetadataSidecar() {
    }

    public static void load(Path imagePath, ByteMemoryContext memory) throws IOException {
        Path sidecar = pathFor(imagePath);
        if (!Files.isRegularFile(sidecar)) {
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(sidecar, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] fields = line.split("\\t", -1);
                if (fields.length != 5) {
                    throw new IOException("Invalid metadata sidecar line " + lineNumber);
                }
                try {
                    int address = Integer.parseInt(fields[0]);
                    long pluginId = Long.parseLong(fields[1]);
                    int sourceLine = Integer.parseInt(fields[2]);
                    int column = Integer.parseInt(fields[3]);
                    String fileName = new String(Base64.getUrlDecoder().decode(fields[4]), StandardCharsets.UTF_8);
                    if (address >= 0 && address < memory.getSize()) {
                        memory.annotations().add(address, new SourceCodeAnnotation(
                                pluginId, SourceCodePosition.of(sourceLine, column, fileName)
                        ));
                    }
                } catch (IllegalArgumentException e) {
                    throw new IOException("Invalid metadata sidecar line " + lineNumber, e);
                }
            }
        }
    }

    public static void save(Path imagePath, ByteMemoryContext memory) throws IOException {
        Path sidecar = pathFor(imagePath);
        try (BufferedWriter writer = Files.newBufferedWriter(sidecar, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();
            try {
                memory.annotations().getAll(SourceCodeAnnotation.class).entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .forEach(entry -> entry.getValue().stream()
                                .sorted(Comparator.comparingLong(SourceCodeAnnotation::getPluginId))
                                .forEach(annotation -> write(writer, entry.getKey(), annotation)));
            } catch (SidecarWriteException e) {
                throw (IOException) e.getCause();
            }
        }
    }

    public static Path pathFor(Path imagePath) {
        return imagePath.resolveSibling(imagePath.getFileName() + ".meta");
    }

    private static void write(BufferedWriter writer, int address, SourceCodeAnnotation annotation) {
        SourceCodePosition position = annotation.getPosition();
        String fileName = Base64.getUrlEncoder().withoutPadding().encodeToString(
                position.fileName.getBytes(StandardCharsets.UTF_8)
        );
        try {
            writer.write(address + "\t" + annotation.getPluginId() + "\t" + position.line + "\t" +
                    position.column + "\t" + fileName);
            writer.newLine();
        } catch (IOException e) {
            throw new SidecarWriteException(e);
        }
    }

    private static final class SidecarWriteException extends RuntimeException {
        private SidecarWriteException(IOException cause) {
            super(cause);
        }
    }
}
