/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2023  Peter Jakubčo
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
package net.emustudio.plugins.device.simh;

import net.emustudio.plugins.device.simh.commands.*;

import java.util.HashMap;
import java.util.Map;

//  do not change order or remove commands, add only at the end
public enum Commands {
    printTimeCmd,               //   0 print the current time in milliseconds
    startTimerCmd,              //   1 start a new timer on the top of the timer stack
    stopTimerCmd,               //   2 stop timer on top of timer stack and show time difference
    resetPTRCmd,                //   3 reset the PTR device
    attachPTRCmd,               //   4 attach the PTR device
    detachPTRCmd,               //   5 detach the PTR device
    getSIMHVersionCmd,          //   6 get the current version of the SIMH pseudo device
    getClockZSDOSCmd,           //   7 get the current time in ZSDOS format
    setClockZSDOSCmd,           //   8 set the current time in ZSDOS format
    getClockCPM3Cmd,            //   9 get the current time in CP/M 3 format
    setClockCPM3Cmd,            //  10 set the current time in CP/M 3 format
    getBankSelectCmd,           //  11 get the selected bank
    setBankSelectCmd,           //  12 select bank
    getCommonCmd,               //  13 get the base address of the common memory segment
    resetSIMHInterfaceCmd,      //  14 reset the SIMH pseudo device
    showTimerCmd,               //  15 show time difference to timer on top of stack
    attachPTPCmd,               //  16 attach PTP to the file with name at beginning of CP/M command line
    detachPTPCmd,               //  17 detach PTP
    hasBankedMemoryCmd,         //  18 determines whether machine has banked memory
    setZ80CPUCmd,               //  19 set the CPU to a Z80
    set8080CPUCmd,              //  20 set the CPU to an 8080
    startTimerInterruptsCmd,    //  21 start timer interrupts
    stopTimerInterruptsCmd,     //  22 stop timer interrupts
    setTimerDeltaCmd,           //  23 set the timer interval in which interrupts occur
    setTimerInterruptAdrCmd,    //  24 set the address to call by timer interrupts
    resetStopWatchCmd,          //  25 reset the millisecond stop watch
    readStopWatchCmd,           //  26 read the millisecond stop watch
    SIMHSleepCmd,               //  27 let SIMH sleep for SIMHSleep milliseconds
    getHostOSPathSeparatorCmd,  //  28 obtain the file path separator of the OS under which SIMH runs
    getHostFilenamesCmd,        //  29 perform wildcard expansion and obtain list of file names
    readURLCmd,                 //  30 read the contents of an URL
    getCPUClockFrequency,       //  31 get the clock frequency of the CPU
    setCPUClockFrequency,       //  32 set the clock frequency of the CPU
    genInterruptCmd,            //  33 generate interrupt,
    unknownCmd;

    public final static Map<Integer, Command> COMMANDS_MAP = new HashMap<>();

    static {
        COMMANDS_MAP.put(printTimeCmd.ordinal(), PrintTime.INS);
        COMMANDS_MAP.put(startTimerCmd.ordinal(), StartTimer.INS);
        COMMANDS_MAP.put(stopTimerCmd.ordinal(), StopTimer.INS);
        COMMANDS_MAP.put(resetPTRCmd.ordinal(), ResetPTR.INS);
        COMMANDS_MAP.put(attachPTRCmd.ordinal(), AttachPTR.INS);
        COMMANDS_MAP.put(detachPTRCmd.ordinal(), DetachPTR.INS);
        COMMANDS_MAP.put(getSIMHVersionCmd.ordinal(), GetSimhVersion.INS);
        COMMANDS_MAP.put(getClockZSDOSCmd.ordinal(), GetClockZSDOS.INS);
        COMMANDS_MAP.put(setClockZSDOSCmd.ordinal(), SetClockZSDOS.INS);
        COMMANDS_MAP.put(getClockCPM3Cmd.ordinal(), GetClockCPM3.INS);
        COMMANDS_MAP.put(setClockCPM3Cmd.ordinal(), SetClockCPM3.INS);
        COMMANDS_MAP.put(getBankSelectCmd.ordinal(), GetBankSelect.INS);
        COMMANDS_MAP.put(setBankSelectCmd.ordinal(), SetBankSelect.INS);
        COMMANDS_MAP.put(getCommonCmd.ordinal(), GetCommon.INS);
        COMMANDS_MAP.put(resetSIMHInterfaceCmd.ordinal(), ResetSimhInterface.INS);
        COMMANDS_MAP.put(showTimerCmd.ordinal(), ShowTimer.INS);
        COMMANDS_MAP.put(attachPTPCmd.ordinal(), AttachPTP.INS);
        COMMANDS_MAP.put(detachPTPCmd.ordinal(), DetachPTP.INS);
        COMMANDS_MAP.put(hasBankedMemoryCmd.ordinal(), HasBankedMemory.INS);
        COMMANDS_MAP.put(setZ80CPUCmd.ordinal(), SetZ80CPU.INS);
        COMMANDS_MAP.put(set8080CPUCmd.ordinal(), Set8080CPU.INS);
        COMMANDS_MAP.put(startTimerInterruptsCmd.ordinal(), StartTimerInterrupts.INS);
        COMMANDS_MAP.put(stopTimerInterruptsCmd.ordinal(), StopTimerInterrupts.INS);
        COMMANDS_MAP.put(setTimerDeltaCmd.ordinal(), SetTimerDelta.INS);
        COMMANDS_MAP.put(setTimerInterruptAdrCmd.ordinal(), SetTimerInterruptAdr.INS);
        COMMANDS_MAP.put(resetStopWatchCmd.ordinal(), ResetStopWatch.INS);
        COMMANDS_MAP.put(readStopWatchCmd.ordinal(), ReadStopWatch.INS);
        COMMANDS_MAP.put(SIMHSleepCmd.ordinal(), SIMHSleep.INS);
        COMMANDS_MAP.put(getHostOSPathSeparatorCmd.ordinal(), GetHostOSPathSeparator.INS);
        COMMANDS_MAP.put(getHostFilenamesCmd.ordinal(), GetHostFilenames.INS);
        COMMANDS_MAP.put(readURLCmd.ordinal(), ReadURL.INS);
        COMMANDS_MAP.put(getCPUClockFrequency.ordinal(), GetCPUClockFrequency.INS);
        COMMANDS_MAP.put(setCPUClockFrequency.ordinal(), SetCPUClockFrequency.INS);
        COMMANDS_MAP.put(genInterruptCmd.ordinal(), GenInterrupt.INS);
    }

    public static Commands fromInt(int number) {
        for (Commands c : Commands.values()) {
            if (c.ordinal() == number) {
                return c;
            }
        }
        throw new IllegalArgumentException("Unknown command");
    }
}
