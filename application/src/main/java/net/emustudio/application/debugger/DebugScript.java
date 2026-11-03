/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.debugger;

import net.emustudio.emulib.plugins.cpu.CPU;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** Repeatable bounded debugger operations, shared by automation and tests. */
public final class DebugScript {
    private DebugScript() {}

    public static void run(DebugSession session, Path script)
            throws IOException, InterruptedException {
        session.requirePaused();
        List<String> lines = Files.readAllLines(script);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] words = line.split("\\s+", 2);
            String op = words[0].toUpperCase(Locale.ROOT), args = words.length > 1 ? words[1] : "";
            try {
                switch (op) {
                    case "STEP":
                        {
                            int n = Integer.decode(args);
                            if (n < 1 || n > 1000000)
                                throw new IllegalArgumentException("STEP count must be 1..1000000");
                            for (int j = 0; j < n; j++) step(session);
                            break;
                        }
                    case "RUN":
                        {
                            String[] values = args.split("\\s+");
                            if (values.length != 2)
                                throw new IllegalArgumentException("RUN address max-instructions");
                            int target = Integer.decode(values[0]),
                                    limit = Integer.decode(values[1]);
                            if (target < 0
                                    || target >= session.cpu.getAddressSpaceSize()
                                    || limit < 1
                                    || limit > 1000000)
                                throw new IllegalArgumentException("Invalid RUN bounds");
                            for (int j = 0;
                                    session.cpu.getInstructionLocation() != target && j < limit;
                                    j++) step(session);
                            if (session.cpu.getInstructionLocation() != target)
                                throw new IllegalArgumentException("RUN instruction limit reached");
                            break;
                        }
                    case "ASSERT":
                        if (session.evaluate(
                                        args,
                                        session.memory == null
                                                ? 0
                                                : session.memory.getSelectedBank())
                                == 0)
                            throw new IllegalArgumentException("Assertion failed: " + args);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown operation: " + op);
                }
            } catch (RuntimeException e) {
                throw new IOException("Script line " + (i + 1) + ": " + e.getMessage(), e);
            }
        }
    }

    private static void step(DebugSession session) throws IOException, InterruptedException {
        session.requirePaused();
        CountDownLatch done = new CountDownLatch(1);
        CPU.RunState[] result = {null};
        CPU.CPUListener listener =
                new CPU.CPUListener() {
                    @Override
                    public void runStateChanged(CPU.RunState state) {
                        if (state != CPU.RunState.STATE_RUNNING) {
                            result[0] = state;
                            done.countDown();
                        }
                    }

                    @Override
                    public void internalStateChanged() {}
                };
        session.cpu.addCPUListener(listener);
        try {
            session.cpu.step();
            if (!done.await(5, TimeUnit.SECONDS)) throw new IOException("CPU step timed out");
            if (result[0] != CPU.RunState.STATE_STOPPED_BREAK)
                throw new IOException("CPU stopped: " + result[0]);
        } finally {
            session.cpu.removeCPUListener(listener);
        }
    }
}
