/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.plugins.compiler.SourceCodePosition;
import net.emustudio.emulib.plugins.memory.annotations.SourceCodeAnnotation;
import net.emustudio.plugins.memory.bytemem.MemoryContextImpl;
import net.emustudio.plugins.memory.bytemem.TestMemoryContextFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MetadataSidecarTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void savesAndLoadsSourceCodeAnnotations() throws IOException {
        Path image = temporaryFolder.newFile("memory.bin").toPath();
        MemoryContextImpl source = TestMemoryContextFactory.create(256, 1, 0);
        SourceCodePosition position = SourceCodePosition.of(12, 3, "/tmp/příliš.asm");
        source.annotations().add(42, new SourceCodeAnnotation(7, position));

        MetadataSidecar.save(image, source);

        MemoryContextImpl target = TestMemoryContextFactory.create(256, 1, 0);
        MetadataSidecar.load(image, target);
        SourceCodeAnnotation annotation = target.annotations().get(42, SourceCodeAnnotation.class).iterator().next();

        assertEquals(7, annotation.getPluginId());
        assertEquals(position, annotation.getPosition());
        assertTrue(Files.readString(MetadataSidecar.pathFor(image)).startsWith("# emuStudio memory metadata v1"));
    }

    @Test
    public void missingSidecarIsIgnored() throws IOException {
        Path image = temporaryFolder.newFile("memory.bin").toPath();
        MetadataSidecar.load(image, TestMemoryContextFactory.create(256, 1, 0));
    }

    @Test(expected = IOException.class)
    public void malformedSidecarIsRejected() throws IOException {
        Path image = temporaryFolder.newFile("memory.bin").toPath();
        Files.writeString(MetadataSidecar.pathFor(image), "not-valid");

        MetadataSidecar.load(image, TestMemoryContextFactory.create(256, 1, 0));
    }
}
