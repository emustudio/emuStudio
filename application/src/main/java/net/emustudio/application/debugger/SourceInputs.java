/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugin.compiler.Compiler;
import net.emustudio.emulib.plugin.compiler.Token;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Compiler inputs captured before compilation reads them; each file is lexed once. */
public final class SourceInputs {
    private static final SourceInputs EMPTY = new SourceInputs(Map.of());
    private final Map<Path, SourceText> files;

    private SourceInputs(Map<Path, SourceText> files) {
        this.files = Collections.unmodifiableMap(files);
    }

    public static SourceInputs empty() { return EMPTY; }

    /** Follows include paths from the root, including source-only include files. */
    public static SourceInputs capture(Compiler compiler, Path root) throws IOException {
        Map<Path, SourceText> files = new LinkedHashMap<>();
        Set<Path> visitedFiles = new HashSet<>();
        Deque<Path> pending = new ArrayDeque<>();
        pending.add(normalize(root));
        while (!pending.isEmpty()) {
            Path path = pending.removeFirst();
            if (files.containsKey(path)) continue;
            SourceText source = SourceText.lex(compiler, path, Files.readString(path));
            files.put(path, source);
            // Resolve aliases only for cycle detection; retain compiler-visible paths as map keys.
            if (!visitedFiles.add(path.toRealPath())) continue;
            for (Token token : source.tokens()) {
                String included = token.getReferencedFile();
                if (token.getType() == Token.INCLUDE_PATH && included != null) {
                    Path include = Path.of(included.replace('\\', File.separatorChar).replace('/', File.separatorChar));
                    pending.add(normalize(path.getParent().resolve(include)));
                }
            }
        }
        return new SourceInputs(files);
    }

    /** Inputs supplied by a trusted source-metadata adapter instead of an include crawl. */
    static SourceInputs of(Compiler compiler, Map<Path, String> texts) {
        Map<Path, SourceText> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> input : texts.entrySet()) {
            Path path = normalize(input.getKey());
            SourceText previous = files.get(path);
            if (previous != null && !previous.text.equals(input.getValue()))
                throw new IllegalArgumentException("Conflicting source inputs: " + path);
            if (previous == null) files.put(path, SourceText.lex(compiler, path, input.getValue()));
        }
        return new SourceInputs(files);
    }

    Map<Path, SourceText> files() { return files; }

    static Path normalize(Path path) { return path.toAbsolutePath().normalize(); }
}
