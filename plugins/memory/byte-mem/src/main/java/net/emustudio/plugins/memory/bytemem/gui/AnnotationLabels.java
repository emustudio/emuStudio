/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui;

import net.emustudio.emulib.application.annotations.MemoryAnnotations;

import java.util.stream.Collectors;

public final class AnnotationLabels {
    private AnnotationLabels() { }

    public static String at(MemoryAnnotations annotations, int address) {
        if (annotations == null) return "";
        return annotations.at(address).stream()
                .filter(entry -> entry.start == address && !entry.text.isEmpty())
                .filter(entry -> entry.kind.equals("label") || entry.kind.startsWith("emuStudio.user/"))
                .map(entry -> entry.text)
                .collect(Collectors.joining(", "));
    }
}
