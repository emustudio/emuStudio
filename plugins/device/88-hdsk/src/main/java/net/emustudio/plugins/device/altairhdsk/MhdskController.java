/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Emulates the MITS 88-HDSK controller firmware command protocol.
 * <p>
 * The controller never talks to CPU ports directly; all host communication flows through the
 * connected 88-4PIO (two 6820 PIAs, four channels). The firmware exchanges single command bytes,
 * data bytes and handshake strobes over those channels, so this class acts as a
 * {@link PioContext.Peripheral} and reacts to control-line and output changes rather than to
 * raw port reads/writes.
 * <p>
 * Media is modelled by up to {@link #PLATTER_COUNT} {@link Platter} images. A logical "unit" is a
 * pair of platters (two surfaces), so unit {@code u} maps to platters {@code 2u} and {@code 2u+1}.
 */
public final class MhdskController implements PioContext.Peripheral, AutoCloseable {
    /**
     * Maximum number of platter images (4 units × 2 surfaces).
     */
    public static final int PLATTER_COUNT = 8;
    private static final Logger LOGGER = LoggerFactory.getLogger(MhdskController.class);

    // Status byte flags reported back to the firmware after a command completes.
    private static final int NOT_READY = 1, BAD_SECTOR = 2, DATA_CRC = 4, BAD_CYLINDER = 0x20;
    private static final int BAD_HEAD = 0x40, WRITE_PROTECT = 0x80;

    /**
     * Multi-byte transfer the controller is currently in the middle of, if any.
     */
    private enum Transfer {NONE, READ_BUFFER, WRITE_BUFFER, SET_IV, READ_STATUS}

    private final PioContext pio;
    /**
     * Disk images; index {@code 2*unit + surface}. A {@code null} entry means no media attached.
     */
    private final Platter[] platters = new Platter[PLATTER_COUNT];
    /**
     * Four 256-byte sector buffers staged between host and media.
     */
    private final byte[][] buffers = new byte[4][256];
    /**
     * Current cylinder (head position) per unit.
     */
    private final int[] cylinders = new int[4];
    /**
     * Interface vector: the controller's register file exposed to the host.
     */
    private final int[] iv = new int[256];
    private Transfer transfer = Transfer.NONE;
    /**
     * Low byte of the pending command, selected buffer, byte position/length of a transfer, IV address and last status.
     */
    private int commandLow, buffer, index, count, ivAddress, status;
    /**
     * Human-readable description of the last command, for the GUI.
     */
    private String activity = "Reset";

    /**
     * @param pio the 88-4PIO this controller is wired to; must provide at least four channels
     * @throws IllegalArgumentException if fewer than two 6820 PIAs (four channels) are available
     */
    public MhdskController(PioContext pio) {
        if (pio.getChannelCount() < 4) {
            throw new IllegalArgumentException("MHDSK requires two 6820 PIAs");
        }
        this.pio = pio;
    }

    @Override
    public void outputChanged(int channel, int data, int outputMask) {
        // Data changes are irrelevant until a handshake strobe arrives; see controlOutputChanged.
    }

    @Override
    public String toString() {
        return "MITS 88-HDSK";
    }

    @Override
    public void controlOutputChanged(int channel, boolean high) {
        if (high) {
            return;
        }
        // B strobes follow writes; A strobes follow reads. DDR writes never issue commands.
        // Channel 1 high byte + channel 3 low byte together form a command; channel 3 alone is
        // write data; channel 2 signals the host consumed a byte the controller presented.
        if (channel == 1 && pio.getOutputMask(1) == 255 && pio.getOutputMask(3) == 255) {
            commandLow = pio.getOutputPins(3);
            acceptCommand(pio.getOutputPins(1));
        } else if (channel == 3 && pio.getOutputMask(3) == 255) {
            acceptData(pio.getOutputPins(3));
        } else if (channel == 2) {
            consumedData();
        }
    }

    /**
     * Toggle control line CA1/CB1 of the given channel to raise a handshake strobe to the host.
     */
    private void pulse(int channel) {
        pio.setControlLine1(channel, true);
        pio.setControlLine1(channel, false);
    }

    /**
     * Finish a command: latch the status byte, mirror it into the IV and strobe it to the host.
     */
    private void complete(int result) {
        status = result;
        iv[2] = result;
        pio.setInputPins(0, result);
        pulse(0);
    }

    /**
     * Decode and dispatch a firmware command. The high command byte carries the command nibble,
     * unit and buffer selection; {@link #commandLow} holds the previously latched low byte with
     * command-specific meaning (cylinder, head/sector, IV address, ...).
     */
    private void acceptCommand(int high) {
        transfer = Transfer.NONE;
        buffer = high & 3;
        int unit = (high >>> 2) & 3;
        int command = high >>> 4;
        index = 0;
        count = commandLow == 0 ? 256 : commandLow;
        activity = String.format("Command %X, unit %d, buffer %d", command, unit, buffer);
        pulse(1); // CMDACK also releases PIA1-B's handshake output for the next command.
        switch (command) {
            case 0:
                // Seek: move the selected unit to the requested cylinder.
                int cylinder = commandLow | ((high & 1) << 8);
                if (!isReady(unit)) {
                    complete(NOT_READY);
                } else if (cylinder >= Platter.CYLINDERS) {
                    complete(BAD_CYLINDER);
                } else {
                    cylinders[unit] = cylinder;
                    select(unit, 0);
                    updateCylinderRegisters(unit);
                    complete(0);
                }
                break;
            case 2:  // Write sector.
            case 3:  // Read sector.
            case 10: // Read sector unformatted (reports a CRC mismatch).
                sector(command, unit);
                break;
            case 4:
                // Host-to-buffer transfer: fill the selected buffer from write data strobes.
                transfer = Transfer.WRITE_BUFFER;
                pulse(3);
                break;
            case 5:
                // Buffer-to-host transfer: stream the selected buffer back to the host.
                transfer = Transfer.READ_BUFFER;
                sendData(buffers[buffer][0] & 255);
                break;
            case 6:
                // Read a single IV (status/register) byte.
                if (!isReady(unit)) {
                    complete(NOT_READY);
                    break;
                }
                select(unit, 0);
                ivAddress = commandLow;
                transfer = Transfer.READ_STATUS;
                sendData(readIv(ivAddress));
                complete(0);
                break;
            case 8:
                // Write a single IV byte supplied by the next data strobe.
                ivAddress = commandLow;
                transfer = Transfer.SET_IV;
                pulse(3);
                break;
            case 12:
                // Format the selected surface.
                format(unit);
                break;
            case 14:
                // Controller initialization / reset.
                initializeController();
                complete(0);
                break;
            default:
                // Undefined firmware command classes restart controller initialization.
                initializeController();
                complete(0);
        }
    }

    /**
     * Present a single byte to the host on channel 2 and strobe its availability.
     */
    private void sendData(int value) {
        pio.setInputPins(2, value);
        iv[6] = value;
        pulse(2);
    }

    /**
     * React to the host acknowledging it consumed a presented byte, advancing an active read.
     */
    private void consumedData() {
        if (transfer == Transfer.READ_STATUS) {
            transfer = Transfer.NONE;
        } else if (transfer == Transfer.READ_BUFFER) {
            if (++index == count) {
                transfer = Transfer.NONE;
                complete(0);
            } else {
                // Firmware signals CDA once for the block, then advances on each read strobe.
                int value = buffers[buffer][index] & 255;
                pio.setInputPins(2, value);
                iv[6] = value;
            }
        }
    }

    /**
     * Store a byte written by the host during a buffer or IV write transfer.
     */
    private void acceptData(int data) {
        if (transfer == Transfer.WRITE_BUFFER) {
            buffers[buffer][index++] = (byte) data;
            if (index == count) {
                transfer = Transfer.NONE;
                complete(0);
            }
        } else if (transfer == Transfer.SET_IV) {
            writeIv(ivAddress, data);
            transfer = Transfer.NONE;
            complete(0);
        }
    }

    /**
     * @return {@code true} if either surface of the unit has media attached.
     */
    private boolean isReady(int unit) {
        return platters[unit * 2] != null || platters[unit * 2 + 1] != null;
    }

    /**
     * Update the drive-select IV byte to reflect the active unit and head.
     */
    private void select(int unit, int head) {
        // IV17-19 invert at the disk interface; these are the bytes visible to the Altair.
        iv[17] = (~((~iv[17] & 0x80) | (1 << unit) | 0x40 | ((head & 3) << 4))) & 255;
    }

    /**
     * Reflect the unit's current cylinder into the IV cylinder registers.
     */
    private void updateCylinderRegisters(int unit) {
        iv[18] = (iv[18] & 254) | ((~(cylinders[unit] >>> 8)) & 1);
        iv[19] = (~cylinders[unit]) & 255;
    }

    /**
     * Read an IV register; addresses 3 and 7 shadow the live PIA output latches.
     */
    private int readIv(int address) {
        if (address == 3) {
            return pio.getOutputPins(1);
        }
        if (address == 7) {
            return pio.getOutputPins(3);
        }
        return iv[address];
    }

    private void writeIv(int address, int value) {
        iv[address] = value;
    }

    /**
     * Execute a read/write/unformatted-read against a single sector of the selected unit.
     */
    private void sector(int command, int unit) {
        int head = commandLow >>> 5;
        int sector = commandLow & 31;
        if (head > 3) {
            complete(BAD_HEAD);
            return;
        }
        if (sector >= Platter.SECTORS) {
            complete(BAD_SECTOR);
            return;
        }
        Platter platter = platters[unit * 2 + head / 2];
        if (platter == null) {
            complete(NOT_READY);
            return;
        }
        select(unit, head);
        try {
            if (command == 2) {
                if (platter.readOnly) {
                    complete(WRITE_PROTECT);
                    return;
                }
                platter.write(cylinders[unit], head & 1, sector, buffers[buffer]);
            } else {
                platter.read(cylinders[unit], head & 1, sector, buffers[buffer]);
            }
            // Raw images represent formatted data; a raw unformatted read has a CRC mismatch.
            complete((platter.readOnly ? WRITE_PROTECT : 0) | (command == 10 ? DATA_CRC : 0));
        } catch (IOException e) {
            LOGGER.warn("MHDSK media access failed", e);
            complete(NOT_READY);
        }
    }

    /**
     * Emulate a surface format. Raw images are already "formatted", so sector data is kept.
     */
    private void format(int unit) {
        int head = commandLow >>> 5;
        if (head > 3) {
            complete(BAD_HEAD);
            return;
        }
        Platter platter = platters[unit * 2 + head / 2];
        if (platter == null) {
            complete(NOT_READY);
            return;
        }
        if (platter.readOnly) {
            complete(WRITE_PROTECT);
            return;
        }
        // Firmware formats headers for the entire selected surface, preserving data areas.
        // Headers are implicit in a raw-sector image, so preserve all sector data here too.
        cylinders[unit] = Platter.CYLINDERS - 1;
        select(unit, head);
        updateCylinderRegisters(unit);
        complete(0);
    }

    /**
     * Reset controller state: clear the active transfer, cylinders and IV, re-arm select bytes.
     */
    private void initializeController() {
        transfer = Transfer.NONE;
        Arrays.fill(cylinders, 0);
        Arrays.fill(iv, 0);
        iv[17] = iv[18] = iv[19] = 255;
        index = count = 0;
    }

    @Override
    public void reset() {
        synchronized (pio) {
            initializeController();
            for (byte[] bytes : buffers) {
                Arrays.fill(bytes, (byte) 0);
            }
            pio.setInputPins(2, 255);
            complete(255); // Power-on response documented by MITS.
            activity = "Reset";
        }
    }

    /**
     * Attach a disk image to a platter slot, replacing any image already there.
     *
     * @param index    platter slot in {@code [0, PLATTER_COUNT)}
     * @param path     image file; must be exactly {@link Platter#CAPACITY} bytes
     * @param readOnly whether the platter should be write-protected
     * @throws IOException if the image is invalid or the previous platter cannot be released
     */
    public void attach(int index, Path path, boolean readOnly) throws IOException {
        synchronized (pio) {
            checkPlatter(index);
            Platter replacement = new Platter(path, readOnly);
            try {
                detach(index);
            } catch (IOException e) {
                replacement.close();
                throw e;
            }
            platters[index] = replacement;
        }
    }

    /**
     * Detach and close the image in the given platter slot, if any.
     */
    public void detach(int index) throws IOException {
        synchronized (pio) {
            checkPlatter(index);
            if (platters[index] != null) {
                platters[index].close();
                platters[index] = null;
            }
        }
    }

    /**
     * Create a new blank, correctly sized MHDSK image at the given path.
     */
    public static void createImage(Path path) throws IOException {
        Platter.create(path);
    }

    /**
     * @return the image path attached to the slot, or {@code null} if empty.
     */
    public Path getImage(int index) {
        synchronized (pio) {
            checkPlatter(index);
            return platters[index] == null ? null : platters[index].path;
        }
    }

    /**
     * @return {@code true} if the slot holds a write-protected image.
     */
    public boolean isReadOnly(int index) {
        synchronized (pio) {
            checkPlatter(index);
            return platters[index] != null && platters[index].readOnly;
        }
    }

    /**
     * @return a GUI-friendly description of the last command and status byte.
     */
    public String getActivity() {
        synchronized (pio) {
            return activity + String.format("; status %02Xh", status);
        }
    }

    private void checkPlatter(int index) {
        if (index < 0 || index >= PLATTER_COUNT) {
            throw new IllegalArgumentException("Invalid platter number");
        }
    }

    @Override
    public void close() {
        synchronized (pio) {
            pio.detachPeripheral(this);
            for (int index = 0; index < platters.length; index++) {
                try {
                    detach(index);
                } catch (IOException e) {
                    LOGGER.warn("Could not close MHDSK platter", e);
                }
            }
        }
    }
}
