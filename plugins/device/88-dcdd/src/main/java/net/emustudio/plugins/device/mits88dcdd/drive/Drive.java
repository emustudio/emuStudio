/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2020  Peter Jakubčo
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Performs disk operations on single drive.
 */
@ThreadSafe
public class Drive {
    private static final Logger LOGGER = LoggerFactory.getLogger(Drive.class);

    public final static short DEFAULT_SECTORS_COUNT = 32;
    public final static short DEFAULT_SECTOR_LENGTH = 137;

    public static final short DEAD_DRIVE = 0b11100111;
    private static final short ALIVE_DRIVE = 0b11100101;
    private static final short MASK_TRACK0 = 0b10111111;

    public static final short SECTOR0 = 0b11000001;

    private static final short MASK_HEAD_LOAD = 0b11111011;
    private static final short MASK_DATA_AVAILABLE = 0b01111111;

    private final int driveIndex;

    private final ReadWriteLock positionLock = new ReentrantReadWriteLock();
    private int track;
    private int sector;
    private int sectorOffset;
    private int sectorsCount = DEFAULT_SECTORS_COUNT;
    private int sectorLength = DEFAULT_SECTOR_LENGTH;

    private Path mountedFloppy = null;
    private SeekableByteChannel imageChannel;
    private boolean selected = false;

