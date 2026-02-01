/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.memory.bytemem.loaders;

import net.emustudio.emulib.runtime.helpers.NumberUtils;
import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

public class BinaryLoader implements Loader {

    @Override
    public boolean isMemoryAddressAware() {
        return false;
    }

    @Override
    public void load(Path path, ByteMemoryContext memory, MemoryBank bank) throws IOException {
        int oldBank = memory.getSelectedBank();

        try (FileInputStream stream = new FileInputStream(path.toFile())) {
            memory.selectBank(bank.bank);
            byte[] content = stream.readAllBytes();
            memory.write(bank.address, NumberUtils.nativeBytesToBytes(content));
        } catch (IOException e) {
            memory.selectBank(oldBank);
            throw e;
        }
    }
}
