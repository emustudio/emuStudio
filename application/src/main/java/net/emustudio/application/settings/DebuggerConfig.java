/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.settings;

import com.electronwill.nightconfig.core.Config;
import net.emustudio.application.emulation.debugger.UserAnnotations;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Debugger settings backed by the computer configuration. */
public final class DebuggerConfig {
    static final String SECTION = "debugger";

    private final Config config;

    public DebuggerConfig(Config config) {
        this.config = Objects.requireNonNull(config);
    }

    public List<String> getWatches() {
        return values("watches", String.class).stream()
                .filter(expression -> !expression.isBlank())
                .collect(Collectors.toUnmodifiableList());
    }

    public void setWatches(List<String> watches) {
        set("watches", List.copyOf(watches));
    }

    public List<Integer> getBreakpoints() {
        return values("breakpoints", Integer.class);
    }

    public void setBreakpoints(Collection<Integer> breakpoints) {
        set("breakpoints", List.copyOf(breakpoints));
    }

    public List<UserAnnotations.Entry> getAnnotations() {
        List<UserAnnotations.Entry> annotations = new ArrayList<>();
        for (Config entry : values("annotations", Config.class)) {
            Object address = entry.get("address");
            Object end = entry.get("end");
            Object label = entry.getOrElse("label", "");
            Object kind = entry.getOrElse("kind", "CODE");
            if (address instanceof Integer && end instanceof Integer && label instanceof String && kind instanceof String) {
                annotations.add(new UserAnnotations.Entry(-1, (int) address, (int) end, (String) label, (String) kind));
            }
        }
        return List.copyOf(annotations);
    }

    public void setAnnotations(List<UserAnnotations.Entry> annotations) {
        List<Config> entries = new ArrayList<>();
        for (UserAnnotations.Entry annotation : annotations) {
            Config entry = Config.inMemory();
            entry.set("address", annotation.address);
            entry.set("end", annotation.end);
            entry.set("label", annotation.label);
            entry.set("kind", annotation.kind);
            entries.add(entry);
        }
        set("annotations", entries);
    }

    private <T> List<T> values(String key, Class<T> type) {
        Object section = config.get(SECTION);
        if (!(section instanceof Config)) return List.of();
        Object entries = ((Config) section).get(key);
        if (!(entries instanceof List<?>)) return List.of();
        return ((List<?>) entries).stream().filter(type::isInstance).map(type::cast).collect(Collectors.toUnmodifiableList());
    }

    private void set(String key, List<?> values) {
        if (config.get(SECTION) instanceof Config) {
            config.set(List.of(SECTION, key), values);
        } else {
            Config section = Config.inMemory();
            section.set(key, values);
            config.set(SECTION, section);
        }
    }

    public int importAnnotations(Path source, UserAnnotations annotations) throws IOException {
        int skipped = importAnnotations(annotations, loadProperties(source));
        setAnnotations(annotations.entries());
        return skipped;
    }

    public static void exportAnnotations(Path target, List<UserAnnotations.Entry> entries) throws IOException {
        saveProperties(target, annotationProperties(entries));
    }

    /** Only ranges, labels and kinds are persisted; annotation identities are runtime-only. */
    static Properties annotationProperties(List<UserAnnotations.Entry> entries) {
        Properties properties = new Properties();
        int index = 0;
        for (UserAnnotations.Entry entry : entries) {
            String key = "annotation." + index++ + ".";
            properties.setProperty(key + "address", Integer.toString(entry.address));
            properties.setProperty(key + "end", Integer.toString(entry.end));
            properties.setProperty(key + "label", entry.label);
            properties.setProperty(key + "kind", entry.kind);
        }
        return properties;
    }

    static int importAnnotations(UserAnnotations annotations, Properties properties) {
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
        return skipped + annotations.merge(entries);
    }

    static Properties loadProperties(Path path) throws IOException {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(path)) {
            p.load(reader);
        }
        return p;
    }

    /** Ascending indices i of present keys prefix + i + suffix. */
    static List<Integer> indices(Properties properties, String prefix, String suffix) {
        Set<Integer> result = new TreeSet<>();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(prefix) || !key.endsWith(suffix) || key.length() <= prefix.length() + suffix.length()) continue;
            String index = key.substring(prefix.length(), key.length() - suffix.length());
            if (!index.chars().allMatch(c -> c >= '0' && c <= '9') || index.length() > 9) continue;
            result.add(Integer.parseInt(index));
        }
        return new ArrayList<>(result);
    }

    static void saveProperties(Path path, Properties properties) throws IOException {
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
