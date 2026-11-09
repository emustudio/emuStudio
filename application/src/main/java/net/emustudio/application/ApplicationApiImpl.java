/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application;

import net.emustudio.emulib.application.ApplicationApi;
import net.emustudio.emulib.application.ContextPool;
import net.emustudio.emulib.application.annotations.MemoryAnnotations;
import net.emustudio.emulib.application.ui.Dialogs;
import net.emustudio.emulib.application.ui.GUI;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ApplicationApiImpl implements ApplicationApi {
    private final ContextPool contextPool;
    private final Dialogs dialogs;
    private final GUI gui;
    private final AtomicInteger programLocation = new AtomicInteger();
    private final MemoryAnnotations annotations = new MemoryAnnotations();
    @Override public MemoryAnnotations getAnnotations() { return annotations; }

    public ApplicationApiImpl(ContextPool contextPool, Dialogs dialogs, GUI gui) {
        this.contextPool = Objects.requireNonNull(contextPool);
        this.dialogs = Objects.requireNonNull(dialogs);
        this.gui = gui;
    }

    @Override
    public ContextPool getContextPool() {
        return contextPool;
    }

    @Override
    public Dialogs getDialogs() {
        return dialogs;
    }

    @Override
    public GUI getGUI() {
        return gui;
    }

    @Override
    public void setProgramLocation(int location) {
        programLocation.set(location);
    }

    @Override
    public int getProgramLocation() {
        return programLocation.get();
    }
}
