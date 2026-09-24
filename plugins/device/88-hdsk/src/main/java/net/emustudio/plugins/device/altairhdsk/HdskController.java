/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.emulib.plugins.memory.MemoryContext;
import net.emustudio.plugins.cpu.intel8080.api.Context8080;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

final class HdskController implements Context8080.CpuPortDevice, AutoCloseable {
    static final int PORT = 0xFD;
    static final int DRIVE_COUNT = 16;
    static final int RESET = 1;
    static final int READ = 2;
    static final int WRITE = 3;
    static final int PARAM = 4;
    static final int OK = 0;
    static final int ERROR = 1;

    private final MemoryContext<Byte> memory;
    private final HardDisk[] drives = new HardDisk[DRIVE_COUNT];
    private final int[] command = new int[6];
    private byte[] parameters = new byte[0];
    private int lastCommand;
    private int commandPosition;
    private int parameterPosition;

    HdskController(MemoryContext<Byte> memory) {
        this.memory = memory;
        Arrays.setAll(drives, ignored -> new HardDisk());
    }

    synchronized void configure(int drive, int sectorSize, int sectorsPerTrack) {
        drive(drive).configure(sectorSize, sectorsPerTrack);
    }

    synchronized void attach(int drive, Path path) throws IOException {
        drive(drive).attach(path);
    }

    synchronized void detach(int drive) throws IOException {
        drive(drive).close();
    }

    synchronized Optional<Path> imagePath(int drive) {
        return Optional.ofNullable(drive(drive).getImagePath());
    }

    synchronized int sectorSize(int drive) {
        return drive(drive).getSectorSize();
    }

    synchronized int sectorsPerTrack(int drive) {
        return drive(drive).getSectorsPerTrack();
    }

    synchronized int tracks(int drive) {
        try {
            return drive(drive).getTracks();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read HDSK image size", e);
        }
    }

    synchronized String activity(int drive) {
        return drive(drive).getActivity();
    }

    synchronized void reset() {
        resetCommand();
        Arrays.fill(command, 0);
    }

    @Override
    public synchronized byte read(int portAddress) {
        if ((lastCommand == READ || lastCommand == WRITE) && commandPosition == command.length) {
            int result = executeTransfer();
            resetCommand();
            return (byte) result;
        }
        if (lastCommand == PARAM && parameterPosition < parameters.length) {
            byte result = parameters[parameterPosition++];
            if (parameterPosition == parameters.length) {
                resetCommand();
            }
            return result;
        }
        return OK;
    }

    @Override
    public synchronized void write(int portAddress, byte value) {
        int unsigned = value & 0xFF;
        if (lastCommand == PARAM) {
            parameters = parameterBlock(unsigned < DRIVE_COUNT ? unsigned : 0);
            parameterPosition = 0;
            return;
        }
        if (lastCommand == READ || lastCommand == WRITE) {
            if (commandPosition < command.length) {
                command[commandPosition++] = unsigned;
            } else {
                resetCommand();
            }
            return;
        }
        if (unsigned >= RESET && unsigned <= PARAM) {
            lastCommand = unsigned;
            commandPosition = 0;
            parameterPosition = 0;
            if (unsigned == RESET) {
                Arrays.fill(command, 0);
            }
        } else {
            lastCommand = RESET;
            commandPosition = 0;
        }
    }

    private int executeTransfer() {
        int driveIndex = command[0] < DRIVE_COUNT ? command[0] : 0;
        HardDisk disk = drives[driveIndex];
        if (!disk.isAttached()) {
            return ERROR;
        }
        int sector = command[1];
        int track = command[2] | (command[3] << 8);
        int dma = command[4] | (command[5] << 8);
        try {
            if (sector >= disk.getSectorsPerTrack()) {
                sector = 0;
            }
            if (track >= disk.getTracks()) {
                track = 0;
            }
            int memorySize = memory.getSize();
            if (memorySize <= 0) {
                return ERROR;
            }
            if (lastCommand == READ) {
                byte[] data = disk.read(track, sector);
                for (int i = 0; i < data.length; i++) {
                    memory.write((dma + i) % memorySize, data[i]);
                }
            } else {
                byte[] data = new byte[disk.getSectorSize()];
                for (int i = 0; i < data.length; i++) {
                    data[i] = memory.read((dma + i) % memorySize);
                }
                disk.write(track, sector, data);
            }
            return OK;
        } catch (IOException | RuntimeException e) {
            return ERROR;
        }
    }

    private byte[] parameterBlock(int driveIndex) {
        HardDisk disk = drives[driveIndex];
        try {
            int ratio = disk.getSectorSize() / 128;
            int spt = disk.getSectorsPerTrack() * ratio;
            int psh = Integer.numberOfTrailingZeros(ratio);
            long reserved = 6L * spt * 128;
            long blocks = Math.max(1, (disk.getCapacity() - reserved) / 4096);
            int dsm = (int) Math.min(0xFFFF, blocks - 1);
            return new byte[]{
                    (byte) spt, (byte) (spt >>> 8), 5, 0x1F, 1,
                    (byte) dsm, (byte) (dsm >>> 8), (byte) 0xFF, 3,
                    (byte) 0xFF, 0, 0, 0, 6, 0, (byte) psh, (byte) (ratio - 1),
                    (byte) disk.getSectorSize(), (byte) (disk.getSectorSize() >>> 8)
            };
        } catch (IOException e) {
            throw new IllegalStateException("Could not build HDSK parameter block", e);
        }
    }

    private void resetCommand() {
        lastCommand = 0;
        commandPosition = 0;
        parameterPosition = 0;
    }

    private HardDisk drive(int index) {
        if (index < 0 || index >= DRIVE_COUNT) {
            throw new IllegalArgumentException("Drive must be between 0 and " + (DRIVE_COUNT - 1));
        }
        return drives[index];
    }

    @Override
    public String getName() {
        return "SIMH Altair HDSK";
    }

    @Override
    public synchronized void close() {
        for (HardDisk drive : drives) {
            try {
                drive.close();
            } catch (IOException e) {
                throw new IllegalStateException("Could not close HDSK image", e);
            }
        }
        reset();
    }
}
