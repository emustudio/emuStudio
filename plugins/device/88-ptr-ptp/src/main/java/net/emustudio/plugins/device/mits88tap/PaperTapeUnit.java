/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88tap;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88tap.api.PaperTapeContext;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class PaperTapeUnit implements Context8080.CpuPortDevice, PaperTapeContext, AutoCloseable {
    public static final int STATUS_PORT = 0x12;
    public static final int DATA_PORT = 0x13;
    private static final int CAN_READ = 0x01;
    private static final int CAN_WRITE = 0x02;
    private static final int RESET = 0x03;
    private static final int CPM_END_OF_FILE = 0x1A;

    private byte[] readerData;
    private int readerPosition;
    private boolean endOfTape;
    private Path readerPath;
    private Path punchPath;
    private OutputStream punch;

    @Override
    public synchronized byte read(int portAddress) {
        switch (portAddress & 0xFF) {
            case STATUS_PORT:
                return (byte) (CAN_WRITE | (readerData != null && !endOfTape ? CAN_READ : 0));
            case DATA_PORT:
                return readTape();
            default:
                return (byte) 0xFF;
        }
    }

    @Override
    public synchronized void write(int portAddress, byte data) {
        switch (portAddress & 0xFF) {
            case STATUS_PORT:
                if ((data & 0xFF) == RESET) {
                    endOfTape = false;
                }
                break;
            case DATA_PORT:
                writeTape(data);
                break;
            default:
                break;
        }
    }

    @Override
    public String getName() {
        return "Altair PTR/PTP";
    }

    private byte readTape() {
        if (readerData == null || endOfTape) {
            return 0;
        }
        if (readerPosition < readerData.length) {
            return readerData[readerPosition++];
        }
        endOfTape = true;
        return CPM_END_OF_FILE;
    }

    private void writeTape(byte data) {
        if (punch == null) {
            return;
        }
        try {
            punch.write(data & 0xFF);
            punch.flush();
        } catch (IOException e) {
            detachPunch();
        }
    }

    @Override
    public synchronized void attachReader(Path path) throws IOException {
        readerData = Files.readAllBytes(path);
        readerPath = path.toAbsolutePath().normalize();
        readerPosition = 0;
        endOfTape = false;
    }

    @Override
    public synchronized void detachReader() {
        readerData = null;
        readerPath = null;
        readerPosition = 0;
        endOfTape = false;
    }

    @Override
    public synchronized void rewindReader() {
        readerPosition = 0;
        endOfTape = false;
    }

    @Override
    public synchronized void attachPunch(Path path) throws IOException {
        detachPunch();
        punch = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        punchPath = path.toAbsolutePath().normalize();
    }

    @Override
    public synchronized void detachPunch() {
        if (punch != null) {
            try {
                punch.close();
            } catch (IOException ignored) {
            }
        }
        punch = null;
        punchPath = null;
    }

    @Override
    public synchronized Optional<Path> getReaderPath() {
        return Optional.ofNullable(readerPath);
    }

    @Override
    public synchronized Optional<Path> getPunchPath() {
        return Optional.ofNullable(punchPath);
    }

    @Override
    public synchronized int getReaderPosition() {
        return readerPosition;
    }

    @Override
    public synchronized int getReaderLength() {
        return readerData == null ? 0 : readerData.length;
    }

    @Override
    public synchronized void close() {
        detachReader();
        detachPunch();
    }
}
