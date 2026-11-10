/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebuggerConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void settingsFromAnotherCpuOrVersionAreLoaded() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        Properties original = new Properties();
        original.setProperty("version", "2");
        original.setProperty("cpu", "another CPU");
        original.setProperty("watch.0", "A + 1");
        DebuggerConfig.saveProperties(settings, original);

        DebuggerConfig config = new DebuggerConfig(settings, () -> 2, new MemoryAnnotations());

        assertEquals(List.of("A + 1"), config.watches);
        assertTrue(config.takeLoadWarnings().isEmpty());
        config.save();
        try (var paths = Files.list(settings.getParent())) {
            assertEquals(List.of(settings), paths.collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void unreadableSettingsFileIsDeletedWithWarning() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("debug.properties");
        Files.writeString(settings, "watch.0=\\u12");

        DebuggerConfig config = new DebuggerConfig(settings, () -> 2, new MemoryAnnotations());

        assertFalse(Files.exists(settings));
        assertTrue(config.watches.isEmpty());
        assertEquals(List.of("Debugger settings file " + settings + " was unreadable and has been deleted."),
                config.takeLoadWarnings());
        assertTrue(config.takeLoadWarnings().isEmpty());
    }

    @Test
    public void settingsFileWithInvalidEncodingIsDeletedWithWarning() throws Exception {
        Path settings = temporary.getRoot().toPath().resolve("binary.properties");
        Files.write(settings, new byte[] {'a', '=', (byte) 0xFF, (byte) 0xFE});

        DebuggerConfig config = new DebuggerConfig(settings, () -> 2, new MemoryAnnotations());

        assertFalse(Files.exists(settings));
        assertEquals(List.of("Debugger settings file " + settings + " was unreadable and has been deleted."),
                config.takeLoadWarnings());
    }

    @Test
    public void invalidSettingsEntriesAreSkippedWithWarning() throws Exception {
        Path settings = temporary.newFile("invalid.properties").toPath();
        Properties values = new Properties();
        values.setProperty("breakpoint.0.address", "0");
        values.setProperty("breakpoint.1.address", "99");
        values.setProperty("breakpoint.2.address", "x");
        values.setProperty("breakpoint.3.address", "0");
        values.setProperty("breakpoint.3.enabled", "maybe");
        values.setProperty("watch.0", " ");
        values.setProperty("rule.0.kind", "SOURCE");
        values.setProperty("annotation.0.address", "0");
        values.setProperty("annotation.0.end", "99");
        values.setProperty("annotation.0.kind", "DATA");
        DebuggerConfig.saveProperties(settings, values);

        DebuggerConfig config = new DebuggerConfig(settings, () -> 1, new MemoryAnnotations());

        assertEquals(List.of(0), List.copyOf(config.breakpoints));
        assertTrue(config.watches.isEmpty());
        assertTrue(config.annotations().entries().isEmpty());
        assertEquals(List.of("Skipped 4 invalid debugger setting(s) in " + settings + "."), config.takeLoadWarnings());
    }

    @Test
    public void annotationViewFollowsAddressSpaceChangesWithoutLosingMetadata() throws Exception {
        AtomicInteger size = new AtomicInteger(2);
        DebuggerConfig config = new DebuggerConfig(null, size::get, new MemoryAnnotations());
        UserAnnotations initial = config.annotations();
        initial.add(new UserAnnotations.Entry(1, "retained", "DATA"));

        size.set(4);

        assertNotSame(initial, config.annotations());
        assertEquals(4, config.annotations().addressSpaceSize());
        assertEquals("retained", config.annotations().get(1).label);
        assertSame(config.annotations(), config.annotations());
    }

    @Test
    public void saveReplacesPreviousFileAndPreservesEscapedUnicodeValues() throws Exception {
        Path file = temporary.newFile("state.properties").toPath();
        Files.writeString(file, "obsolete=old\n");
        Properties properties = new Properties();
        properties.setProperty("label", "žluťoučký = debugger\nsecond line");

        DebuggerConfig.saveProperties(file, properties);

        Properties restored = DebuggerConfig.loadProperties(file);
        assertEquals("žluťoučký = debugger\nsecond line", restored.getProperty("label"));
        assertNull(restored.getProperty("obsolete"));
        try (Stream<Path> files = Files.list(temporary.getRoot().toPath())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void failedReplacementCleansTemporaryFileAndPreservesTarget() throws Exception {
        Path target = temporary.newFolder("state").toPath();
        Path existing = target.resolve("keep.txt");
        Files.writeString(existing, "keep");

        assertThrows(IOException.class, () -> DebuggerConfig.saveProperties(target, new Properties()));

        assertEquals("keep", Files.readString(existing));
        try (Stream<Path> files = Files.list(temporary.getRoot().toPath())) {
            assertEquals(1, files.count());
        }
    }

    @Test
    public void missingFileReportsIoFailure() {
        Path missing = temporary.getRoot().toPath().resolve("missing.properties");

        assertThrows(IOException.class, () -> DebuggerConfig.loadProperties(missing));
    }

    @Test
    public void indicesAreScannedWithoutCountKeysOrContiguity() {
        Properties properties = new Properties();
        properties.setProperty("entries", "1");
        properties.setProperty("annotation.12.address", "1");
        properties.setProperty("annotation.0.address", "2");
        properties.setProperty("annotation.3.label", "no address");
        properties.setProperty("annotation.x.address", "3");
        properties.setProperty("annotation.-1.address", "4");
        properties.setProperty("annotation..address", "5");

        assertEquals(java.util.List.of(0, 12), DebuggerConfig.indices(properties, "annotation.", ".address"));
    }

    @Test
    public void importRetriesConcurrentEditsWithoutLosingLiveValuesOrOtherOwners() throws Exception {
        DebuggerConfig saved = new DebuggerConfig(null, () -> 8, new MemoryAnnotations());
        saved.annotations().add(new UserAnnotations.Entry(1, "saved", "DATA"));
        saved.annotations().add(new UserAnnotations.Entry(2, "retained", "CODE"));
        MemoryAnnotations engine = spy(new MemoryAnnotations());
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.getAndSet(false)) {
                engine.put(new MemoryAnnotations.Entry(UserAnnotations.OWNER, 1L << 40, 3, 4,
                        "DATA", "live edit", null));
                engine.put(new MemoryAnnotations.Entry(42, 1, 0, 1, "custom", "changed", null));
            }
            return call.callRealMethod();
        }).when(engine).compareAndReplaceOwner(anyLong(), eq(UserAnnotations.OWNER), anyCollection());
        DebuggerConfig live = new DebuggerConfig(null, () -> 8, engine);

        assertEquals(0, live.importAnnotations(saved.annotationProperties()));

        assertEquals("saved", live.annotations().get(1).label);
        assertEquals("retained", live.annotations().get(2).label);
        assertEquals("live edit", live.annotations().get(3).label);
        assertEquals("changed", engine.get(42, 1).text);
    }

    @Test
    public void malformedImportEntriesAreSkippedAndCounted() throws Exception {
        int size = 2;
        DebuggerConfig live = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        live.annotations().add(new UserAnnotations.Entry(0, "keep", "CODE"));
        DebuggerConfig saved = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        saved.annotations().add(new UserAnnotations.Entry(1, "saved", "DATA"));
        Properties input = saved.annotationProperties();
        input.setProperty("annotation.1.address", "invalid");
        input.setProperty("annotation.2.address", "0");
        input.setProperty("annotation.2.end", "1");
        input.setProperty("annotation.2.kind", "SOURCE");
        input.setProperty("annotation.3.address", "0");
        input.setProperty("annotation.3.end", "1");
        input.setProperty("annotation.3.kind", "UNKNOWN");

        assertEquals(3, live.importAnnotations(input));

        assertEquals(2, live.annotations().entries().size());
        assertEquals("keep", live.annotations().get(0).label);
        assertEquals("saved", live.annotations().get(1).label);
    }

    @Test
    public void metadataLoadPreservesCompilerMappingsAndOtherOwners() throws Exception {
        int size = 2;
        MemoryAnnotations engine = new MemoryAnnotations();
        DebuggerConfig live = new DebuggerConfig(null, () -> size, engine);
        DebuggerConfig captured = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        captured.annotations().add(new UserAnnotations.Entry(0, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(9, 8, 1, 2, "custom:note", "keep", null));

        live.importAnnotations(captured.annotationProperties());

        assertEquals("user", live.annotations().get(0).label);
        assertEquals("keep", engine.get(9, 8).text);
    }

    @Test
    public void importIgnoresLegacyCountAndVersionKeys() throws Exception {
        int size = 4;
        DebuggerConfig config = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        Properties input = new Properties();
        input.setProperty("version", "1");
        input.setProperty("entries", "1");
        for (int i : new int[] {0, 7}) {
            input.setProperty("annotation." + i + ".address", Integer.toString(i == 0 ? 1 : 2));
            input.setProperty("annotation." + i + ".end", Integer.toString(i == 0 ? 2 : 3));
            input.setProperty("annotation." + i + ".label", "value" + i);
            input.setProperty("annotation." + i + ".kind", "DATA");
        }

        assertEquals(0, config.importAnnotations(input));

        assertEquals("value0", config.annotations().get(1).label);
        assertEquals("value7", config.annotations().get(2).label);
    }

    @Test
    public void repeatedImportDoesNotDuplicateAnnotations() throws Exception {
        DebuggerConfig config = new DebuggerConfig(null, () -> 4, new MemoryAnnotations());
        config.annotations().add(new UserAnnotations.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties saved = config.annotationProperties();

        config.importAnnotations(saved);
        config.importAnnotations(saved);

        assertEquals(1, config.annotations().entries().size());
    }

    @Test
    public void savedAnnotationsContainOnlyRangesLabelsAndKinds() throws Exception {
        DebuggerConfig config = new DebuggerConfig(null, () -> 4, new MemoryAnnotations());
        config.annotations().add(new UserAnnotations.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties expected = new Properties();
        expected.setProperty("annotation.0.address", "1");
        expected.setProperty("annotation.0.end", "3");
        expected.setProperty("annotation.0.label", "buffer");
        expected.setProperty("annotation.0.kind", "DATA");

        assertEquals(expected, config.annotationProperties());
    }

    @Test
    public void importedAnnotationWithoutKindDefaultsToCode() throws Exception {
        DebuggerConfig config = new DebuggerConfig(null, () -> 4, new MemoryAnnotations());
        Properties input = new Properties();
        input.setProperty("annotation.0.address", "1");
        input.setProperty("annotation.0.end", "3");
        input.setProperty("annotation.0.label", "routine");

        assertEquals(0, config.importAnnotations(input));

        assertEquals("CODE", config.annotations().get(1).kind);
        assertEquals("CODE", config.annotationProperties().getProperty("annotation.0.kind"));
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        int size = 2;
        DebuggerConfig original = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        original.annotations().add(new UserAnnotations.Entry(1, "Entrée\nsecond line", "CODE"));
        original.watches.add("A");
        original.breakpoints.add(1);
        Path file = temporary.newFile("annotations.properties").toPath();

        original.saveAnnotations(file);
        Properties exported = DebuggerConfig.loadProperties(file);
        assertFalse(exported.containsKey("annotation.0.bookmark"));
        assertFalse(exported.containsKey("watch.0"));
        assertFalse(exported.containsKey("breakpoint.0.address"));
        DebuggerConfig restored = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        assertEquals(0, restored.loadAnnotations(file));

        UserAnnotations.Entry entry = restored.annotations().get(1);
        assertEquals(1, entry.address);
        assertEquals("Entrée\nsecond line", entry.label);
        assertEquals("CODE", entry.kind);
        assertTrue(restored.watches.isEmpty());
        assertTrue(restored.breakpoints.isEmpty());
    }

    @Test
    public void importKeepsIndependentAnnotationsAtTheSameAddress() throws Exception {
        int size = 2;
        DebuggerConfig config = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        config.annotations().add(new UserAnnotations.Entry(0, "old", "CODE"));
        config.annotations().add(new UserAnnotations.Entry(1, "keep", "DATA"));
        DebuggerConfig imported = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        imported.annotations().add(new UserAnnotations.Entry(0, "replacement", "CODE"));

        config.importAnnotations(imported.annotationProperties());

        assertEquals(3, config.annotations().entries().size());
        assertTrue(config.annotations().entries().stream().anyMatch(entry -> entry.label.equals("replacement")));
        assertTrue(config.annotations().entries().stream().anyMatch(entry -> entry.label.equals("old")));
        assertEquals("keep", config.annotations().get(1).label);
    }

    @Test
    public void overlappingRangesRemainIndependentAfterSaveAndReload() throws Exception {
        int size = 16;
        DebuggerConfig config = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        config.annotations().add(new UserAnnotations.Entry(-1, 2, 8, "first", "CODE"));
        config.annotations().add(new UserAnnotations.Entry(-1, 2, 5, "second", "CODE"));
        Properties saved = config.annotationProperties();
        DebuggerConfig restored = new DebuggerConfig(null, () -> size, new MemoryAnnotations());

        restored.importAnnotations(saved);
        UserAnnotations.Entry first = restored.annotations().entries().stream().filter(entry -> entry.label.equals("first")).findFirst().orElseThrow();
        UserAnnotations.Entry second = restored.annotations().entries().stream().filter(entry -> entry.label.equals("second")).findFirst().orElseThrow();
        restored.annotations().update(new UserAnnotations.Entry(first.id, 3, 9, "changed", "CODE"));
        restored.annotations().remove(second);

        assertEquals(1, restored.annotations().entries().size());
        assertEquals(first.id, restored.annotations().entries().get(0).id);
        assertEquals(3, restored.annotations().entries().get(0).address);
        assertEquals(9, restored.annotations().entries().get(0).end);
        assertEquals("changed", restored.annotations().entries().get(0).label);
    }

    @Test
    public void importingAllocatesIndependentIdentities() throws Exception {
        int size = 8;
        DebuggerConfig config = new DebuggerConfig(null, () -> size, new MemoryAnnotations());
        UserAnnotations.Entry original = config.annotations().add(new UserAnnotations.Entry(-1, 1, 4, "original", "CODE"));
        Properties imported = config.annotationProperties();
        imported.setProperty("annotation.0.label", "imported");

        config.importAnnotations(imported);

        assertEquals(2, config.annotations().entries().size());
        assertEquals("original", config.annotations().entries().stream().filter(entry -> entry.id == original.id).findFirst().get().label);
        assertTrue(config.annotations().entries().stream().anyMatch(entry -> entry.id != original.id && entry.label.equals("imported")));
    }
}
