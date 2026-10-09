/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.plugins.device.altairhdsk;

import net.emustudio.plugins.device.mits88pio.api.PioContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

/** MITS firmware command protocol. CPU ports belong exclusively to the connected 88-4PIO. */
public final class MhdskController implements PioContext.Peripheral, AutoCloseable {
    public static final int PLATTER_COUNT = 8;
    private static final Logger LOGGER = LoggerFactory.getLogger(MhdskController.class);
    private static final int NOT_READY = 1, BAD_SECTOR = 2, DATA_CRC = 4, BAD_CYLINDER = 0x20;
    private static final int BAD_HEAD = 0x40, WRITE_PROTECT = 0x80;
    private enum Transfer { NONE, READ_BUFFER, WRITE_BUFFER, SET_IV, READ_STATUS }

    private final PioContext pio;
    private final Platter[] platters = new Platter[PLATTER_COUNT];
    private final byte[][] buffers = new byte[4][256];
    private final int[] cylinders = new int[4];
    private final int[] iv = new int[256];
    private Transfer transfer = Transfer.NONE;
    private int commandLow, buffer, index, count, ivAddress, status;
    private String activity = "Reset";

    public MhdskController(PioContext pio) {
        if (pio.getChannelCount() < 4) { throw new IllegalArgumentException("MHDSK requires two 6820 PIAs"); }
        this.pio = pio;
    }

    @Override public void outputChanged(int channel, int data, int outputMask) { }

    @Override
    public void controlOutputChanged(int channel, boolean high) {
        if (high) { return; }
        // B strobes follow writes; A strobes follow reads. DDR writes never issue commands.
        if (channel == 1 && pio.getOutputMask(1) == 255 && pio.getOutputMask(3) == 255) {
            commandLow = pio.getOutputPins(3);
            acceptCommand(pio.getOutputPins(1));
        } else if (channel == 3 && pio.getOutputMask(3) == 255) {
            acceptData(pio.getOutputPins(3));
        } else if (channel == 2) {
            consumedData();
        }
    }

    private void pulse(int channel) {
        pio.setControlLine1(channel, true);
        pio.setControlLine1(channel, false);
    }

