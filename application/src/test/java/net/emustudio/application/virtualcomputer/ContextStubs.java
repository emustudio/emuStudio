/* SPDX-FileCopyrightText: 2006-2026 Peter Jakubčo
   SPDX-License-Identifier: GPL-3.0-or-later */
package net.emustudio.application.virtualcomputer;

import net.emustudio.application.virtualcomputer.stubs.CPUContextStub;
import net.emustudio.application.virtualcomputer.stubs.CompilerContextStub;
import net.emustudio.application.virtualcomputer.stubs.DeviceContextStub;
import net.emustudio.application.virtualcomputer.stubs.ShortMemoryContextStub;
import net.emustudio.emulib.plugins.Context;
import net.emustudio.emulib.plugins.annotations.PluginContext;

public class ContextStubs {

    @PluginContext
    public interface DifferentCPUContextStubWithEqualHash extends CPUContextStub {

    }

    @PluginContext
    public interface DifferentCompilerContextStubWithEqualHash extends CompilerContextStub {

    }

    @PluginContext
    public interface DifferentShortMemoryContextStubWithEqualHash extends ShortMemoryContextStub {

    }

    @PluginContext
    public interface DifferentDeviceContextStubWithEqualHash extends DeviceContextStub {

    }

    @PluginContext
    public interface FirstEmptyContextStub extends Context {

    }

    @PluginContext
    public interface SecondEmptyContextStub extends Context {

    }
}
