/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer.stubs;

import net.emustudio.emulib.plugins.PluginInitializationException;
import net.emustudio.emulib.plugins.annotations.PLUGIN_TYPE;
import net.emustudio.emulib.plugins.annotations.PluginRoot;
import net.emustudio.emulib.plugins.cpu.Disassembler;
import net.emustudio.emulib.runtime.ApplicationApi;
import net.emustudio.emulib.runtime.settings.PluginSettings;

import javax.swing.*;

@SuppressWarnings("unused")
@PluginRoot(title = "CPU", type = PLUGIN_TYPE.CPU)
public class CPUImplStub extends AbstractCPUStub {

    public CPUImplStub(long pluginID, ApplicationApi applicationApi, PluginSettings settings) {
    }

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
    public void setBreakpoint(int pos) {
    }

    @Override
    public void unsetBreakpoint(int pos) {
    }

    @Override
    public boolean isBreakpointSet(int pos) {
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
    public void initialize() throws PluginInitializationException {
        throw new PluginInitializationException();
    }

    @Override
    public void destroy() {
    }

    @Override
    public void showSettings(JFrame parent) {
    }

    @Override
    public boolean isShowSettingsSupported() {
        return false;
    }

    @Override
    public String getVersion() {
        return "";
    }

    @Override
    public String getCopyright() {
        return "(c)";
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getTitle() {
        return CPUImplStub.class.getAnnotation(PluginRoot.class).title();
    }
}
