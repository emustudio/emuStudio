/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
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
import java.util.stream.Stream;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DebuggerConfigTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void readsAndWritesParentConfigDirectly() {
        Config parent = Config.inMemory();
        parent.set("name", "Computer");
        DebuggerConfig config = new DebuggerConfig(parent);

        assertTrue(config.getWatches().isEmpty());
        assertFalse(parent.contains("debugger"));
        parent.set("debugger.watches", List.of("A + 1"));
        assertEquals(List.of("A + 1"), config.getWatches());

        config.setWatches(List.of("B"));
        config.setBreakpoints(List.of(1));
        config.setAnnotations(List.of(new UserAnnotations.Entry(1, "buffer", "DATA")));
        assertEquals(List.of("B"), parent.get("debugger.watches"));
        assertEquals(List.of(1), parent.get("debugger.breakpoints"));
        assertEquals("buffer", config.getAnnotations().get(0).label);
        assertEquals("Computer", parent.get("name"));
    }

    @Test
    public void canReplaceMalformedSectionWithoutTouchingOtherSettings() {
        Config parent = Config.inMemory();
        parent.set("debugger", true);
        parent.set("name", "Computer");
        DebuggerConfig config = new DebuggerConfig(parent);

        assertTrue(config.getWatches().isEmpty());

        config.setWatches(List.of("A"));
        assertEquals(List.of("A"), parent.get("debugger.watches"));
        assertEquals("Computer", parent.get("name"));
    }

    @Test
    public void malformedListEntriesAreSkipped() {
        Config parent = Config.inMemory();
        parent.set("debugger.watches", List.of(" ", "A", 1));
        parent.set("debugger.breakpoints", List.of(0, "x", 2));
        Config annotation = Config.inMemory();
        annotation.set("address", "bad");
        parent.set("debugger.annotations", List.of(annotation));
        DebuggerConfig config = new DebuggerConfig(parent);

        assertEquals(List.of("A"), config.getWatches());
        assertEquals(List.of(0, 2), config.getBreakpoints());
        assertTrue(config.getAnnotations().isEmpty());
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
        UserAnnotations saved = new UserAnnotations(8, new MemoryAnnotations());
        saved.add(new UserAnnotations.Entry(1, "saved", "DATA"));
        saved.add(new UserAnnotations.Entry(2, "retained", "CODE"));
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
        UserAnnotations live = new UserAnnotations(8, engine);

        assertEquals(0, DebuggerConfig.importAnnotations(live, DebuggerConfig.annotationProperties(saved.entries())));

        assertEquals("saved", live.get(1).label);
        assertEquals("retained", live.get(2).label);
        assertEquals("live edit", live.get(3).label);
        assertEquals("changed", engine.get(42, 1).text);
    }

    @Test
    public void malformedImportEntriesAreSkippedAndCounted() throws Exception {
        int size = 2;
        UserAnnotations live = new UserAnnotations(size, new MemoryAnnotations());
        live.add(new UserAnnotations.Entry(0, "keep", "CODE"));
        UserAnnotations saved = new UserAnnotations(size, new MemoryAnnotations());
        saved.add(new UserAnnotations.Entry(1, "saved", "DATA"));
        Properties input = DebuggerConfig.annotationProperties(saved.entries());
        input.setProperty("annotation.1.address", "invalid");
        input.setProperty("annotation.2.address", "0");
        input.setProperty("annotation.2.end", "1");
        input.setProperty("annotation.2.kind", "SOURCE");
        input.setProperty("annotation.3.address", "0");
        input.setProperty("annotation.3.end", "1");
        input.setProperty("annotation.3.kind", "UNKNOWN");

        assertEquals(3, DebuggerConfig.importAnnotations(live, input));

        assertEquals(2, live.entries().size());
        assertEquals("keep", live.get(0).label);
        assertEquals("saved", live.get(1).label);
    }

    @Test
    public void metadataLoadPreservesCompilerMappingsAndOtherOwners() throws Exception {
        int size = 2;
        MemoryAnnotations engine = new MemoryAnnotations();
        UserAnnotations live = new UserAnnotations(size, engine);
        UserAnnotations captured = new UserAnnotations(size, new MemoryAnnotations());
        captured.add(new UserAnnotations.Entry(0, "user", "CODE"));
        engine.put(new MemoryAnnotations.Entry(9, 8, 1, 2, "custom:note", "keep", null));

        DebuggerConfig.importAnnotations(live, DebuggerConfig.annotationProperties(captured.entries()));

        assertEquals("user", live.get(0).label);
        assertEquals("keep", engine.get(9, 8).text);
    }

    @Test
    public void importIgnoresLegacyCountAndVersionKeys() throws Exception {
        int size = 4;
        UserAnnotations config = new UserAnnotations(size, new MemoryAnnotations());
        Properties input = new Properties();
        input.setProperty("version", "1");
        input.setProperty("entries", "1");
        for (int i : new int[] {0, 7}) {
            input.setProperty("annotation." + i + ".address", Integer.toString(i == 0 ? 1 : 2));
            input.setProperty("annotation." + i + ".end", Integer.toString(i == 0 ? 2 : 3));
            input.setProperty("annotation." + i + ".label", "value" + i);
            input.setProperty("annotation." + i + ".kind", "DATA");
        }

        assertEquals(0, DebuggerConfig.importAnnotations(config, input));

        assertEquals("value0", config.get(1).label);
        assertEquals("value7", config.get(2).label);
    }

    @Test
    public void repeatedImportDoesNotDuplicateAnnotations() throws Exception {
        UserAnnotations config = new UserAnnotations(4, new MemoryAnnotations());
        config.add(new UserAnnotations.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties saved = DebuggerConfig.annotationProperties(config.entries());

        DebuggerConfig.importAnnotations(config, saved);
        DebuggerConfig.importAnnotations(config, saved);

        assertEquals(1, config.entries().size());
    }

    @Test
    public void savedAnnotationsContainOnlyRangesLabelsAndKinds() throws Exception {
        UserAnnotations config = new UserAnnotations(4, new MemoryAnnotations());
        config.add(new UserAnnotations.Entry(-1, 1, 3, "buffer", "DATA"));
        Properties expected = new Properties();
        expected.setProperty("annotation.0.address", "1");
        expected.setProperty("annotation.0.end", "3");
        expected.setProperty("annotation.0.label", "buffer");
        expected.setProperty("annotation.0.kind", "DATA");

        assertEquals(expected, DebuggerConfig.annotationProperties(config.entries()));
    }

    @Test
    public void importedAnnotationWithoutKindDefaultsToCode() throws Exception {
        UserAnnotations config = new UserAnnotations(4, new MemoryAnnotations());
        Properties input = new Properties();
        input.setProperty("annotation.0.address", "1");
        input.setProperty("annotation.0.end", "3");
        input.setProperty("annotation.0.label", "routine");

        assertEquals(0, DebuggerConfig.importAnnotations(config, input));

        assertEquals("CODE", config.get(1).kind);
        assertEquals("CODE", DebuggerConfig.annotationProperties(config.entries()).getProperty("annotation.0.kind"));
    }

    @Test
    public void savedAnnotationsRestoreEveryUserField() throws Exception {
        int size = 2;
        UserAnnotations original = new UserAnnotations(size, new MemoryAnnotations());
        original.add(new UserAnnotations.Entry(1, "Entrée\nsecond line", "CODE"));
        Path file = temporary.newFile("annotations.properties").toPath();

        DebuggerConfig.exportAnnotations(file, original.entries());
        Properties exported = DebuggerConfig.loadProperties(file);
        assertFalse(exported.containsKey("annotation.0.bookmark"));
        assertFalse(exported.containsKey("watch.0"));
        assertFalse(exported.containsKey("breakpoint.0.address"));
        UserAnnotations restored = new UserAnnotations(size, new MemoryAnnotations());
        DebuggerConfig config = new DebuggerConfig(Config.inMemory());
        assertEquals(0, config.importAnnotations(file, restored));

        UserAnnotations.Entry entry = restored.get(1);
        assertEquals(1, entry.address);
        assertEquals("Entrée\nsecond line", entry.label);
        assertEquals("CODE", entry.kind);
        assertEquals(entry.label, config.getAnnotations().get(0).label);
    }

    @Test
    public void importKeepsIndependentAnnotationsAtTheSameAddress() throws Exception {
        int size = 2;
        UserAnnotations config = new UserAnnotations(size, new MemoryAnnotations());
        config.add(new UserAnnotations.Entry(0, "old", "CODE"));
        config.add(new UserAnnotations.Entry(1, "keep", "DATA"));
        UserAnnotations imported = new UserAnnotations(size, new MemoryAnnotations());
        imported.add(new UserAnnotations.Entry(0, "replacement", "CODE"));

        DebuggerConfig.importAnnotations(config, DebuggerConfig.annotationProperties(imported.entries()));

        assertEquals(3, config.entries().size());
        assertTrue(config.entries().stream().anyMatch(entry -> entry.label.equals("replacement")));
        assertTrue(config.entries().stream().anyMatch(entry -> entry.label.equals("old")));
        assertEquals("keep", config.get(1).label);
    }

    @Test
    public void overlappingRangesRemainIndependentAfterSaveAndReload() throws Exception {
        int size = 16;
        UserAnnotations config = new UserAnnotations(size, new MemoryAnnotations());
        config.add(new UserAnnotations.Entry(-1, 2, 8, "first", "CODE"));
        config.add(new UserAnnotations.Entry(-1, 2, 5, "second", "CODE"));
        Properties saved = DebuggerConfig.annotationProperties(config.entries());
        UserAnnotations restored = new UserAnnotations(size, new MemoryAnnotations());

        DebuggerConfig.importAnnotations(restored, saved);
        UserAnnotations.Entry first = restored.entries().stream().filter(entry -> entry.label.equals("first")).findFirst().orElseThrow();
        UserAnnotations.Entry second = restored.entries().stream().filter(entry -> entry.label.equals("second")).findFirst().orElseThrow();
        restored.update(new UserAnnotations.Entry(first.id, 3, 9, "changed", "CODE"));
        restored.remove(second);

        assertEquals(1, restored.entries().size());
        assertEquals(first.id, restored.entries().get(0).id);
        assertEquals(3, restored.entries().get(0).address);
        assertEquals(9, restored.entries().get(0).end);
        assertEquals("changed", restored.entries().get(0).label);
    }

    @Test
    public void importingAllocatesIndependentIdentities() throws Exception {
        int size = 8;
        UserAnnotations config = new UserAnnotations(size, new MemoryAnnotations());
        UserAnnotations.Entry original = config.add(new UserAnnotations.Entry(-1, 1, 4, "original", "CODE"));
        Properties imported = DebuggerConfig.annotationProperties(config.entries());
        imported.setProperty("annotation.0.label", "imported");

        DebuggerConfig.importAnnotations(config, imported);

        assertEquals(2, config.entries().size());
        assertEquals("original", config.entries().stream().filter(entry -> entry.id == original.id).findFirst().get().label);
        assertTrue(config.entries().stream().anyMatch(entry -> entry.id != original.id && entry.label.equals("imported")));
    }
}
