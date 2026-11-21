/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.emulation.debugger;

import net.emustudio.emulib.plugin.debugger.Debugging;
import net.emustudio.emulib.plugin.debugger.ValueView;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Session-confined evaluation with immutable snapshots for readers on other threads. */
final class WatchExpressions {
    private final Debugging debug;
    private final Map<String, Debugging.Expression> expressions = new HashMap<>();
    private volatile Map<String, ValueView> values = Map.of();
    private volatile Map<String, String> errors = Map.of();

    WatchExpressions(Debugging debug) {
        this.debug = debug;
    }

    Map<String, ValueView> values() { return values; }
    Map<String, String> errors() { return errors; }

    void refresh(Collection<String> watches) {
        Map<String, ValueView> currentValues = new LinkedHashMap<>();
        Map<String, String> currentErrors = new LinkedHashMap<>();
        for (String watch : watches) {
            String error = errors.get(watch);
            if (error != null) {
                currentErrors.put(watch, error);
                continue;
            }
            try {
                currentValues.put(watch, evaluate(watch));
            } catch (RuntimeException failure) {
                currentErrors.put(watch, Objects.toString(failure.getMessage(), "Watch evaluation failed"));
            }
        }
        values = Map.copyOf(currentValues);
        errors = Map.copyOf(currentErrors);
    }

    void retain(Collection<String> watches) {
        expressions.keySet().retainAll(new HashSet<>(watches));
    }

    void clearChanges() {
        Map<String, ValueView> cleared = new LinkedHashMap<>();
        values.forEach((source, value) -> cleared.put(source, new ValueView(value.text, value.formats, false)));
        values = Map.copyOf(cleared);
    }

    private ValueView evaluate(String watch) {
        if (debug == null) throw new IllegalStateException("Expressions unsupported by this CPU");
        ValueView current = expressions.computeIfAbsent(watch, debug::compile).display();
        ValueView previous = values.get(watch);
        return new ValueView(current.text, current.formats, current.changed || previous != null && previous.changed);
    }
}
