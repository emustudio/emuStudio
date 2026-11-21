/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.gui.debugger;

import org.slf4j.Logger;

import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

/**
 * Shared debugger actions and address formatting.
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

}