    private void complete(int result) {
        status = result;
        iv[2] = result;
        pio.setInputPins(0, result);
        pulse(0);
    }

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
                int cylinder = commandLow | ((high & 1) << 8);
                if (!isReady(unit)) { complete(NOT_READY); }
                else if (cylinder >= Platter.CYLINDERS) { complete(BAD_CYLINDER); }
                else {
                    cylinders[unit] = cylinder;
                    select(unit, 0);
                    updateCylinderRegisters(unit);
                    complete(0);
                }
                break;
            case 2: case 3: case 10:
                sector(command, unit);
                break;
            case 4:
                transfer = Transfer.WRITE_BUFFER;
                pulse(3);
                break;
            case 5:
                transfer = Transfer.READ_BUFFER;
                sendData(buffers[buffer][0] & 255);
                break;
            case 6:
                if (!isReady(unit)) { complete(NOT_READY); break; }
                select(unit, 0);
                ivAddress = commandLow;
                transfer = Transfer.READ_STATUS;
                sendData(readIv(ivAddress));
                complete(0);
                break;
            case 8:
                ivAddress = commandLow;
                transfer = Transfer.SET_IV;
                pulse(3);
                break;
            case 12:
                format(unit);
                break;
            case 14:
                initializeController();
                complete(0);
                break;
            default:
                // Undefined firmware command classes restart controller initialization.
                initializeController();
                complete(0);
        }
    }

    private void sendData(int value) {
        pio.setInputPins(2, value);
        iv[6] = value;
        pulse(2);
    }

    private void consumedData() {
        if (transfer == Transfer.READ_STATUS) {
            transfer = Transfer.NONE;
        } else if (transfer == Transfer.READ_BUFFER) {
            if (++index == count) { transfer = Transfer.NONE; complete(0); }
            else {
                // Firmware signals CDA once for the block, then advances on each read strobe.
                int value = buffers[buffer][index] & 255;
                pio.setInputPins(2, value);
                iv[6] = value;
            }
        }
    }

    private void acceptData(int data) {
        if (transfer == Transfer.WRITE_BUFFER) {
            buffers[buffer][index++] = (byte) data;
            if (index == count) { transfer = Transfer.NONE; complete(0); }
        } else if (transfer == Transfer.SET_IV) {
            writeIv(ivAddress, data);
            transfer = Transfer.NONE;
            complete(0);
        }
    }

    private boolean isReady(int unit) {
        return platters[unit * 2] != null || platters[unit * 2 + 1] != null;
    }

    private void select(int unit, int head) {
        // IV17-19 invert at the disk interface; these are the bytes visible to the Altair.
        iv[17] = (~((~iv[17] & 0x80) | (1 << unit) | 0x40 | ((head & 3) << 4))) & 255;
    }

    private void updateCylinderRegisters(int unit) {
        iv[18] = (iv[18] & 254) | ((~(cylinders[unit] >>> 8)) & 1);
        iv[19] = (~cylinders[unit]) & 255;
    }

    private int readIv(int address) {
        if (address == 3) { return pio.getOutputPins(1); }
        if (address == 7) { return pio.getOutputPins(3); }
        return iv[address];
    }

    private void writeIv(int address, int value) {
        iv[address] = value;
    }

    private void sector(int command, int unit) {
        int head = commandLow >>> 5;
        int sector = commandLow & 31;
        if (head > 3) { complete(BAD_HEAD); return; }
        if (sector >= Platter.SECTORS) { complete(BAD_SECTOR); return; }
        Platter platter = platters[unit * 2 + head / 2];
        if (platter == null) { complete(NOT_READY); return; }
        select(unit, head);
        try {
            if (command == 2) {
                if (platter.readOnly) { complete(WRITE_PROTECT); return; }
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

    private void format(int unit) {
        int head = commandLow >>> 5;
        if (head > 3) { complete(BAD_HEAD); return; }
        Platter platter = platters[unit * 2 + head / 2];
        if (platter == null) { complete(NOT_READY); return; }
        if (platter.readOnly) { complete(WRITE_PROTECT); return; }
        // Firmware formats headers for the entire selected surface, preserving data areas.
        // Headers are implicit in a raw-sector image, so preserve all sector data here too.
        cylinders[unit] = Platter.CYLINDERS - 1;
        select(unit, head);
        updateCylinderRegisters(unit);
        complete(0);
    }

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
            for (byte[] bytes : buffers) { Arrays.fill(bytes, (byte) 0); }
            pio.setInputPins(2, 255);
            complete(255); // Power-on response documented by MITS.
            activity = "Reset";
        }
    }

    public void attach(int index, Path path, boolean readOnly) throws IOException {
        synchronized (pio) {
            checkPlatter(index);
            Platter replacement = new Platter(path, readOnly);
            try { detach(index); }
            catch (IOException e) { replacement.close(); throw e; }
            platters[index] = replacement;
        }
    }

    public void detach(int index) throws IOException {
        synchronized (pio) {
            checkPlatter(index);
            if (platters[index] != null) { platters[index].close(); platters[index] = null; }
        }
    }

    public static void createImage(Path path) throws IOException { Platter.create(path); }
    public Path getImage(int index) {
        synchronized (pio) { checkPlatter(index); return platters[index] == null ? null : platters[index].path; }
    }
    public boolean isReadOnly(int index) {
        synchronized (pio) { checkPlatter(index); return platters[index] != null && platters[index].readOnly; }
    }
    public String getActivity() { synchronized (pio) { return activity + String.format("; status %02Xh", status); } }

    private void checkPlatter(int index) {
        if (index < 0 || index >= PLATTER_COUNT) { throw new IllegalArgumentException("Invalid platter number"); }
    }

    @Override public void close() {
        synchronized (pio) {
            pio.detachPeripheral(this);
            for (int index = 0; index < platters.length; index++) {
                try { detach(index); }
                catch (IOException e) { LOGGER.warn("Could not close MHDSK platter", e); }
            }
        }
    }
}
