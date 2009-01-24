/*
 * PseudoContext.java
 * 
 * (c) Copyright 2008, Peter Jakubco
 * Copyright (c) 2002-2007, Peter Schorn
 */

package simh;

import interfaces.SMemoryContext;
import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;
import plugins.device.IDeviceContext;

/**
 *
 * @author vbmacher
 */
public class PseudoContext implements IDeviceContext {
    private SMemoryContext mem;    

/* SIMH pseudo device status registers                                                                          */
    /* ZSDOS clock definitions                                                                                      */
    private Calendar ClockZSDOSDelta = Calendar.getInstance(); /* delta between real clock and Altair clock                    */
    private int setClockZSDOSPos = 0; /* determines state for receiving address of parameter block    */
    private int setClockZSDOSAdr = 0; /* address in M of 6 byte parameter block for setting time      */
    private int getClockZSDOSPos = 0; /* determines state for sending clock information               */

    /* CPM3 clock definitions                                                                                       */
    private int ClockCPM3Delta = 0; /* delta between real clock and Altair clock                    */
    private int setClockCPM3Pos = 0; /* determines state for receiving address of parameter block    */
    private int setClockCPM3Adr = 0; /* address in M of 5 byte parameter block for setting time      */
    private int getClockCPM3Pos = 0; /* determines state for sending clock information               */
    private int daysCPM3SinceOrg = 0; /* days since 1 Jan 1978                                        */

/* interrupt related                                                                                            */
    private int timeOfNextInterrupt; /* time when next interrupt is scheduled                        */
    private boolean timerInterrupt = false; /* timer interrupt pending                                      */
    private int timerInterruptHandler = 0x0fc00;  /* default address of interrupt handling routine                */
    private int setTimerInterruptAdrPos = 0; /* determines state for receiving timerInterruptHandler         */
    private int timerDelta = 100;      /* interrupt every 100 ms                                       */
    private int setTimerDeltaPos = 0; /* determines state for receiving timerDelta                    */

/* stop watch and timer related                                                                                 */
    private int stopWatchDelta        = 0;        /* stores elapsed time of stop watch                            */
    private int getStopWatchDeltaPos   = 0;        /* determines the state for receiving stopWatchDelta            */
    private int stopWatchNow          = 0;        /* stores starting time of stop watch                           */
    private int markTimeSP             = 0;        /* stack pointer for timer stack                                */

    ///* miscellaneous                                                                                                */
    private int versionPos     = 0; /* determines state for sending device identifier               */
    private int lastCPMStatus  = 0; /* result of last attachCPM command                             */
    private int lastCommand    = 0; /* most recent command processed on port 0xfeh */
    private int getCommonPos   = 0; /* determines state for sending the 'common' register           */
    private Calendar currentTime = Calendar.getInstance();
    private boolean currentTimeValid = false;
    private short version[] = {'S', 'I', 'M', 'H', '0', '0', '3',0};

    private final static int SECONDS_PER_MINUTE = 60;
    private final static int SECONDS_PER_HOUR = (60 * SECONDS_PER_MINUTE);
    private final static int SECONDS_PER_DAY = (24 * SECONDS_PER_HOUR);
    
