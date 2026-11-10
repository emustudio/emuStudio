/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * Shared debugger actions, address formatting and selection.
 */
final class DebuggerWidgets {

    @FunctionalInterface
    interface Operation {
        void run() throws Exception;
    }

    private DebuggerWidgets() {
    }

    /** Logs a failed user action, unwrapping asynchronous wrappers to the real cause. */
    static void logFailure(Logger logger, Throwable failure) {
        while ((failure instanceof CompletionException || failure instanceof ExecutionException)
                && failure.getCause() != null)
            failure = failure.getCause();
        logger.warn(failure.getMessage() == null ? failure.toString() : failure.getMessage(), failure);
    }

    /** Inclusive address range of the half-open interval [start, end). */
    static String addressRange(int start, int end) {
        return String.format("%04X–%04X", start, end - 1);
    }

    /**
     * Shared single-value selection; listeners hear only identity changes, while equal values refresh silently.
     */
    static final class Selection<T> {
        private final BiPredicate<T, T> same;
        private final List<Consumer<T>> listeners = new ArrayList<>();
        private T value;

        Selection(BiPredicate<T, T> same) {
            this.same = Objects.requireNonNull(same);
        }

        T get() {
            return value;
        }

        void set(T next) {
            boolean changed = value == null ? next != null : next == null || !same.test(value, next);
            value = next;
            if (changed) listeners.forEach(listener -> listener.accept(next));
        }

        void listen(Consumer<T> listener) {
            listeners.add(listener);
        }
    }

}
