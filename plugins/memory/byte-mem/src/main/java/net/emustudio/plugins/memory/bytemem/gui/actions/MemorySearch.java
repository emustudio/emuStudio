/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.gui.actions;

import net.emustudio.plugins.memory.bytemem.gui.table.MemoryTableModel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class MemorySearch {
    private final MemoryTableModel tableModel;
    private final Consumer<Integer> selectAddress;
    private final Consumer<String> showStatus;

    private List<Integer> matches = Collections.emptyList();
    private int currentMatch = -1;

    public MemorySearch(MemoryTableModel tableModel, Consumer<Integer> selectAddress, Consumer<String> showStatus) {
        this.tableModel = Objects.requireNonNull(tableModel);
        this.selectAddress = Objects.requireNonNull(selectAddress);
        this.showStatus = Objects.requireNonNull(showStatus);
    }

    public void start(byte[] sequence, int from) {
        matches = tableModel.findSequences(sequence);
        currentMatch = -1;
        if (matches.isEmpty()) {
            showStatus.accept("No more matches");
            return;
        }

        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i) >= from) {
                currentMatch = i;
                break;
            }
        }
        if (currentMatch == -1) {
            currentMatch = 0;
        }
        selectCurrent();
    }

    public void next() {
        if (matches.isEmpty()) {
            showStatus.accept("No more matches");
            return;
        }
        currentMatch = (currentMatch + 1) % matches.size();
        selectCurrent();
    }

    public void previous() {
        if (matches.isEmpty()) {
            showStatus.accept("No more matches");
            return;
        }
        currentMatch = (currentMatch + matches.size() - 1) % matches.size();
        selectCurrent();
    }

    private void selectCurrent() {
        selectAddress.accept(matches.get(currentMatch));
        showStatus.accept("Match " + (currentMatch + 1) + " of " + matches.size());
    }
}