    public PseudoContext(SMemoryContext mem) {
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
    
    public void reset() {
        currentTimeValid = false;
        lastCommand = 0;
        lastCPMStatus = 0;
        setClockZSDOSPos = 0;
        getClockZSDOSPos = 0;
        ClockZSDOSDelta = Calendar.getInstance();
        ClockCPM3Delta  = 0;
        setClockCPM3Pos = 0;
        getClockCPM3Pos = 0;
        getStopWatchDeltaPos = 0;
        getCommonPos = 0;
        setTimerDeltaPos = 0;
        setTimerInterruptAdrPos = 0;
        markTimeSP = 0;
        versionPos = 0;
        timerInterrupt = false;
//    if (simh_unit.flags & UNIT_SIMH_TIMERON)
//        simh_dev_set_timeron(NULL, 0, NULL, NULL);
    }
    
    private int toBCD(int x) { return (x / 10) * 16 + (x % 10); }
    private int fromBCD(int x) { return 10 * ((0xf0 & x) >> 4) + (0x0f & x); }

    /* setClockZSDOSAdr points to 6 byte block in M: YY MM DD HH MM SS in BCD notation */
    private void setClockZSDOS() {
        int year = fromBCD((Short)mem.read(setClockZSDOSAdr));
        int yy = (year < 50 ? year + 100 : year) + 1900;
        int mm = fromBCD((Short)mem.read(setClockZSDOSAdr + 1)) - 1;
        int dd = fromBCD((Short)mem.read(setClockZSDOSAdr + 2));
        int hh = fromBCD((Short)mem.read(setClockZSDOSAdr + 3));
        int min = fromBCD((Short)mem.read(setClockZSDOSAdr + 4));
        int ss = fromBCD((Short)mem.read(setClockZSDOSAdr + 5));
        ClockZSDOSDelta.set(year, mm, dd, hh, min, ss);
    }

//#define CPM_COMMAND_LINE_LENGTH    128
//#define TIMER_STACK_LIMIT          10       /* stack depth of timer stack   */
//static uint32 markTime[TIMER_STACK_LIMIT];  /* timer stack                  */
//
//static void warnNoRealTimeClock(void) {
//    if (simh_unit.flags & UNIT_SIMH_VERBOSE)
//        printf("Sorry - no real time clock available.\n");
//}
//
//static t_stat simh_dev_set_timeron(UNIT *uptr, int32 value, char *cptr, void *desc) {
//    if (rtc_avail) {
//        timeOfNextInterrupt = sim_os_msec() + timerDelta;
//        return sim_activate(&simh_unit, simh_unit.wait);    /* activate unit */
//    }
//    warnNoRealTimeClock();
//    return SCPE_ARG;
//}
//
//static t_stat simh_dev_set_timeroff(UNIT *uptr, int32 value, char *cptr, void *desc) {
//    timerInterrupt = FALSE;
//    sim_cancel(&simh_unit);
//    return SCPE_OK;
//}
//
//static t_stat simh_svc(UNIT *uptr) {
//    uint32 n = sim_os_msec();
//    if (n >= timeOfNextInterrupt) {
//        timerInterrupt = TRUE;
//        timeOfNextInterrupt += timerDelta;
//        if (n >= timeOfNextInterrupt)               /* time of next interrupt is not in the future  */
//            timeOfNextInterrupt = n + timerDelta;   /* make sure it is in the future!               */
//    }
//    if (simh_unit.flags & UNIT_SIMH_TIMERON)
//        sim_activate(&simh_unit, simh_unit.wait);   /* activate unit                                */
//    return SCPE_OK;
//}
//
//static char cpmCommandLine[CPM_COMMAND_LINE_LENGTH];
//static void createCPMCommandLine(void) {
//    int32 i, len = (GetBYTEWrapper(0x80) & 0x7f); /* 0x80 contains length of command line, discard first char */
//    for (i = 0; i < len - 1; i++)
//        cpmCommandLine[i] = (char)GetBYTEWrapper(0x82 + i); /* the first char, typically ' ', is discarded */
//    cpmCommandLine[i] = 0; /* make C string */
//}
//
///* The CP/M command line is used as the name of a file and UNIT* uptr is attached to it. */
//static void attachCPM(UNIT *uptr) {
//    createCPMCommandLine();
//    if (uptr == &ptr_unit)
//        sim_switches = SWMASK('R');
//    else if (uptr == &ptp_unit)
//        sim_switches = SWMASK('W') | SWMASK('C');   /* 'C' option makes sure that file is properly truncated
//                                                        if it had existed before                                */
//    lastCPMStatus = attach_unit(uptr, cpmCommandLine);
//    if ((lastCPMStatus != SCPE_OK) && (simh_unit.flags & UNIT_SIMH_VERBOSE)) {
//        MESSAGE_3("Cannot open '%s' (%s).", cpmCommandLine, scp_error_messages[lastCPMStatus - SCPE_BASE]);
//        /* must keep curly braces as messageX is a macro with two statements */
//    }
//}
//
//
    
    private short mkCPM3Origin() {
 	short month, year;
	short result;
	short[] m_to_d = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
        month = 11; // t->tm_mon
	year = (short)((1977 + month / 12 + 1900) &0xFFFF); // t->tm_year
	month %= 12;
	if (month < 0) {
            year -= 1;
            month += 12;
        }
	result = (short)(((year - 1970) * 365 + (year - 1969) / 4 + m_to_d[month])&0xffff);
	result = (short)(((year - 1970) * 365 + m_to_d[month])&0xffff);
	if (month <= 1) year -= 1;
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
	return(result);
    }
    
    /* setClockCPM3Adr points to 5 byte block in M:
        0 - 1 int16:    days since 31 Dec 77
            2 BCD byte: HH
            3 BCD byte: MM
            4 BCD byte: SS                              */
    private void setClockCPM3() {
        ClockCPM3Delta = mkCPM3Origin() +
                ((Short)mem.read(setClockCPM3Adr) + (Short)mem.read(setClockCPM3Adr + 1) * 256)
                * SECONDS_PER_DAY + fromBCD((Short)mem.read(setClockCPM3Adr + 2))
                * SECONDS_PER_HOUR + fromBCD((Short)mem.read(setClockCPM3Adr + 3))
                * SECONDS_PER_MINUTE + fromBCD((Short)mem.read(setClockCPM3Adr + 4))
                - (short)(Calendar.getInstance().getTimeInMillis()/1000);
    }

    @Override
    public Object in(EventObject evt) {
        int result = 0;
        switch(lastCommand) {
            case getHostFilenames:
//#if UNIX_PLATFORM
//            if (globValid) {
//                if (globPosNameList < globS.gl_pathc) {
//                    if (!(result = globS.gl_pathv[globPosNameList][globPosName++])) {
//                        globPosNameList++;
//                        globPosName = 0;
//                    }
//                }
//                else {
//                    globValid = FALSE;
//                    lastCommand = 0;
//                    globfree(&globS);
//                }
//            }
//#elif defined (_WIN32)
//            if (globValid) {
//                if (globFinished) {
//                    globValid = FALSE;
//                }
//                else if (!(result = FindFileData.cFileName[globPosName++])) {
//                    globPosName = 0;
//                    if (!FindNextFile(hFind, &FindFileData)) {
//                        globFinished = TRUE;
//                        FindClose(hFind);
//                        hFind = INVALID_HANDLE_VALUE;
//                    }
//                }
//            }
//#else
                lastCommand = 0;
//#endif
                break;
            case attachPTRCmd:
            case attachPTPCmd:
                result = lastCPMStatus;
                lastCommand = 0;
                break;
            case getClockZSDOSCmd:
                if (currentTimeValid) {
                    switch(getClockZSDOSPos) {
                        case 0:
                            int year = (currentTime.get(Calendar.YEAR)-1900);
                            result = toBCD(year > 99 ? year - 100 : year);
                            getClockZSDOSPos = 1; break;
                        case 1:
                            result = toBCD(currentTime.get(Calendar.MONTH) + 1);
                            getClockZSDOSPos = 2; break;
                        case 2:
                            result = toBCD(currentTime.get(Calendar.DAY_OF_MONTH));
                            getClockZSDOSPos = 3; break;
                        case 3:
                            result = toBCD(currentTime.get(Calendar.HOUR_OF_DAY));
                            getClockZSDOSPos = 4; break;
                        case 4:
                            result = toBCD(currentTime.get(Calendar.MINUTE));
                            getClockZSDOSPos = 5; break;
                        case 5:
                            result = toBCD(currentTime.get(Calendar.SECOND));
                            getClockZSDOSPos = lastCommand = 0; break;
                    }
                } else { result = getClockZSDOSPos = lastCommand = 0; }
                break;
            case getClockCPM3Cmd:
                if (currentTimeValid) {
                    switch(getClockCPM3Pos) {
                        case 0:
                            result = daysCPM3SinceOrg & 0xff;
                            getClockCPM3Pos = 1; break;
                        case 1:
                            result = (daysCPM3SinceOrg >> 8) & 0xff;
                            getClockCPM3Pos = 2; break;
                        case 2:
                            result = toBCD(currentTime.get(Calendar.HOUR_OF_DAY));
                            getClockCPM3Pos = 3; break;
                        case 3:
                            result = toBCD(currentTime.get(Calendar.MINUTE));
                            getClockCPM3Pos = 4; break;
                        case 4:
                            result = toBCD(currentTime.get(Calendar.SECOND));
                            getClockCPM3Pos = lastCommand = 0; break;
                    }
                } else { result = getClockCPM3Pos = lastCommand = 0; }
                break;
            case getSIMHVersionCmd:
                result = version[versionPos++];
                if (result == 0) { versionPos = lastCommand = 0; }
                break;
            case getBankSelectCmd:
                result = mem.getSelectedBank(); lastCommand = 0; break;
            case getCommonCmd:
                if (getCommonPos == 0) {
                    result = mem.getCommonBoundary() & 0xff;
                    getCommonPos = 1;
                } else {
                    result = (mem.getCommonBoundary() >> 8) & 0xff;
                    getCommonPos = lastCommand = 0;
                } break;
            case hasBankedMemoryCmd:
                result = mem.getBanksCount(); lastCommand = 0; break;
            case readStopWatchCmd:
                if (getStopWatchDeltaPos == 0) {
                    result = stopWatchDelta & 0xff;
                    getStopWatchDeltaPos = 1;
                } else {
                    result = (stopWatchDelta >> 8) & 0xff;
                    getStopWatchDeltaPos = lastCommand = 0;
                } break;
            case getHostOSPathSeparator:
                result = File.separatorChar;
                break;
            default: /* undefined */
                result = lastCommand = 0;
        }
        return result;
    }

    @Override
    public void out(EventObject evt, Object value) {
    	int val = (Integer)value;
        long now;
        switch(lastCommand) {
            case setClockZSDOSCmd:
                if (setClockZSDOSPos == 0) {
                    setClockZSDOSAdr = val;
                    setClockZSDOSPos = 1;
                } else {
                    setClockZSDOSAdr |= (val << 8);
                    setClockZSDOS();
                    setClockZSDOSPos = lastCommand = 0;
                } break;
            case setClockCPM3Cmd:
                if (setClockCPM3Pos == 0) {
                    setClockCPM3Adr = val;
                    setClockCPM3Pos = 1;
                } else {
                    setClockCPM3Adr |= (val << 8);
                    setClockCPM3();
                    setClockCPM3Pos = lastCommand = 0;
                } break;
            case setBankSelectCmd:
                    mem.setSeletedBank((short)(val&0xff));
                    lastCommand = 0; break;
            case setTimerDeltaCmd:
                if (setTimerDeltaPos == 0) {
                    timerDelta = val;
                    setTimerDeltaPos = 1;
                } else {
                    timerDelta |= (val << 8);
                    setTimerDeltaPos = lastCommand = 0;
                } break;
            case setTimerInterruptAdrCmd:
                if (setTimerInterruptAdrPos == 0) {
                    timerInterruptHandler = val;
                    setTimerInterruptAdrPos = 1;
                } else {
                    timerInterruptHandler |= (val << 8);
                    setTimerInterruptAdrPos = lastCommand = 0;
                } break;
            default:
                lastCommand = val;
                switch(val) {
                    case getHostFilenames:
//#if UNIX_PLATFORM
//                    if (!globValid) {
//                        globValid = TRUE;
//                        globPosNameList = globPosName = 0;
//                        createCPMCommandLine();
//                        globError = glob(cpmCommandLine, GLOB_ERR, NULL, &globS);
//                        if (globError) {
//                            if (simh_unit.flags & UNIT_SIMH_VERBOSE) {
//                                MESSAGE_3("Cannot expand '%s'. Error is %i.", cpmCommandLine, globError);
//                            }
//                            globfree(&globS);
//                            globValid = FALSE;
//                        }
//                    }
//#elif defined (_WIN32)
//                    if (!globValid) {
//                        globValid = TRUE;
//                        globPosName = 0;
//                        globFinished = FALSE;
//                        createCPMCommandLine();
//                        hFind = FindFirstFile(cpmCommandLine, &FindFileData);
//                        if (hFind == INVALID_HANDLE_VALUE) {
//                            if (simh_unit.flags & UNIT_SIMH_VERBOSE) {
//                                MESSAGE_3("Cannot expand '%s'. Error is %lu.", cpmCommandLine, GetLastError());
//                            }
//                            globValid = FALSE;
//                        }
//                    }
//#endif
                        break;
                    case SIMHSleepCmd:
//#if defined (_WIN32)
//                    if ((SIMHSleep / 1000) && !sio_unit.u4) /* time to sleep and SIO not attached to a file */
//                        Sleep(SIMHSleep / 1000);
//#else
//                    if (SIMHSleep && !sio_unit.u4)          /* time to sleep and SIO not attached to a file */
//                        usleep(SIMHSleep);
//#endif
                        break;
                    case printTimeCmd:  /* print time */
//                    if (rtc_avail) {
//                        MESSAGE_2("Current time in milliseconds = %d.", sim_os_msec());
//                    }
//                    else {
//                        warnNoRealTimeClock();
//                    }
                        break;
                    case startTimerCmd: /* create a new timer on top of stack */
//                    if (rtc_avail) {
//                        if (markTimeSP < TIMER_STACK_LIMIT) {
//                            markTime[markTimeSP++] = sim_os_msec();
//                        }
//                        else {
//                            MESSAGE_1("Timer stack overflow.");
//                        }
//                    }
//                    else {
//                        warnNoRealTimeClock();
//                    }
                        break;
                    case stopTimerCmd:  /* stop timer on top of stack and show time difference */
//                    if (rtc_avail) {
//                        if (markTimeSP > 0) {
//                            uint32 delta = sim_os_msec() - markTime[--markTimeSP];
//                            MESSAGE_2("Timer stopped. Elapsed time in milliseconds = %d.", delta);
//                        }
//                        else {
//                            MESSAGE_1("No timer active.");
//                        }
//                    }
//                    else {
//                        warnNoRealTimeClock();
//                    }
                        break;
                    case resetPTRCmd:   /* reset ptr device */
//                    ptr_reset(NULL);
                        break;
                    case attachPTRCmd:  /* attach ptr to the file with name at beginning of CP/M command line */
//                    attachCPM(&ptr_unit);
                        break;
                    case detachPTRCmd:  /* detach ptr */
//                    detach_unit(&ptr_unit);
                        break;
                    case getSIMHVersionCmd:
                        versionPos = 0; break;
                    case getClockZSDOSCmd:
//                    time(&now);
                        now = Calendar.getInstance().getTimeInMillis();
                        now += ClockZSDOSDelta.getTimeInMillis(); // bug i think
                        currentTime.setTimeInMillis(now);
                        currentTimeValid = true;
                        getClockZSDOSPos = 0;
                        break;
                    case setClockZSDOSCmd:
                        setClockZSDOSPos = 0; break;
                    case getClockCPM3Cmd:
                        now = Calendar.getInstance().getTimeInMillis();
                        now += ClockCPM3Delta*1000;
                        currentTime.setTimeInMillis(now);
                        currentTimeValid = true;
                        daysCPM3SinceOrg = (int)((now - mkCPM3Origin()) / SECONDS_PER_DAY);
                        getClockCPM3Pos = 0; break;
                    case setClockCPM3Cmd:
                        setClockCPM3Pos = 0;break;
                    case getBankSelectCmd:
                    case setBankSelectCmd:
                    case getCommonCmd:
                    case hasBankedMemoryCmd:
                    case getHostOSPathSeparator: break;
                    case resetSIMHInterfaceCmd:
                        markTimeSP  = 0;
                        lastCommand = 0;
//#if UNIX_PLATFORM
//                    if (globValid) {
//                        globValid = FALSE;
//                        globfree(&globS);
//                    }
//#elif defined (_WIN32)
//                    if (globValid) {
//                        globValid = FALSE;
//                        if (hFind != INVALID_HANDLE_VALUE) {
//                            FindClose(hFind);
//                        }
//                    }
//#endif
                        break;
                    case showTimerCmd:  /* show time difference to timer on top of stack */
//                    if (rtc_avail) {
//                        if (markTimeSP > 0) {
//                            uint32 delta = sim_os_msec() - markTime[markTimeSP - 1];
//                            MESSAGE_2("Timer running. Elapsed in milliseconds = %d.", delta);
//                        }
//                        else {
//                            MESSAGE_1("No timer active.");
//                        }
//                    }
//                    else {
//                        warnNoRealTimeClock();
//                    }
                       break;
                    case attachPTPCmd:  /* attach ptp to the file with name at beginning of CP/M command line */
//                    attachCPM(&ptp_unit);
                        break;
                    case detachPTPCmd:  /* detach ptp */
//                    detach_unit(&ptp_unit);
                        break;
                    case setZ80CPUCmd:
//                    cpu_unit.flags |= UNIT_CHIP;
                        break;
                    case set8080CPUCmd:
//                    cpu_unit.flags &= ~UNIT_CHIP;
                        break;
                    case startTimerInterruptsCmd:
//                    if (simh_dev_set_timeron(NULL, 0, NULL, NULL) == SCPE_OK) {
//                        timerInterrupt = FALSE;
//                        simh_unit.flags |= UNIT_SIMH_TIMERON;
//                    }
                        break;
                    case stopTimerInterruptsCmd:
//                    simh_unit.flags &= ~UNIT_SIMH_TIMERON;
//                    simh_dev_set_timeroff(NULL, 0, NULL, NULL);
                        break;
                    case setTimerDeltaCmd:
                        setTimerDeltaPos = 0; break;
                    case setTimerInterruptAdrCmd:
                        setTimerInterruptAdrPos = 0; break;
                    case resetStopWatchCmd:
//                    stopWatchNow = rtc_avail ? sim_os_msec() : 0;
                        break;
                    case readStopWatchCmd:
                        getStopWatchDeltaPos = 0;
//                    stopWatchDelta = rtc_avail ? sim_os_msec() - stopWatchNow : 0;
                        break;
                    default:
//                    if (simh_unit.flags & UNIT_SIMH_VERBOSE) {
//                        MESSAGE_3("Unknown command (%i) to SIMH pseudo device on port %03xh ignored.",
//                            data, port);
//                    }
                }
        }
    }

    @Override
    public String getID() { return "SIMH-PSEUDO"; }

	@Override
	public Class<?> getDataType() {
		return Integer.class;
	}

	@Override
	public String getHash() {
		return "4a0411686e1560c765c1d6ea903a9c5f";
	}

}
