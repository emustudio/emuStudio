/* SPDX-FileCopyrightText: 2006-2025 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.runtime.io.IntelHEX;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.IOException;
import java.nio.file.Path;

public class HexLoader implements Loader {

    @Override
    public boolean isMemoryAddressAware() {
        return true;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws IOException {
        int oldBank = memory.getSelectedBank();
        try {
            memory.selectBank(bank.bank); // need to do it before loading
            IntelHEX.loadIntoMemory(path.toFile(), memory, p -> p);
        } catch (IOException e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }
}
