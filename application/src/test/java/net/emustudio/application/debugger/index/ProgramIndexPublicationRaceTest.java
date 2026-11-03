/* SPDX-FileCopyrightText: 2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger.index;

import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static net.emustudio.application.debugger.index.ProgramIndex.*;
import static org.junit.Assert.*;

public class ProgramIndexPublicationRaceTest {
    @Test(timeout = 15000)
    public void competingContentEditsPublishExactlyOneCompleteChange() throws Exception {
        competingEdits(true);
    }

    @Test(timeout = 15000)
    public void competingBreakpointEditsPublishExactlyOneCompleteChange() throws Exception {
        competingEdits(false);
    }

    private static void competingEdits(boolean content) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (int attempt = 0; attempt < 20; attempt++) {
                ProgramIndex index = create("same-component-race", 65536, 2, 49152);
                Snapshot base = index.snapshot();
                CountDownLatch staged = new CountDownLatch(2), release = new CountDownLatch(1);
                Future<CommitResult> first = workers.submit(() -> edit(index, base, 1, content, staged, release));
                Future<CommitResult> second = workers.submit(() -> edit(index, base, 2, content, staged, release));
                assertTrue("Both edits must capture the same root", staged.await(5, TimeUnit.SECONDS));
                release.countDown();
                List<CommitResult> results = List.of(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
                assertEquals(1, results.stream().filter(result -> result == CommitResult.PUBLISHED).count());
                assertEquals(1, results.stream().filter(result -> result == CommitResult.STALE).count());
                Snapshot current = index.snapshot();
                int[] entries = {0};
                if (content) current.forEachAnnotation(annotation -> entries[0]++);
                else current.forEachBreakpoint(breakpoint -> entries[0]++);
                assertEquals("The losing edit publishes no partial entries", 2, entries[0]);
                assertEquals(content ? 1 : 0, current.contentRevision());
                assertEquals(content ? 0 : 1, current.breakpointRevision());
                assertEquals(0, base.contentRevision());
                assertEquals(0, base.breakpointRevision());
            }
        } finally { workers.shutdownNow(); }
    }

    private static CommitResult edit(ProgramIndex index, Snapshot base, long owner, boolean content,
                                     CountDownLatch staged, CountDownLatch release) throws InterruptedException {
        if (content) {
            try (ContentUpdate update = index.updateContent(base)) {
                update.putAnnotation(owner, 1, new Range(0, 0, 1), Kinds.COMMENT, "first");
                update.putAnnotation(owner, 2, new Range(1, 1, 2), Kinds.COMMENT, "second");
                staged.countDown();
                assertTrue(release.await(5, TimeUnit.SECONDS));
                return update.commit();
            }
        }
        try (BreakpointUpdate update = index.updateBreakpoints(base)) {
            update.put(owner * 2, 0, true, "");
            update.put(owner * 2 + 1, 1, false, "A == 1");
            staged.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return update.commit();
        }
    }
}