    private final List<DriveListener> listeners = new CopyOnWriteArrayList<>();
    private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1);


    /*
       7   6   5   4   3   2   1   0
     +---+---+---+---+---+---+---+---+
     | R | Z | I | X | X | H | M | W |
     +---+---+---+---+---+---+---+---+

     W - When 0, write circuit ready to write another byte.
     M - When 0, head movement is allowed
     H - When 0, indicates head is loaded for read/write
     X - not used (will be 0)
     I - When 0, indicates interrupts enabled (not used this emulator)
     Z - When 0, indicates head is on track 0
     R - When 0, indicates that read circuit has new byte to read
     */
    private short port1status = DEAD_DRIVE;
    private short port2status = SECTOR0;

    public Drive(int driveIndex) {
        this.driveIndex = driveIndex;
        reset();
    }

    public void setSectorsCount(int sectorsCount) {
        if (sectorsCount <= 0) {
            throw new IllegalArgumentException("[drive=" + driveIndex + "] Sectors count must be > 0");
        }
        inWriteLock(() -> this.sectorsCount = sectorsCount);
        reset();
    }

    public void setSectorLength(int sectorLength) {
        if (sectorLength <= 0) {
            throw new IllegalArgumentException("[drive=" + driveIndex + "] Sector length must be > 0");
        }
        inWriteLock(() -> this.sectorLength = sectorLength);
        reset();
    }

    public int getSectorsCount() {
        return inReadLock(() -> sectorsCount);
    }

    public int getSectorLength() {
        return inReadLock(() -> sectorLength);
    }

    public void addDriveListener(DriveListener listener) {
        this.listeners.add(listener);
    }

    public DriveParameters getDriveParameters() {
        return inReadLock(
            () -> new DriveParameters(port1status, port2status, track, sector, getOffset(), mountedFloppy)
        );
    }

    public boolean isSelected() {
        return selected;
    }

    public void select() {
        if (mountedFloppy == null) {
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
            sectorOffset = sectorLength;
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
        this.mountedFloppy = imagePath;
        Set<OpenOption> optionSet = new HashSet<>();
        optionSet.add(StandardOpenOption.READ);
        optionSet.add(StandardOpenOption.WRITE);

        imageChannel = Files.newByteChannel(imagePath, optionSet);
    }

    public void umount() {
        if (inReadLock(() -> selected)) {
            deselect();
        }
        mountedFloppy = null;
        try {
            if (imageChannel != null) {
                imageChannel.close();
            }
        } catch (IOException e) {
            LOGGER.error("[drive={}] Could not un-mount disk image", driveIndex, e);
        }
    }

    public Path getImagePath() {
        return mountedFloppy;
    }

    public short getPort1status() {
        return inReadLock(() -> port1status);
    }

    public short getPort2status() {
        return inReadLock(() -> {
            if (((~port1status) & (~MASK_HEAD_LOAD)) != 0) {
                return port2status;
            } else {
                return (short) 0;
            }
        });
    }

    public void writeToPort2(short val) {
        inWriteLock(() -> {
            if ((val & 0x01) != 0) { /* Step head in */
                track++;
                // TODO: do not allow more tracks than available
                sector = 0;
                sectorOffset = sectorLength;
                port2status = SECTOR0;
            }
            if ((val & 0x02) != 0) { /* Step head out */
                track--;
                if (track < 0) {
                    track = 0;
                    port1status &= 0xBF; // head is on track 0
                }
                sector = 0;
                sectorOffset = sectorLength;
                port2status = SECTOR0;
            }
            if ((val & 0x04) != 0) { /* Head load */
                port1status &= MASK_HEAD_LOAD;
                port1status &= MASK_DATA_AVAILABLE;
                port2status = (short) ((sector << 1) & 0x3E | 0xC0);
                if (sectorOffset != 0) {
                    port2status |= 1; // SR0 = false
                }
            }
            if ((val & 0x08) != 0) { /* Head Unload */
                port1status |= (~MASK_HEAD_LOAD); /* turn off 'head loaded' */
                port1status |= (~MASK_DATA_AVAILABLE); /* turn off 'read data avail */
                sector = 0;
                sectorOffset = sectorLength;
                port2status = SECTOR0;
            }
            /* Interrupts & head current are ignored */
            if ((val & 0x80) != 0) { /* write sequence start */
                sectorOffset = 0;
                port2status &= 0xFE; // SR0 = true
                port1status &= 0xFE; /* enter new write data on */
            }
        });
        notifyParamsChanged();
    }

    public void nextSectorIfHeadIsLoaded() {
        inWriteLock(() -> {
            if (((~port1status) & (~MASK_HEAD_LOAD)) != 0) { /* head loaded? */
                sector = (short) ((sector + 1) % sectorsCount);
                sectorOffset = sectorLength;
                port2status = (short) ((sector << 1) & 0x3E | 0xC0);
            }
        });
        notifyParamsChanged();
    }

    public void writeData(int data) {
        inWriteLock(() -> {
            byteBuffer.clear();
            byteBuffer.put((byte) (data & 0xFF));
            byteBuffer.flip();

            if (sectorOffset == sectorLength) {
                port1status |= 1; /* ENWD off */
                port2status &= 0xFE; // SR0 = TRUE
                return;
            }

            int pos = sectorsCount * sectorLength * track + sectorLength * sector + sectorOffset;
            try {
                imageChannel.position(pos);
                imageChannel.write(byteBuffer);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                sectorOffset = (sectorOffset == sectorLength) ? sectorLength : (sectorOffset + 1);
                port2status |= 1;
            }
        });
        notifyParamsChanged();
    }

    public short readData() {
        if (mountedFloppy == null) {
            return 0;
        }
        short result = inWriteLock(() -> {
            try {
                int offset = (sectorOffset == sectorLength) ? 0 : sectorOffset;
                imageChannel.position(sectorsCount * sectorLength * track + sectorLength * sector + offset);
                byteBuffer.clear();
                int bytesRead = imageChannel.read(byteBuffer);
                if (bytesRead != byteBuffer.capacity()) {
                    throw new IOException("[drive=" + driveIndex + "] Could not read data from disk image");
                }
                byteBuffer.flip();
                return (short) (byteBuffer.get() & 0xFF);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } finally {
                sectorOffset = (sectorOffset == sectorLength) ? 1 : (sectorOffset + 1);
                if (sectorOffset == sectorLength) {
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
        return inReadLock(() -> sectorOffset == sectorLength ? 0 : sectorOffset);
    }

    private void reset() {
        inWriteLock(() -> {
            track = 0;
            sector = 0;
            sectorOffset = sectorLength;
            port1status = DEAD_DRIVE;
            port2status = SECTOR0;
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
