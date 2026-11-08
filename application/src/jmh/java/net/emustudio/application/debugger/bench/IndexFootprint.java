/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.bench;

import net.emustudio.emulib.runtime.annotations.MemoryAnnotations;
import net.emustudio.application.debugger.index.ProgramIndex;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Reachable immutable graph sizes; this is not a heap-dominator retained-size analysis. */
public final class IndexFootprint {
    private IndexFootprint() { }

    public static void main(String[] arguments) throws Exception {
        System.out.println("# " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
        System.out.println("# Reachable object-graph bytes. Shared layout metadata and Class objects excluded; aliases counted once.");
        System.out.println("layout,entries,structure,bytes");
        for (String layout : new String[] {"single", "clustered", "sparse"}) {
            for (int count : new int[] {1, 1000, 10000, 50000}) {
                BenchmarkData data = new BenchmarkData(layout, count);
                if (!layout.equals("sparse")) {
                    ProgramIndex.Layout layoutMetadata = ProgramIndex.create("bench", data.size, new MemoryAnnotations()).snapshot().layout();
                    System.out.printf("%s,%d,old_annotations,%d%n", layout, count, bytes(data.baseline(layoutMetadata), layoutMetadata));
                }
                System.out.printf("%s,%d,index_annotations,%d%n", layout, count, bytes(data.index(false)));
                System.out.printf("%s,%d,index_annotations_and_sources,%d%n", layout, count, bytes(data.index()));
            }
            for (int count : new int[] {1, 1000, 10000}) {
                ProgramIndexGraphBenchmark.Graphs graphs = new ProgramIndexGraphBenchmark.Graphs();
                graphs.layout = layout;
                graphs.nodes = count;
                graphs.setup();
                System.out.printf("%s,%d,old_graph,%d%n", layout, count, bytes(graphs.baseline));
                System.out.printf("%s,%d,index_graph_with_listing,%d%n", layout, count, bytes(graphs.analysis));
            }
        }
    }

    static long bytes(Object root, Object... excluded) throws IllegalAccessException {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Collections.addAll(visited, excluded);
        ArrayDeque<Object> pending = new ArrayDeque<>();
        pending.add(root);
        long bytes = 0;
        while (!pending.isEmpty()) {
            Object value = pending.removeLast();
            if (value instanceof Class<?> || !visited.add(value)) continue;
            bytes += IndexFootprintAgent.sizeOf(value);
            Class<?> type = value.getClass();
            if (type.isArray()) {
                if (!type.getComponentType().isPrimitive())
                    for (int i = 0; i < Array.getLength(value); i++) add(pending, Array.get(value, i));
            } else {
                for (Class<?> owner = type; owner != null; owner = owner.getSuperclass())
                    for (Field field : owner.getDeclaredFields())
                        if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                            if (!field.trySetAccessible())
                                throw new IllegalStateException("Open module for footprint field: " + field);
                            add(pending, field.get(value));
                        }
            }
        }
        return bytes;
    }

    private static void add(ArrayDeque<Object> pending, Object value) {
        if (value != null) pending.add(value);
    }
}
