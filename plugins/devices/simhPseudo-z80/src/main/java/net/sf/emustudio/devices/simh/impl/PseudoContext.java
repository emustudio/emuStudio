/*
 * KISS, YAGNI, DRY
 *
 * Copyright (C) 2002-2007, Peter Schorn
 * (c) Copyright 2006-2017, Peter Jakubčo
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.emustudio.devices.simh.impl;

import emulib.plugins.device.DeviceContext;
import net.sf.emustudio.memory.standard.StandardMemoryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Calendar;

class PseudoContext implements DeviceContext<Short> {
    private final static Logger LOGGER = LoggerFactory.getLogger(PseudoContext.class);

    private StandardMemoryContext mem;

    /* SIMH pseudo device status registers                                                                          */
    /* ZSDOS clock definitions                                                                                      */
    private Calendar ClockZSDOSDelta = Calendar.getInstance(); /* delta between real clock and Altair clock                    */

    private int setClockZSDOSPos = 0; /* determines state for receiving address of parameter block    */

    private int setClockZSDOSAdr = 0; /* address in M of 6 byte parameter block for setting time      */

    private short getClockZSDOSPos = 0; /* determines state for sending clock information               */

    /* CPM3 clock definitions                                                                                       */
    private int ClockCPM3Delta = 0; /* delta between real clock and Altair clock                    */

    private int setClockCPM3Pos = 0; /* determines state for receiving address of parameter block    */

    private int setClockCPM3Adr = 0; /* address in M of 5 byte parameter block for setting time      */

    private short getClockCPM3Pos = 0; /* determines state for sending clock information               */

    private short daysCPM3SinceOrg = 0; /* days since 1 Jan 1978                                        */

    /* interrupt related                                                                                            */
    private int timeOfNextInterrupt; /* time when next interrupt is scheduled                        */

    //private boolean timerInterrupt = false; /* timer interrupt pending                                      */

    private int timerInterruptHandler = 0x0fc00;  /* default address of interrupt handling routine                */

    private int setTimerInterruptAdrPos = 0; /* determines state for receiving timerInterruptHandler         */

    private int timerDelta = 100;      /* interrupt every 100 ms                                       */

    private int setTimerDeltaPos = 0; /* determines state for receiving timerDelta                    */

    /* stop watch and timer related                                                                                 */
    private short stopWatchDelta = 0;        /* stores elapsed time of stop watch                            */

    private int getStopWatchDeltaPos = 0;        /* determines the state for receiving stopWatchDelta            */

    private int stopWatchNow = 0;        /* stores starting time of stop watch                           */

    private int markTimeSP = 0;        /* stack pointer for timer stack                                */

    ///* miscellaneous                                                                                                */
    private int versionPos = 0; /* determines state for sending device identifier               */

    private short lastCPMStatus = 0; /* result of last attachCPM command                             */

    private short lastCommand = 0; /* most recent command processed on port 0xfeh */

    private int getCommonPos = 0; /* determines state for sending the 'common' register           */

    private Calendar currentTime = Calendar.getInstance();
    private boolean currentTimeValid = false;
    private short version[] = {'S', 'I', 'M', 'H', '0', '0', '3', 0};
    private final static int SECONDS_PER_MINUTE = 60;
    private final static int SECONDS_PER_HOUR = (60 * SECONDS_PER_MINUTE);
    private final static int SECONDS_PER_DAY = (24 * SECONDS_PER_HOUR);

    void setMemory(StandardMemoryContext mem) {
        this.mem = mem;
    }
    /*  Z80 or 8080 programs communicate with the SIMH pseudo device via port 0xfe.
    The following principles apply:

    1)  For commands that do not require parameters and do not return results
    ld  a,<cmd>
    out (0feh),a
    Special case is the reset command which needs to be send 128 times to make
    sure that the internal state is properly reset.

    2)  For commands that require parameters and do not return results
    ld  a,<cmd>
    out (0feh),a
    ld  a,<p1>
    out (0feh),a
    ld  a,<p2>
    out (0feh),a
    ...
    Note: The calling program must send all parameter bytes. Otherwise
    the pseudo device is left in an undefined state.

    3)  For commands that do not require parameters and return results
    ld  a,<cmd>
    out (0feh),a
    in  a,(0feh)    ; <A> contains first byte of result
    in  a,(0feh)    ; <A> contains second byte of result
    ...
    Note: The calling program must request all bytes of the result. Otherwise
    the pseudo device is left in an undefined state.

    4)  Commands requiring parameters and returning results do not exist currently.

     */
    private final static int printTimeCmd = 0;             /*  0 print the current time in milliseconds                            */

    private final static int startTimerCmd = 1;            /*  1 start a new timer on the top of the timer stack                   */

    private final static int stopTimerCmd = 2;             /*  2 stop timer on top of timer stack and show time difference         */

    private final static int resetPTRCmd = 3;              /*  3 reset the PTR device                                              */

    private final static int attachPTRCmd = 4;             /*  4 attach the PTR device                                             */

    private final static int detachPTRCmd = 5;             /*  5 detach the PTR device                                             */

    private final static int getSIMHVersionCmd = 6;        /*  6 get the current version of the SIMH pseudo device                 */

    private final static int getClockZSDOSCmd = 7;         /*  7 get the current time in ZSDOS format                              */

    private final static int setClockZSDOSCmd = 8;         /*  8 set the current time in ZSDOS format                              */

    private final static int getClockCPM3Cmd = 9;          /*  9 get the current time in CP/M 3 format                             */

    private final static int setClockCPM3Cmd = 10;         /* 10 set the current time in CP/M 3 format                             */

    private final static int getBankSelectCmd = 11;        /* 11 get the selected bank                                             */

    private final static int setBankSelectCmd = 12;        /* 12 set the selected bank                                             */

    private final static int getCommonCmd = 13;            /* 13 get the base address of the common memory segment                 */

    private final static int resetSIMHInterfaceCmd = 14;   /* 14 reset the SIMH pseudo device                                      */

    private final static int showTimerCmd = 15;            /* 15 show time difference to timer on top of stack                     */

    private final static int attachPTPCmd = 16;            /* 16 attach PTP to the file with name at beginning of CP/M command line*/

    private final static int detachPTPCmd = 17;            /* 17 detach PTP                                                        */

    private final static int hasBankedMemoryCmd = 18;      /* 18 determines whether machine has banked memory                      */

    private final static int setZ80CPUCmd = 19;            /* 19 set the CPU to a Z80                                              */

    private final static int set8080CPUCmd = 20;           /* 20 set the CPU to an 8080                                            */

    private final static int startTimerInterruptsCmd = 21; /* 21 start timer interrupts                                            */

    private final static int stopTimerInterruptsCmd = 22;  /* 22 stop timer interrupts                                             */

    private final static int setTimerDeltaCmd = 23;        /* 23 set the timer interval in which interrupts occur                  */

    private final static int setTimerInterruptAdrCmd = 24; /* 24 set the address to call by timer interrupts                       */

    private final static int resetStopWatchCmd = 25;       /* 25 reset the millisecond stop watch                                  */

    private final static int readStopWatchCmd = 26;        /* 26 read the millisecond stop watch                                   */

    private final static int SIMHSleepCmd = 27;            /* 27 let SIMH sleep for SIMHSleep microseconds                         */

    private final static int getHostOSPathSeparator = 28;  /* 28 obtain the file path separator of the OS under which SIMH runs    */

    private final static int getHostFilenames = 29;        /* 29 perform wildcard expansion and obtain list of file names          */


    void reset() {
        currentTimeValid = false;
        lastCommand = 0;
        lastCPMStatus = 0;
        setClockZSDOSPos = 0;
        getClockZSDOSPos = 0;
        ClockZSDOSDelta = Calendar.getInstance();
        ClockCPM3Delta = 0;
        setClockCPM3Pos = 0;
        getClockCPM3Pos = 0;
        getStopWatchDeltaPos = 0;
        getCommonPos = 0;
        setTimerDeltaPos = 0;
        setTimerInterruptAdrPos = 0;
        markTimeSP = 0;
        versionPos = 0;
    }

    private int toBCD(int x) {
        return (x / 10) * 16 + (x % 10);
    }

    private int fromBCD(int x) {
        return 10 * ((0xf0 & x) >> 4) + (0x0f & x);
    }

    /* setClockZSDOSAdr points to 6 byte block in M: YY MM DD HH MM SS in BCD notation */
    private void setClockZSDOS() {
        int year = fromBCD(mem.read(setClockZSDOSAdr));
        //int yy = (year < 50 ? year + 100 : year) + 1900;
        int mm = fromBCD(mem.read(setClockZSDOSAdr + 1)) - 1;
        int dd = fromBCD(mem.read(setClockZSDOSAdr + 2));
        int hh = fromBCD(mem.read(setClockZSDOSAdr + 3));
        int min = fromBCD(mem.read(setClockZSDOSAdr + 4));
        int ss = fromBCD(mem.read(setClockZSDOSAdr + 5));
        ClockZSDOSDelta.set(year, mm, dd, hh, min, ss);
    }

    private short mkCPM3Origin() {
        short month, year;
        short result;
        short[] m_to_d = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        month = 11; // t->tm_mon
        year = (short) ((1977 + month / 12 + 1900) & 0xFFFF); // t->tm_year
        month %= 12;
        if (month < 0) {
            year -= 1;
            month += 12;
        }
        //result = (short) (((year - 1970) * 365 + (year - 1969) / 4 + m_to_d[month]) & 0xffff);
        result = (short) (((year - 1970) * 365 + m_to_d[month]) & 0xffff);
        if (month <= 1) {
            year -= 1;
        }
        result += (year - 1968) / 4;
        result -= (year - 1900) / 100;
        result += (year - 1600) / 400;
        result += 31; //t->tm_mday;
        result -= 1;
        result *= 24;
        result += 0; //t->tm_hour;
        result *= 60;
        result += 0; //t->tm_min;
        result *= 60;
        result += 0; //t->tm_sec;
        return (result);
    }

    /* setClockCPM3Adr points to 5 byte block in M:
    0 - 1 int16:    days since 31 Dec 77
    2 BCD byte: HH
    3 BCD byte: MM
    4 BCD byte: SS                              */
    private void setClockCPM3() {
        ClockCPM3Delta = mkCPM3Origin()
                + (mem.read(setClockCPM3Adr) + mem.read(setClockCPM3Adr + 1) * 256)
                * SECONDS_PER_DAY + fromBCD(mem.read(setClockCPM3Adr + 2))
                * SECONDS_PER_HOUR + fromBCD(mem.read(setClockCPM3Adr + 3))
                * SECONDS_PER_MINUTE + fromBCD(mem.read(setClockCPM3Adr + 4))
                - (short) (Calendar.getInstance().getTimeInMillis() / 1000);
    }

    @Override
    public Short read() {
        short result = 0;
        switch (lastCommand) {
            case getHostFilenames:
                LOGGER.trace("[command={},name=getHostFilenames] Unimplemented command!", lastCommand);
                lastCommand = 0;
                break;
            case attachPTRCmd:
            case attachPTPCmd:
                LOGGER.trace("[command={},name=attachPTRCmd/attachPTPCmd] Unimplemented command!", lastCommand);

                result = lastCPMStatus;
                lastCommand = 0;
                break;
            case getClockZSDOSCmd:
                if (currentTimeValid) {
                    switch (getClockZSDOSPos) {
                        case 0:
                            int year = (currentTime.get(Calendar.YEAR) - 1900);
                            result = (short) toBCD(year > 99 ? year - 100 : year);
                            getClockZSDOSPos = 1;
                            break;
                        case 1:
                            result = (short) toBCD(currentTime.get(Calendar.MONTH) + 1);
                            getClockZSDOSPos = 2;
                            break;
                        case 2:
                            result = (short) toBCD(currentTime.get(Calendar.DAY_OF_MONTH));
                            getClockZSDOSPos = 3;
                            break;
                        case 3:
                            result = (short) toBCD(currentTime.get(Calendar.HOUR_OF_DAY));
                            getClockZSDOSPos = 4;
                            break;
                        case 4:
                            result = (short) toBCD(currentTime.get(Calendar.MINUTE));
                            getClockZSDOSPos = 5;
                            break;
                        case 5:
                            result = (short) toBCD(currentTime.get(Calendar.SECOND));
                            getClockZSDOSPos = lastCommand = 0;
                            break;
                    }
                } else {
                    result = getClockZSDOSPos = lastCommand = 0;
                }
                break;
            case getClockCPM3Cmd:
                if (currentTimeValid) {
                    switch (getClockCPM3Pos) {
                        case 0:
                            result = (short) (daysCPM3SinceOrg & 0xff);
                            getClockCPM3Pos = 1;
                            break;
                        case 1:
                            result = (short) ((daysCPM3SinceOrg >> 8) & 0xff);
                            getClockCPM3Pos = 2;
                            break;
                        case 2:
                            result = (short) toBCD(currentTime.get(Calendar.HOUR_OF_DAY));
                            getClockCPM3Pos = 3;
                            break;
                        case 3:
                            result = (short) toBCD(currentTime.get(Calendar.MINUTE));
                            getClockCPM3Pos = 4;
                            break;
                        case 4:
                            result = (short) toBCD(currentTime.get(Calendar.SECOND));
                            getClockCPM3Pos = lastCommand = 0;
                            break;
                    }
                } else {
                    result = getClockCPM3Pos = lastCommand = 0;
                }
                break;
            case getSIMHVersionCmd:
                result = version[versionPos++];
                if (result == 0) {
                    versionPos = lastCommand = 0;
                }
                break;
            case getBankSelectCmd:
                result = mem.getSelectedBank();
                lastCommand = 0;
                break;
            case getCommonCmd:
                if (getCommonPos == 0) {
                    result = (short) (mem.getCommonBoundary() & 0xff);
                    getCommonPos = 1;
                } else {
                    result = (short) ((mem.getCommonBoundary() >> 8) & 0xff);
                    getCommonPos = lastCommand = 0;
                }
                break;
            case hasBankedMemoryCmd:
                result = (short) mem.getBanksCount();
                lastCommand = 0;
                break;
            case readStopWatchCmd:
                if (getStopWatchDeltaPos == 0) {
                    result = (short) (stopWatchDelta & 0xff);
                    getStopWatchDeltaPos = 1;
                } else {
                    result = (short) ((stopWatchDelta >> 8) & 0xff);
                    getStopWatchDeltaPos = lastCommand = 0;
                }
                break;
            case getHostOSPathSeparator:
                result = (short) File.separatorChar;
                break;
            default: /* undefined */
                LOGGER.debug("[command={}] Unknown command!", lastCommand);
                result = lastCommand = 0;
        }
        return result;
    }

    @Override
    public void write(Short value) {
        long now;
        switch (lastCommand) {
            case setClockZSDOSCmd:
                if (setClockZSDOSPos == 0) {
                    setClockZSDOSAdr = value;
                    setClockZSDOSPos = 1;
                } else {
                    setClockZSDOSAdr |= (value << 8);
                    setClockZSDOS();
                    setClockZSDOSPos = lastCommand = 0;
                }
                break;
            case setClockCPM3Cmd:
                if (setClockCPM3Pos == 0) {
                    setClockCPM3Adr = value;
                    setClockCPM3Pos = 1;
                } else {
                    setClockCPM3Adr |= (value << 8);
                    setClockCPM3();
                    setClockCPM3Pos = lastCommand = 0;
                }
                break;
            case setBankSelectCmd:
                mem.selectBank((short) (value & 0xff));
                lastCommand = 0;
                break;
            case setTimerDeltaCmd:
                if (setTimerDeltaPos == 0) {
                    timerDelta = value;
                    setTimerDeltaPos = 1;
                } else {
                    timerDelta |= (value << 8);
                    setTimerDeltaPos = lastCommand = 0;
                }
                break;
            case setTimerInterruptAdrCmd:
                if (setTimerInterruptAdrPos == 0) {
                    timerInterruptHandler = value;
                    setTimerInterruptAdrPos = 1;
                } else {
                    timerInterruptHandler |= (value << 8);
                    setTimerInterruptAdrPos = lastCommand = 0;
                }
                break;
            default:
                lastCommand = value;
                switch (value) {
                    case getHostFilenames:
                        LOGGER.trace("[command={},name=getHostFilenames,method=write] Unimplemented command!", lastCommand);
                        break;
                    case SIMHSleepCmd:
                        LOGGER.trace("[command={},name=SIMHSleepCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case printTimeCmd:
                        LOGGER.trace("[command={},name=printTimeCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case startTimerCmd:
                        LOGGER.trace("[command={},name=startTimerCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case stopTimerCmd:
                        LOGGER.trace("[command={},name=stopTimerCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case resetPTRCmd:
                        LOGGER.trace("[command={},name=resetPTRCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case attachPTRCmd:
                        LOGGER.trace("[command={},name=attachPTRCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case detachPTRCmd:
                        LOGGER.trace("[command={},name=detachPTRCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case getSIMHVersionCmd:
                        versionPos = 0;
                        break;
                    case getClockZSDOSCmd:
                        now = Calendar.getInstance().getTimeInMillis();
                        now += ClockZSDOSDelta.getTimeInMillis(); // bug i think
                        currentTime.setTimeInMillis(now);
                        currentTimeValid = true;
                        getClockZSDOSPos = 0;
                        break;
                    case setClockZSDOSCmd:
                        setClockZSDOSPos = 0;
                        break;
                    case getClockCPM3Cmd:
                        now = Calendar.getInstance().getTimeInMillis();
                        now += ClockCPM3Delta * 1000;
                        currentTime.setTimeInMillis(now);
                        currentTimeValid = true;
                        daysCPM3SinceOrg = (short) ((now - mkCPM3Origin()) / SECONDS_PER_DAY);
                        getClockCPM3Pos = 0;
                        break;
                    case setClockCPM3Cmd:
                        setClockCPM3Pos = 0;
                        break;
                    case getBankSelectCmd:
                    case setBankSelectCmd:
                    case getCommonCmd:
                    case hasBankedMemoryCmd:
                    case getHostOSPathSeparator:
                        break;
                    case resetSIMHInterfaceCmd:
                        markTimeSP = 0;
                        lastCommand = 0;
                        LOGGER.trace("[command={},name=resetSIMHInterfaceCMD,method=write] Partially implemented command!", lastCommand);
                        break;
                    case showTimerCmd:
                        LOGGER.trace("[command={},name=showTimerCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case attachPTPCmd:
                        LOGGER.trace("[command={},name=attachPTPCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case detachPTPCmd:
                        LOGGER.trace("[command={},name=detachPTPCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case setZ80CPUCmd:
                        LOGGER.trace("[command={},name=setZ80CPUCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case set8080CPUCmd:
                        LOGGER.trace("[command={},name=set8080CPUCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case startTimerInterruptsCmd:
                        LOGGER.trace("[command={},name=startTimerInterruptsCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case stopTimerInterruptsCmd:
                        LOGGER.trace("[command={},name=stopTimerInterruptsCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case setTimerDeltaCmd:
                        setTimerDeltaPos = 0;
                        break;
                    case setTimerInterruptAdrCmd:
                        setTimerInterruptAdrPos = 0;
                        break;
                    case resetStopWatchCmd:
                        LOGGER.trace("[command={},name=resetStopWatchCmd,method=write] Unimplemented command!", lastCommand);
                        break;
                    case readStopWatchCmd:
                        LOGGER.trace("[command={},name=readStopWatchCmd,method=write] Partially implemented command!", lastCommand);
                        getStopWatchDeltaPos = 0;
                        break;
                    default:
                        LOGGER.debug("[command={},method=write] Unknown command!", lastCommand);
                }
        }
    }

    @Override
    public Class<Short> getDataType() {
        return Short.class;
    }

}
