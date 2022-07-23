package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin;
import static net.emustudio.plugins.device.simh.commands.GetClockCPM3.*;

public class SetClockCPM3 implements Command {
    public final static SetClockCPM3 INS = new SetClockCPM3();

    public int ClockCPM3Delta = 0; // delta between real clock and Altair clock
    private int setClockCPM3Pos = 0; // determines state for receiving address of parameter block
    private int setClockCPM3Adr = 0; // address in M of 5 byte parameter block for setting time

    @Override
    public void reset() {
        ClockCPM3Delta = 0;
        setClockCPM3Pos = 0;
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setClockCPM3Pos == 0) {
            setClockCPM3Adr = data;
            setClockCPM3Pos = 1;
        } else {
            setClockCPM3Adr |= (data << 8);
            setClockCPM3(control.getMemory());
            setClockCPM3Pos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        setClockCPM3Pos = 0;
    }

    /* setClockCPM3Adr points to 5 byte block in M:
    0 - 1 int16:    days since 31 Dec 77
        2 BCD byte: HH
        3 BCD byte: MM
        4 BCD byte: SS                              */
    private void setClockCPM3(ByteMemoryContext mem) {
        long targetSeconds = CPM3_ORIGIN +
            (mem.read(setClockCPM3Adr) + mem.read(setClockCPM3Adr + 1) * 256) * SECONDS_PER_DAY +
            (long) bcd2bin(mem.read(setClockCPM3Adr + 2)) * SECONDS_PER_HOUR +
            (long) bcd2bin(mem.read(setClockCPM3Adr + 3)) * SECONDS_PER_MINUTE +
            bcd2bin(mem.read(setClockCPM3Adr + 4));

        // compute target year, month and day and replace hour, minute and second fields
        LocalDateTime targetDate = LocalDateTime
            .ofEpochSecond(targetSeconds, 0, ZoneOffset.UTC)
            .withHour(bcd2bin(mem.read(setClockCPM3Adr + 2)))
            .withMinute(bcd2bin(mem.read(setClockCPM3Adr + 3)))
            .withSecond(bcd2bin(mem.read(setClockCPM3Adr + 4)));
        ClockCPM3Delta = (int) (targetDate.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    }
}
