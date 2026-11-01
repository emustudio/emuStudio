/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** A captured logical bank and its portable, image-bound user metadata. */
public final class MetadataExport {
    private MetadataExport() {}

    public static void export(
            Path directory,
            MemoryAnalysis image,
            AnnotationIndex annotations,
            String processor,
            int bank,
            int base)
            throws IOException {
        if (base < 0 || (long) base + image.size() > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Export exceeds supported address range");
        if (processor == null || processor.isBlank())
            throw new IllegalArgumentException("Processor identifier is required");
        byte[] bytes = image.bytes(bank);
        Files.createDirectories(directory);
        Path binary = directory.resolve("memory-bank-" + bank + ".bin");
        Files.write(binary, bytes);
        Properties p = annotations.properties();
        p.setProperty("processor", processor);
        p.setProperty("base", Integer.toString(base));
        p.setProperty("bank", Integer.toString(bank));
        p.setProperty("size", Integer.toString(image.size()));
        p.setProperty("common", Integer.toString(image.common));
        p.setProperty("binary", binary.getFileName().toString());
        AtomicProperties.save(directory.resolve("memory-bank-" + bank + ".properties"), p);
    }
}
