/*
 * Run-time library for emuStudio and plugins.
 *
 *     Copyright (C) 2006-2020  Peter Jakubčo
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.emustudio.application.virtualcomputer.stubs;

import net.emustudio.emulib.plugins.cpu.CPU;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.PluginSettings;

import javax.swing.*;

public class UnannotatedCPUStub implements CPU {

    public UnannotatedCPUStub(long pluginID, ApplicationApi applicationApi, PluginSettings pluginSettings) {}

    @Override
    public void addCPUListener(CPUListener listener) {

    }

    @Override
    public void removeCPUListener(CPUListener listener) {

    }

    @Override
    public void step() {

    }

    @Override
    public void execute() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void stop() {

    }

    @Override
    public JPanel getStatusPanel() {
        return null;
    }

    @Override
    public boolean isBreakpointSupported() {
        return false;
    }

    @Override
    public void setBreakpoint(int memLocation) {

    }

    @Override
    public void unsetBreakpoint(int memLocation) {

    }

    @Override
    public boolean isBreakpointSet(int memLocation) {
        return false;
    }

    @Override
    public void reset(int startAddress) {

    }

    @Override
    public int getInstructionLocation() {
        return 0;
    }

    @Override
    public boolean setInstructionLocation(int pos) {
        return false;
    }

    @Override
    public Disassembler getDisassembler() {
        return null;
    }

    @Override
    public void reset() {

    }

    @Override
    public void initialize() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void showSettings() {

    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getVersion() {
        return null;
    }

    @Override
    public String getCopyright() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
