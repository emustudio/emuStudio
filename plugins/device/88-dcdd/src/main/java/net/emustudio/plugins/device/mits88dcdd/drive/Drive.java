/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.plugins.device.mits88dcdd.drive;

import net.emustudio.plugins.cpu.intel8080.api.Context8080;
import net.emustudio.plugins.device.mits88dcdd.DiskSettings;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * Performs disk operations on single drive.
 */
@ThreadSafe
public class Drive {
    private static final Logger LOGGER = LoggerFactory.getLogger(Drive.class);

    public static final byte DEAD_DRIVE = (byte) 0b11100111;
    private static final byte ALIVE_DRIVE = (byte) 0b11100101;
    private static final byte MASK_TRACK0 = (byte) 0b10111111;

    public static final byte SECTOR0 = (byte) 0b11000001;

    private static final byte MASK_HEAD_LOAD = (byte) 0b11111011;
    private static final byte MASK_DATA_AVAILABLE = 0b01111111;

    private final static Map<Integer, Byte> RST_MAP = Map.of(
        0, (byte) 0xC7,
        1, (byte) 0xCF,
        2, (byte) 0xD7,
        3, (byte) 0xDF,
        4, (byte) 0xE7,
        5, (byte) 0xEF,
        6, (byte) 0xF7,
        7, (byte) 0xFF
    );

    private final int driveIndex;
    private final Context8080 cpu;
    private final byte[] rstInterrupt;
    private volatile boolean interruptsSupported;

    private final ReadWriteLock positionLock = new ReentrantReadWriteLock();
    private int track;
    private int sector;
    private int sectorOffset;
    private DiskSettings.DriveSettings driveSettings = DiskSettings.DriveSettings.DEFAULT;

    private Path mountedImage = null;
    private SeekableByteChannel imageChannel;
    private boolean selected = false;

