package net.emustudio.plugins.device.simh.commands;

import java.time.Instant;
import java.time.LocalDateTime;

import static net.emustudio.emulib.runtime.helpers.NumberUtils.bin2bcd;

public class GetClockZSDOS implements Command {
    public final static GetClockZSDOS INS = new GetClockZSDOS();

    private boolean currentTimeValid = false;
    private LocalDateTime currentTime;

    // ZSDOS clock definitions
   // private int ClockZSDOSDelta = 0; // delta between real clock and Altair clock
    private int getClockZSDOSPos = 0; // determines state for sending clock information

    @Override
    public void reset() {
        currentTimeValid = false;
        getClockZSDOSPos = 0;
    }

    @Override
    public byte read(Control control) {
        byte result = 0;
        if (currentTimeValid) {
            switch (getClockZSDOSPos) {

                case 0:
                    result = (byte) bin2bcd(currentTime.getYear() % 100);
                    getClockZSDOSPos = 1;
                    break;

                case 1:
                    result = (byte) bin2bcd(currentTime.getMonthValue());
                    getClockZSDOSPos = 2;
                    break;

                case 2:
                    result = (byte) bin2bcd(currentTime.getDayOfMonth());
                    getClockZSDOSPos = 3;
                    break;

                case 3:
                    result = (byte) bin2bcd(currentTime.getHour());
                    getClockZSDOSPos = 4;
                    break;

                case 4:
                    result = (byte) bin2bcd(currentTime.getMinute());
                    getClockZSDOSPos = 5;
                    break;

                case 5:
                    result = (byte) bin2bcd(currentTime.getSecond());
                    getClockZSDOSPos = 0;
                    control.clearCommand();
                    break;
            }
        } else {
            getClockZSDOSPos = 0;
            control.clearCommand();
        }
        return result;
    }

    @Override
    public void write(byte data, Control control) {

    }

    @Override
    public void start(Control control) {
        int delta = SetClockZSDOS.INS.ClockZSDOSDelta;
        currentTime = LocalDateTime.from(Instant.ofEpochSecond(Instant.now().getEpochSecond() + delta));
        currentTimeValid = true;
        getClockZSDOSPos = 0;
    }
}
