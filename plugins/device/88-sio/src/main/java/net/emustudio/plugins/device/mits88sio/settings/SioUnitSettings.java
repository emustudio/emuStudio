/*
 * This file is part of emuStudio.
 *
 * Copyright (C) 2006-2022  Peter Jakubčo
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
package net.emustudio.plugins.device.mits88sio.settings;

import net.emustudio.emulib.runtime.settings.BasicSettings;
import net.jcip.annotations.ThreadSafe;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Settings of one SIO unit
 */
@ThreadSafe
public class SioUnitSettings {
    private final static String KEY_STATUS_PORTS = "statusPorts";
    private final static String KEY_DATA_PORTS = "dataPorts";
    private final static String KEY_CLEAR_INPUT_BIT8 = "clearInputBit8";
    private final static String KEY_CLEAR_OUTPUT_BIT8 = "clearOutputBit8";
    private final static String KEY_INPUT_TO_UPPER_CASE = "inputToUpperCase";
    private final static String KEY_MAP_DELETE_CHAR = "mapDeleteChar";
    private final static String KEY_MAP_BACKSPACE_CHAR = "mapBackspaceChar";
    private final static String KEY_INPUT_INTERRUPT_VECTOR = "inputInterruptVector";
    private final static String KEY_OUTPUT_INTERRUPT_VECTOR = "outputInterruptVector";

    private final BasicSettings settings;
    private final List<SettingsObserver> observers = new CopyOnWriteArrayList<>();

    public enum MAP_CHAR {
        BACKSPACE,
        DELETE,
        UNDERSCORE,
        UNCHANGED
    }

    public SioUnitSettings(BasicSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    public void addObserver(SettingsObserver observer) {
        observers.add(observer);
    }

    public void clearObservers() {
        observers.clear();
    }

    public boolean isClearInputBit8() {
        return settings.getBoolean(KEY_CLEAR_INPUT_BIT8, false);
    }

    public void setClearInputBit8(boolean value) {
        settings.setBoolean(KEY_CLEAR_INPUT_BIT8, value);
        notifySettingsChanged();
    }

    public boolean isClearOutputBit8() {
        return settings.getBoolean(KEY_CLEAR_OUTPUT_BIT8, false);
    }

    public void setClearOutputBit8(boolean value) {
        settings.setBoolean(KEY_CLEAR_OUTPUT_BIT8, value);
        notifySettingsChanged();
    }

    public boolean isInputToUpperCase() {
        return settings.getBoolean(KEY_INPUT_TO_UPPER_CASE, false);
    }

    public void setInputToUpperCase(boolean value) {
        settings.setBoolean(KEY_INPUT_TO_UPPER_CASE, value);
        notifySettingsChanged();
    }

    public MAP_CHAR getMapDeleteChar() {
        return MAP_CHAR.valueOf(settings.getString(KEY_MAP_DELETE_CHAR, MAP_CHAR.UNCHANGED.name()));
    }

    public void setMapDeleteChar(MAP_CHAR value) {
        settings.setString(KEY_MAP_DELETE_CHAR, value.name());
        notifySettingsChanged();
    }

    public MAP_CHAR getMapBackspaceChar() {
        return MAP_CHAR.valueOf(settings.getString(KEY_MAP_BACKSPACE_CHAR, MAP_CHAR.UNCHANGED.name()));
    }

    public void setMapBackspaceChar(MAP_CHAR value) {
        settings.setString(KEY_MAP_BACKSPACE_CHAR, value.name());
        notifySettingsChanged();
    }

    public List<Integer> getStatusPorts() {
        return Arrays.stream(settings.getString(KEY_STATUS_PORTS, "").split(","))
            .map(String::trim)
            .filter(p -> !p.isEmpty())
            .map(Integer::decode)
            .collect(Collectors.toList());
    }

    public void setStatusPorts(List<Integer> value) {
        settings.setString(KEY_STATUS_PORTS, value.stream()
            .map(i -> "0x" + Integer.toHexString(i))
            .reduce((s, s2) -> s + ", " + s2)
            .orElse(""));
        notifySettingsChanged();
    }

    public List<Integer> getDataPorts() {
        return Arrays.stream(settings.getString(KEY_DATA_PORTS, "").split(","))
            .map(String::trim)
            .filter(p -> !p.isEmpty())
            .map(Integer::decode)
            .collect(Collectors.toList());
    }

    public void setDataPorts(List<Integer> value) {
        settings.setString(KEY_DATA_PORTS, value.stream()
            .map(i -> "0x" + Integer.toHexString(i))
            .reduce((s, s2) -> s + "," + s2)
            .orElse(""));
        notifySettingsChanged();
    }

    public int getInputInterruptVector() {
        return settings.getInt(KEY_INPUT_INTERRUPT_VECTOR, 0);
    }

    public void setInputInterruptVector(int vector) {
        settings.setInt(KEY_INPUT_INTERRUPT_VECTOR, vector);
        notifySettingsChanged();
    }

    public int getOutputInterruptVector() {
        return settings.getInt(KEY_OUTPUT_INTERRUPT_VECTOR, 0);
    }

    public void setOutputInterruptVector(int vector) {
        settings.setInt(KEY_OUTPUT_INTERRUPT_VECTOR, vector);
        notifySettingsChanged();
    }

    public List<Integer> getDefaultStatusPorts() {
        return List.of(0x10, 0x14, 0x16, 0x18);
    }

    public List<Integer> getDefaultDataPorts() {
        return List.of(0x11, 0x15, 0x17, 0x19);
    }

    private void notifySettingsChanged() {
        observers.forEach(SettingsObserver::settingsChanged);
    }
}