    private final List<DriveListener> listeners = new CopyOnWriteArrayList<>();
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1);

    private boolean sectorTrue = true; // SR0 alternation
    private boolean signalInterrupts = false;

    /*
       7   6   5   4   3   2   1   0
     +---+---+---+---+---+---+---+---+
     | R | Z | I | X | X | H | M | W |
     +---+---+---+---+---+---+---+---+

     W - When 0, write circuit ready to write another byte.
     M - When 0, head movement is allowed
     H - When 0, indicates head is loaded for read/write
     X - not used (will be 0)
     I - When 0, indicates interrupts enabled
     Z - When 0, indicates head is on track 0
     R - When 0, indicates that read circuit has new byte to read
     */
    private byte port1status = DEAD_DRIVE;
    /*
    +---+---+---+---+---+---+---+---+
    | X | X |  Sector Number    | T |
    +---+---+---+---+---+---+---+---+

    X = Not used
    Sector number = binary of the sector number currently under the head, 0-31.
    T = Sector True, is a 0 when the sector is positioned to read or write.
     */
    private byte port2status = SECTOR0;

    public Drive(int driveIndex, Context8080 cpu, Supplier<Integer> interruptVector, boolean interruptsSupported) {
        this.driveIndex = driveIndex;
        this.cpu = Objects.requireNonNull(cpu);
        this.rstInterrupt = new byte[]{RST_MAP.get(interruptVector.get())};
        this.interruptsSupported = interruptsSupported;
        reset();
    }

    public void setDriveSettings(DiskSettings.DriveSettings driveSettings) {
        this.driveSettings = Objects.requireNonNull(driveSettings);
        reset();
    }

    public void addDriveListener(DriveListener listener) {
        this.listeners.add(listener);
    }

    public DriveParameters getDriveParameters() {
        return inReadLock(
            () -> new DriveParameters(port1status, port2status, track, sector, getOffset(), mountedImage)
        );
    }

    public void setInterruptsSupported(boolean interruptsSupported) {
        this.interruptsSupported = interruptsSupported;
    }

    public boolean isSelected() {
        return selected;
    }

    public void select() {
        if (mountedImage == null) {
            LOGGER.warn("[drive={}] Could not select drive; image is not mounted", driveIndex);
        } else {
            selectInternal();
            notifyDiskSelected();
            notifyParamsChanged();
        }
    }

    private void selectInternal() {
        inWriteLock(() -> {
            selected = true;
            port1status = ALIVE_DRIVE;
            port2status = SECTOR0;
            sector = 0;
            sectorOffset = driveSettings.sectorSize;
            if (track == 0) {
                port1status &= MASK_TRACK0;
            }
        });
    }

    public void deselect() {
        deselectInternal();
        notifyDiskSelected();
        notifyParamsChanged();
    }

    private void deselectInternal() {
        inWriteLock(() -> {
            selected = false;
            port1status = DEAD_DRIVE;
        });
    }

    public void mount(Path imagePath) throws IOException {
        if (!Files.exists(imagePath) || Files.isDirectory(imagePath) || !Files.isReadable(imagePath)) {
            throw new FileNotFoundException("Image file is not readable or does not exist");
        }

        umount();
        this.mountedImage = imagePath;
        Set<OpenOption> optionSet = new HashSet<>();
        optionSet.add(StandardOpenOption.READ);
        optionSet.add(StandardOpenOption.WRITE);

        imageChannel = Files.newByteChannel(imagePath, optionSet);
    }

    public boolean isMounted() {
        return mountedImage != null;
    }

    public void umount() {
        if (inReadLock(() -> selected)) {
            deselect();
        }
        mountedImage = null;
        try {
            if (imageChannel != null) {
                imageChannel.close();
            }
        } catch (IOException e) {
            LOGGER.error("[drive={}] Could not un-mount disk image", driveIndex, e);
        }
    }

    public byte getPort1status() {
        return inReadLock(() -> port1status);
    }

    public byte getPort2status() {
        return inReadLock(() -> {
            if (((~port1status) & (~MASK_HEAD_LOAD)) != 0) {
                return port2status;
            } else {
                return (byte) 0xFF; // When head is not loaded, real hardware returns 0xFF
            }
        });
    }

    public void writeToPort2(short val) {
        inWriteLock(() -> {
            if ((val & 0x01) != 0) {
                // Step head in
                track++;
                sector = 0;
                sectorOffset = driveSettings.sectorSize;
                port2status = SECTOR0;
            }
            if ((val & 0x02) != 0) {
                // Step head out
                track--;
                if (track < 0) {
                    track = 0;
                    port1status &= 0xBF; // head is on track 0
                }
                sector = 0;
                sectorOffset = driveSettings.sectorSize;
                port2status = SECTOR0;
            }
            if ((val & 0x04) != 0) {
                // Head load
                port1status &= MASK_HEAD_LOAD;
                port1status &= MASK_DATA_AVAILABLE;
                port2status = (byte) ((sector << 1) & 0x3E | 0xC0);
                if (sectorOffset != 0) {
                    port2status |= 1; // SR0 = false
                }
            }
            if ((val & 0x08) != 0) {
                // Head Unload
                port1status |= (~MASK_HEAD_LOAD); // turn off 'head loaded'
                port1status |= (~MASK_DATA_AVAILABLE); // turn off 'read data avail'
                sector = 0;
                sectorOffset = driveSettings.sectorSize;
                port2status = SECTOR0;
            }
            if ((val & 0x10) != 0) {
                // interrupts enable
                signalInterrupts = true;
            }
            if ((val & 0x20) != 0) {
                // interrupts disable
                signalInterrupts = false;
            }
            if ((val & 0x80) != 0) {
                // write sequence start
                sectorOffset = 0;
                port2status &= 0xFE; // SR0 = true
                port1status &= 0xFE; // enter new write data on
            }
        });
        notifyParamsChanged();
    }

    public void nextSectorIfHeadIsLoaded() {
        inWriteLock(() -> {
            if (((~port1status) & (~MASK_HEAD_LOAD)) != 0) { // head loaded?
                if (sectorTrue) {
                    sector = (byte) ((sector + 1) % driveSettings.sectorsPerTrack);
                    sectorOffset = driveSettings.sectorSize;
                    if (signalInterrupts) {
                        signalInterrupt();
                    }
                }
                port2status = (byte) (((sector << 1) & 0x3E) | 0xC0 | (sectorTrue ? 1 : 0));
                sectorTrue = !sectorTrue;
            }
        });
        notifyParamsChanged();
    }

    public void writeData(int data) {
        inWriteLock(() -> {
            byteBuffer.clear();
            byteBuffer.put((byte) (data & 0xFF));
            byteBuffer.flip();

            if (sectorOffset == driveSettings.sectorSize) {
                port1status |= 1; // ENWD off
                port2status &= 0xFE; // SR0 = TRUE
                return;
            }

            int pos = driveSettings.sectorsPerTrack * driveSettings.sectorSize * track + driveSettings.sectorSize * sector + sectorOffset;
            try {
                imageChannel.position(pos);
                imageChannel.write(byteBuffer);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                sectorOffset = (sectorOffset == driveSettings.sectorSize) ? driveSettings.sectorSize : (sectorOffset + 1);
                port2status |= 1;
            }
        });
        notifyParamsChanged();
    }

    public byte readData() {
        if (mountedImage == null) {
            return 0;
        }
        byte result = inWriteLock(() -> {
            try {
                int offset = (sectorOffset == driveSettings.sectorSize) ? 0 : sectorOffset;
                imageChannel.position((long) driveSettings.sectorsPerTrack * driveSettings.sectorSize * track + (long) driveSettings.sectorSize * sector + offset);
                byteBuffer.clear();
                int bytesRead = imageChannel.read(byteBuffer);
                if (bytesRead != byteBuffer.capacity()) {
                    throw new IOException("[drive=" + driveIndex + "] Could not read data from disk image");
                }
                byteBuffer.flip();
                return (byte) (byteBuffer.get() & 0xFF);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                sectorOffset = (sectorOffset == driveSettings.sectorSize) ? 1 : (sectorOffset + 1);
                if (sectorOffset == driveSettings.sectorSize) {
                    port2status &= 0xFE;
                } else {
                    port2status |= 1;
                }
            }
        });
        notifyParamsChanged();
        return result;
    }

    public int getSector() {
        return inReadLock(() -> sector);
    }

    public int getTrack() {
        return inReadLock(() -> track);
    }

    public int getOffset() {
        return inReadLock(() -> sectorOffset == driveSettings.sectorSize ? 0 : sectorOffset);
    }

    private void reset() {
        inWriteLock(() -> {
            track = 0;
            sector = 0;
            sectorOffset = driveSettings.sectorSize;
            port1status = DEAD_DRIVE;
            port2status = SECTOR0;
            signalInterrupts = false;
        });
    }

    private void notifyDiskSelected() {
        boolean tmpSelected = inReadLock(() -> selected);
        for (DriveListener listener : listeners) {
            listener.driveSelect(tmpSelected);
        }
    }

    private void notifyParamsChanged() {
        DriveParameters parameters = getDriveParameters();
        for (DriveListener listener : listeners) {
            listener.driveParamsChanged(parameters);
        }
    }

    private void signalInterrupt() {
        if (interruptsSupported && cpu.isInterruptSupported()) {
            cpu.signalInterrupt(rstInterrupt);
        }
    }

    private <T> T inWriteLock(Callable<T> action) {
        positionLock.writeLock().lock();
        try {
            return action.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            positionLock.writeLock().unlock();
        }
    }

    private void inWriteLock(Runnable action) {
        positionLock.writeLock().lock();
        try {
            action.run();
        } finally {
            positionLock.writeLock().unlock();
        }
    }

    private <T> T inReadLock(Callable<T> action) {
        positionLock.readLock().lock();
        try {
            return action.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            positionLock.readLock().unlock();
        }
    }
}
