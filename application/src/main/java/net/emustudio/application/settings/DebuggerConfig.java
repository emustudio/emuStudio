/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import net.emustudio.application.emulation.debugger.UserAnnotations;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.IntSupplier;

/** Per-computer debugger settings, independent of execution and watch evaluation. */
public final class DebuggerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(DebuggerConfig.class);

    public final List<String> watches = new CopyOnWriteArrayList<>();
    public final Set<Integer> breakpoints = new ConcurrentSkipListSet<>();

    private final Path path;
    private final IntSupplier addressSpaceSize;
    private final MemoryAnnotations annotationEngine;
    private final List<String> loadWarnings = new ArrayList<>();
    private Properties lastSaved;
    private UserAnnotations annotations;

    public DebuggerConfig(Path path, IntSupplier addressSpaceSize, MemoryAnnotations annotationEngine) throws IOException {
        this.path = path;
        this.addressSpaceSize = Objects.requireNonNull(addressSpaceSize);
        this.annotationEngine = Objects.requireNonNull(annotationEngine);
        load();
    }

    public synchronized UserAnnotations annotations() {
        int size = addressSpaceSize.getAsInt();
        if (size < 1) throw new IllegalStateException("Memory unavailable");
        if (annotations == null || annotations.addressSpaceSize() != size)
            annotations = new UserAnnotations(size, annotationEngine);
        return annotations;
    }

    /** Problems found while loading settings; each call returns and clears them. */
    public synchronized List<String> takeLoadWarnings() {
        List<String> result = List.copyOf(loadWarnings);
        loadWarnings.clear();
        return result;
    }

    public synchronized void save() throws IOException {
        if (path == null) return;
        Properties properties = addressSpaceSize.getAsInt() > 0 ? annotationProperties() : new Properties();
        int watchIndex = 0;
        for (String watch : watches) properties.setProperty("watch." + watchIndex++, watch);
        int breakpointIndex = 0;
        for (int address : breakpoints)
            properties.setProperty("breakpoint." + breakpointIndex++ + ".address", Integer.toString(address));
        if (!properties.equals(lastSaved)) {
            saveProperties(path, properties);
            lastSaved = properties;
        }
    }

    private void load() throws IOException {
        if (path == null || !Files.isRegularFile(path)) return;
        Properties properties;
        try {
            properties = loadProperties(path);
        } catch (IllegalArgumentException | CharacterCodingException unreadable) {
            Files.delete(path);
            loadWarning("Debugger settings file " + path + " was unreadable and has been deleted.");
            return;
        }
        int skipped = 0;
        for (int index : indices(properties, "watch.", "")) {
            String watch = properties.getProperty("watch." + index);
            if (watch.isBlank()) skipped++;
            else watches.add(watch);
        }
        for (int index : indices(properties, "breakpoint.", ".address")) {
            if (!addLoadedBreakpoint(properties.getProperty("breakpoint." + index + ".address"))) skipped++;
        }
        if (addressSpaceSize.getAsInt() > 0) skipped += importAnnotations(properties);
        if (skipped > 0) loadWarning("Skipped " + skipped + " invalid debugger setting(s) in " + path + ".");
    }

    private boolean addLoadedBreakpoint(String address) {
        try {
            int at = Integer.parseInt(address);
            if (at < 0 || at >= addressSpaceSize.getAsInt()) return false;
            breakpoints.add(at);
            return true;
        } catch (NumberFormatException invalid) {
            return false;
        }
    }

    /** Exports only user annotations; watches and breakpoints stay in the debugger settings file. */
    public synchronized void saveAnnotations(Path target) throws IOException {
        saveProperties(target, annotationProperties());
    }

    /** Appends user annotations, skipping duplicates. Returns the number of invalid entries. */
    public synchronized int loadAnnotations(Path source) throws IOException {
        return importAnnotations(loadProperties(source));
    }

    /** Only ranges, labels and kinds are persisted; annotation identities are runtime-only. */
    Properties annotationProperties() {
        Properties properties = new Properties();
        int index = 0;
        for (UserAnnotations.Entry entry : annotations().entries()) {
            String key = "annotation." + index++ + ".";
            properties.setProperty(key + "address", Integer.toString(entry.address));
            properties.setProperty(key + "end", Integer.toString(entry.end));
            properties.setProperty(key + "label", entry.label);
            properties.setProperty(key + "kind", entry.kind);
        }
        return properties;
    }

    int importAnnotations(Properties properties) {
        List<UserAnnotations.Entry> entries = new ArrayList<>();
        int skipped = 0;
        for (int index : indices(properties, "annotation.", ".address")) {
            String key = "annotation." + index + ".";
            try {
                entries.add(new UserAnnotations.Entry(-1, Integer.parseInt(properties.getProperty(key + "address")),
                        Integer.parseInt(properties.getProperty(key + "end")),
                        properties.getProperty(key + "label", ""), properties.getProperty(key + "kind", "CODE")));
            } catch (RuntimeException invalid) {
                skipped++;
            }
        }
        return skipped + annotations().merge(entries);
    }

    private void loadWarning(String message) {
        loadWarnings.add(message);
        LOGGER.warn(message);
    }

    public static Properties loadProperties(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
        }
        return p;
    }

    /** Ascending indices i of present keys prefix + i + suffix. */
    public static List<Integer> indices(Properties properties, String prefix, String suffix) {
        Set<Integer> result = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix) || !key.endsWith(suffix) || key.length() <= prefix.length() + suffix.length()) continue;
            String index = key.substring(prefix.length(), key.length() - suffix.length());
            if (!index.chars().allMatch(c -> c >= '0' && c <= '9') || index.length() > 9) continue;
            result.add(Integer.parseInt(index));
        }
        return new ArrayList<>(result);
    }

    public static void saveProperties(Path path, Properties properties) throws IOException {
        Path target = path.toAbsolutePath();
        Path temp = Files.createTempFile(target.getParent(), ".emustudio-debug-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temp)) {
                properties.store(writer, "emuStudio debugger metadata");
            }
            try {
                Files.move(
                        temp,
                        target,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
