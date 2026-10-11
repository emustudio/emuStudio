/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import net.emustudio.emulib.plugins.compiler.*;
import javax.swing.*;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/** Resolves compiler-provided symbol roles/scopes; has no language keywords or syntax rules. */
final class SourceRefactoring {
    static Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> rename(
            SearchWorkspace.Snapshot root, List<SearchWorkspace.Snapshot> open, Supplier<LexicalAnalyzer> lexers,
            int offset, String newName) throws IOException {
        LexicalAnalyzer support = lexers.get();
        if (!support.isValidSymbolName(newName)) throw new IllegalArgumentException("Invalid or reserved symbol name");
        Map<Path, SearchWorkspace.Snapshot> available = new HashMap<>();
        for (SearchWorkspace.Snapshot snapshot : open) if (snapshot.path != null) available.put(snapshot.path, snapshot);
        Map<SearchWorkspace.Snapshot, List<Token>> analyzed = new LinkedHashMap<>();
        collect(root, available, analyzed, lexers, new HashSet<>());
        Map<String, Token> declarations = new HashMap<>();
        for (List<Token> tokens : analyzed.values()) {
            for (Token token : tokens) {
                if (token.getType() != Token.SYMBOL_DEFINITION || token.getSymbolName() == null || token.getSymbolScope() == null) continue;
                String key = key(support, token.getSymbolScope(), token.getSymbolName());
                if (declarations.putIfAbsent(key, token) != null) throw new IllegalArgumentException("Ambiguous declaration: " + token.getSymbolName());
            }
        }
        Token target = analyzed.get(root).stream().filter(token -> token.getSymbolName() != null && offset >= token.getOffset()
                && offset < token.getOffset() + token.getSymbolName().length()).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Place the caret on a declared symbol or its reference"));
        String targetKey = binding(support, target, declarations);
        if (targetKey == null) throw new IllegalArgumentException("Cannot resolve the symbol in this source/include scope");
        String normalized = support.normalizeSymbolName(newName);
        if (!normalized.equals(support.normalizeSymbolName(target.getSymbolName()))) {
            for (Token declaration : declarations.values()) {
                if (normalized.equals(support.normalizeSymbolName(declaration.getSymbolName()))) throw new IllegalArgumentException("The new name conflicts with an existing declaration");
            }
            for (List<Token> tokens : analyzed.values()) for (Token token : tokens) {
                if (token.getSymbolName() != null && normalized.equals(support.normalizeSymbolName(token.getSymbolName()))) {
                    throw new IllegalArgumentException("The new name conflicts with an existing symbol reference");
                }
            }
        }
        Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> result = new LinkedHashMap<>();
        for (Map.Entry<SearchWorkspace.Snapshot, List<Token>> entry : analyzed.entrySet()) {
            List<EditorSearch.Match> edits = new ArrayList<>();
            for (Token token : entry.getValue()) {
                if (token.getSymbolName() != null && targetKey.equals(binding(support, token, declarations))) {
                    int start = token.getOffset(), end = start + token.getSymbolName().length();
                    edits.add(new EditorSearch.Match(start, end, entry.getKey().text.substring(start, end), newName));
                }
            }
            if (!edits.isEmpty()) result.put(entry.getKey(), edits);
        }
        return result;
    }
    private static void collect(SearchWorkspace.Snapshot source, Map<Path, SearchWorkspace.Snapshot> available,
                                Map<SearchWorkspace.Snapshot, List<Token>> analyzed, Supplier<LexicalAnalyzer> lexers, Set<Path> visiting) throws IOException {
        if (analyzed.containsKey(source)) throw new IllegalArgumentException("A source file is included more than once");
        if (source.path != null && !visiting.add(source.path)) throw new IllegalArgumentException("Cyclic source includes");
        List<Token> tokens = lexers.get().analyze(source.text); analyzed.put(source, tokens);
        for (Token token : tokens) {
            if (token.getReferencedFile() == null) continue;
            if (source.path == null) throw new IllegalArgumentException("Save this source before resolving includes");
            if (token.getSymbolScope() != null && !token.getSymbolScope().isEmpty()) throw new IllegalArgumentException("Includes inside a local scope cannot be safely renamed");
            Path included = source.path.getParent().resolve(token.getReferencedFile()).normalize().toAbsolutePath();
            if (visiting.contains(included)) throw new IllegalArgumentException("Cyclic source includes");
            SearchWorkspace.Snapshot next = available.get(included);
            if (next == null) {
                next = new SearchWorkspace.Snapshot(included, null, Files.readString(included)); available.put(included, next);
            }
            collect(next, available, analyzed, lexers, visiting);
        }
        if (source.path != null) visiting.remove(source.path);
    }
    private static String key(LexicalAnalyzer support, String scope, String name) { return scope + "\0" + support.normalizeSymbolName(name); }
    private static String binding(LexicalAnalyzer support, Token token, Map<String, Token> declarations) {
        if (token.getSymbolName() == null || token.getSymbolScope() == null) return null;
        String scope = token.getType() == Token.SYMBOL_DEFINITION ? token.getSymbolScope() : token.getReferenceScope();
        if (scope == null) return null;
        String local = key(support, scope, token.getSymbolName());
        if (declarations.containsKey(local)) return local;
        String global = key(support, "", token.getSymbolName());
        return declarations.containsKey(global) ? global : null;
    }
    static void rename(TabbedEditor workspace) {
        REditor editor = workspace.activeEditor();
        if (editor.compiler() == null) throw new IllegalArgumentException("This language has no symbol analysis");
        String newName = JOptionPane.showInputDialog(workspace.getView(), "New symbol name");
        if (newName == null) return;
        List<SearchWorkspace.Snapshot> open = SearchWorkspace.open(workspace.editors());
        SearchWorkspace.Snapshot root = open.stream().filter(snapshot -> snapshot.buffer == editor
                || snapshot.buffer.textPane().getDocument() == editor.textPane().getDocument()).findFirst().orElseThrow();
        try {
            Map<SearchWorkspace.Snapshot, List<EditorSearch.Match>> changes = rename(root, open, editor.compiler()::createLexer, editor.textPane().getCaretPosition(), newName.trim());
            StringBuilder preview = new StringBuilder();
            for (Map.Entry<SearchWorkspace.Snapshot, List<EditorSearch.Match>> entry : changes.entrySet()) {
                preview.append(entry.getKey().title()).append('\n');
                for (EditorSearch.Match change : entry.getValue()) {
                    preview.append("  ").append(entry.getKey().line(change.start)).append(": ").append(change.before).append(" → ").append(change.after).append('\n');
                }
            }
            JTextArea area = new JTextArea(preview.toString(), 20, 70); area.setEditable(false);
            if (JOptionPane.showConfirmDialog(workspace.getView(), new JScrollPane(area), "Rename preview", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) SearchWorkspace.apply(changes);
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    static void extract(TabbedEditor workspace) {
        REditor editor = workspace.activeEditor();
        if (editor.compiler() == null) throw new IllegalArgumentException("This language has no extraction support");
        LexicalAnalyzer support = editor.compiler().createLexer(); List<String> kinds = support.getExtractionKinds();
        if (kinds.isEmpty()) throw new IllegalArgumentException("This language has no extraction support");
        String kind = (String) JOptionPane.showInputDialog(workspace.getView(), "Extraction", "Extract selection", JOptionPane.PLAIN_MESSAGE, null, kinds.toArray(), kinds.get(0));
        if (kind == null) return;
        String name = JOptionPane.showInputDialog(workspace.getView(), "New symbol name"); if (name == null) return;
        String before = editor.textPane().getText();
        String after = support.extract(before, editor.textPane().getSelectionStart(), editor.textPane().getSelectionEnd(), name.trim(), kind);
        JTextArea area = new JTextArea(after, 20, 70); area.setEditable(false);
        if (JOptionPane.showConfirmDialog(workspace.getView(), new JScrollPane(area), "Extraction preview", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
            if (!before.equals(editor.textPane().getText())) throw new IllegalArgumentException("Source changed during preview");
            editor.commands().selections().clear(); TextEdits.replace(editor.textPane(), 0, before.length(), after);
        }
    }
}
