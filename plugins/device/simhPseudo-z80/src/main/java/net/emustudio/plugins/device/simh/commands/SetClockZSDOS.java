package net.emustudio.plugins.device.simh.commands;

import net.emustudio.plugins.memory.bytemem.api.ByteMemoryContext;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bcd2bin;

public class SetClockZSDOS implements Command {
    public final static SetClockZSDOS INS = new SetClockZSDOS();

    private int setClockZSDOSPos = 0; // determines state for receiving address of parameter block
    private int setClockZSDOSAdr = 0; // address in M of 6 byte parameter block for setting time
    public int ClockZSDOSDelta = 0; // delta between real clock and Altair clock

    @Override
    public void reset() {
        ClockZSDOSDelta = 0;
        setClockZSDOSPos = 0;
    }

    @Override
    public byte read(Control control) {
        return 0;
    }

    @Override
    public void write(byte data, Control control) {
        if (setClockZSDOSPos == 0) {
            setClockZSDOSAdr = data;
            setClockZSDOSPos = 1;
        } else {
            setClockZSDOSAdr |= (data << 8);
            setClockZSDOS(control.getMemory());
            setClockZSDOSPos = 0;
            control.clearCommand();
        }
    }

    @Override
    public void start(Control control) {
        setClockZSDOSPos = 0;
    }

    /* setClockZSDOSAdr points to 6 byte block in M: YY MM DD HH MM SS in BCD notation */
    private void setClockZSDOS(ByteMemoryContext mem) {
        int year = bcd2bin(mem.read(setClockZSDOSAdr));
        int tm_year = (year < 50 ? year + 100 : year) + 1900;
        int tm_mon  = bcd2bin(mem.read(setClockZSDOSAdr + 1));
        int tm_mday = bcd2bin(mem.read(setClockZSDOSAdr + 2));
        int tm_hour = bcd2bin(mem.read(setClockZSDOSAdr + 3));
        int tm_min  = bcd2bin(mem.read(setClockZSDOSAdr + 4));
        int tm_sec  = bcd2bin(mem.read(setClockZSDOSAdr + 5));

        LocalDateTime newTime = LocalDateTime.of(tm_year, tm_mon, tm_mday, tm_hour, tm_min, tm_sec);
        ClockZSDOSDelta = (int)(newTime.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
    }
}
