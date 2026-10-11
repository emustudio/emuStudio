/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.editor;

import java.util.*;
import java.util.regex.*;

/** Immutable preview: replacements always use the text that was searched. */
final class EditorSearch {
    static final class Match {
        final int start, end;
        final String before, after;
        Match(int start, int end, String before, String after) {
            this.start = start; this.end = end; this.before = before; this.after = after;
        }
    }

    static List<Match> find(String text, String query, String replacement, boolean regex,
                            boolean matchCase, boolean wholeWord, boolean preserveCase, int start, int end) {
        if (query.isEmpty()) return List.of();
        String pattern = regex ? query : Pattern.quote(query);
        if (wholeWord) pattern = "(?<![\\p{L}\\p{N}_])(?:" + pattern + ")(?![\\p{L}\\p{N}_])";
        Matcher matcher = Pattern.compile(pattern, Pattern.MULTILINE | (matchCase ? 0
                : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)).matcher(text);
        matcher.region(start, end);
        List<Match> result = new ArrayList<>();
        while (matcher.find()) {
            String after = regex ? expandReplacement(matcher, replacement) : replacement;
            if (preserveCase) after = preserve(matcher.group(), after);
            result.add(new Match(matcher.start(), matcher.end(), matcher.group(), after));
        }
        return result;
    }

    private static String expandReplacement(Matcher matcher, String replacement) {
        StringBuilder expanded = new StringBuilder();
        for (int i = 0; i < replacement.length(); i++) {
            char c = replacement.charAt(i);
            if (c == '\\') {
                if (++i == replacement.length()) throw new IllegalArgumentException("Trailing replacement escape");
                expanded.append(replacement.charAt(i));
            } else if (c == '$') {
                if (++i == replacement.length()) throw new IllegalArgumentException("Missing capture group");
                String value;
                if (replacement.charAt(i) == '{') {
                    int end = replacement.indexOf('}', i);
                    if (end < 0) throw new IllegalArgumentException("Missing capture group closing brace");
                    value = matcher.group(replacement.substring(i + 1, end)); i = end;
                } else {
                    int group = Character.digit(replacement.charAt(i), 10);
                    if (group < 0 || group > matcher.groupCount()) throw new IllegalArgumentException("Invalid capture group");
                    while (i + 1 < replacement.length() && Character.isDigit(replacement.charAt(i + 1))) {
                        int next = group * 10 + Character.digit(replacement.charAt(i + 1), 10);
                        if (next > matcher.groupCount()) break;
                        group = next; i++;
                    }
                    value = matcher.group(group);
                }
                if (value != null) expanded.append(value);
            } else expanded.append(c);
        }
        return expanded.toString();
    }

    private static String preserve(String before, String after) {
        if (before.equals(before.toUpperCase(Locale.ROOT))) return after.toUpperCase(Locale.ROOT);
        if (before.equals(before.toLowerCase(Locale.ROOT))) return after.toLowerCase(Locale.ROOT);
        if (!before.isEmpty() && Character.isUpperCase(before.charAt(0))
                && before.substring(1).equals(before.substring(1).toLowerCase(Locale.ROOT))) {
            return after.isEmpty() ? after : after.substring(0, 1).toUpperCase(Locale.ROOT)
                    + after.substring(1).toLowerCase(Locale.ROOT);
        }
        return after;
    }

    static String apply(String original, List<Match> matches) {
        int previousEnd = 0;
        for (Match match : matches) {
            if (match.start < previousEnd || match.end < match.start || match.end > original.length()
                    || !original.substring(match.start, match.end).equals(match.before)) {
                throw new IllegalArgumentException("Replacement ranges overlap or do not match the source snapshot");
            }
            previousEnd = match.end;
        }
        StringBuilder result = new StringBuilder(original);
        for (int i = matches.size() - 1; i >= 0; i--) {
            Match match = matches.get(i);
            result.replace(match.start, match.end, match.after);
        }
        return result.toString();
    }
}
