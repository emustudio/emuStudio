/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.mits88mds;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

final class MdsController implements AutoCloseable {
    static final int STATUS_PORT = 0x08;
    static final int CONTROL_PORT = 0x09;
    static final int DATA_PORT = 0x0A;
    static final int DRIVE_COUNT = 16;

    private final MdsDrive[] drives = new MdsDrive[DRIVE_COUNT];
    private int selectedDrive = -1;

    MdsController() {
        Arrays.setAll(drives, ignored -> new MdsDrive());
    }

    Context8080.CpuPortDevice statusPort() {
        return port("status/select", this::readStatus, this::select);
    }

    Context8080.CpuPortDevice controlPort() {
        return port("sector/control", this::readSectorPosition, this::control);
    }

    Context8080.CpuPortDevice dataPort() {
        return port("data", this::readData, this::writeData);
    }

    synchronized void attach(int drive, Path path) throws IOException {
        drive(drive).attach(path);
    }

    synchronized void detach(int drive) throws IOException {
        if (selectedDrive == drive) {
            selectedDrive = -1;
        }
        drive(drive).close();
    }

    synchronized Optional<Path> imagePath(int drive) {
        return Optional.ofNullable(drive(drive).getImagePath());
    }

    synchronized int track(int drive) {
        return drive(drive).getTrack();
    }

    synchronized int sector(int drive) {
        return drive(drive).getSector();
    }

    synchronized boolean isSelected(int drive) {
        return selectedDrive == drive;
    }

    synchronized void reset() {
        selectedDrive = -1;
        Arrays.stream(drives).forEach(MdsDrive::reset);
    }

    private synchronized int readStatus() throws IOException {
        return current().map(MdsDrive::status).orElse(0xFF);
    }

    private synchronized void select(int value) throws IOException {
        current().ifPresent(MdsController::deselectUnchecked);
        int requested = value & 0x0F;
        if (!drive(requested).isAttached()) {
            selectedDrive = -1;
            return;
        }
        selectedDrive = requested;
        if ((value & 0x80) != 0) {
            drive(requested).deselect();
        } else {
            drive(requested).select();
        }
    }

    private synchronized int readSectorPosition() throws IOException {
        return current().isPresent() ? current().orElseThrow().readSectorPosition() : 0xFF;
    }

    private synchronized void control(int value) throws IOException {
        if (current().isPresent()) {
            current().orElseThrow().control(value);
        }
    }

    private synchronized int readData() throws IOException {
        return current().isPresent() ? current().orElseThrow().readData() : 0;
    }

    private synchronized void writeData(int value) throws IOException {
        if (current().isPresent()) {
            current().orElseThrow().writeData(value);
        }
    }

    private Optional<MdsDrive> current() {
        return selectedDrive < 0 ? Optional.empty() : Optional.of(drives[selectedDrive]);
    }

    private MdsDrive drive(int index) {
        if (index < 0 || index >= DRIVE_COUNT) {
            throw new IllegalArgumentException("Drive must be between 0 and " + (DRIVE_COUNT - 1));
        }
        return drives[index];
    }

    private Context8080.CpuPortDevice port(String name, IoRead read, IoWrite write) {
        return new Context8080.CpuPortDevice() {
            @Override
            public byte read(int portAddress) {
                try {
                    return (byte) read.run();
                } catch (IOException e) {
                    throw new IllegalStateException("88-MDS " + name + " read failed", e);
                }
            }

            @Override
            public void write(int portAddress, byte value) {
                try {
                    write.run(value & 0xFF);
                } catch (IOException e) {
                    throw new IllegalStateException("88-MDS " + name + " write failed", e);
                }
            }

            @Override
            public String getName() {
                return "MITS 88-MDS " + name + " port";
            }
        };
    }

    private static void deselectUnchecked(MdsDrive drive) {
        try {
            drive.deselect();
        } catch (IOException e) {
            throw new IllegalStateException("Could not flush minidisk image", e);
        }
    }

    @Override
    public synchronized void close() {
        for (MdsDrive drive : drives) {
            try {
                drive.close();
            } catch (IOException e) {
                throw new IllegalStateException("Could not close minidisk image", e);
            }
        }
        selectedDrive = -1;
    }

    @FunctionalInterface
    private interface IoRead {
        int run() throws IOException;
    }

    @FunctionalInterface
    private interface IoWrite {
        void run(int value) throws IOException;
    }
}
